package com.github.oberdiah.deepcomplexity.context

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.github.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import com.github.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.getField
import com.github.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.intellij.psi.PsiType
import kotlin.test.assertEquals

typealias Vars = Map<UnknownKey, RootExpression<*>>

/**
 * A potentially subtle but important point is that an unknown variable in a context never
 * refers to another variable within that same context.
 *
 * For example, in a context `{ x: y + 1, y: 2}`, the variable `x` is not equal to `2 + 1`, it's equal to `y' + 1`.
 *
 * We encode this with the [KeyBackreference] class, representing an unknown that this context will never manage
 * to resolve.
 *
 * Vars are the currently understood values for things that can be modified.
 *
 * How objects are stored:
 *  - Every object's field will be stored as its own variable.
 *  - An object itself (the heap reference) may also be an unknown. e.g. { x: a }
 *  - Critically: HeapMarkers are not unknowns. They are constants that point to a specific object, so they do
 *    not have a previous or future state. E.g. in { #1.x: 2, x: #1 }, it is completely OK to resolve `x.x` to `2`.
 *    Confusingly it is also OK to resolve `b.x` using { b: a, a.x: 2 } to `2` - although `b` is `a'` and not `a`,
 *    `a.x` is also actually `a'.x`.
 *  - ObjectExprs can be considered a ConstExpr, except that their values can be used to build QualifiedKeys.
 */
class Context(
    variables: Vars,
    /**
     * Unfortunately necessary, and I don't think there's any way around it (though I guess we could store
     * it in the key of the `this` object? What would we do when we don't know that expression yet, though?)
     *
     * The reason this is needed is when we're doing aliasing resolution inside a method with no
     * additional context, we need to know if `this` has the same type as any of the parameters, in case
     * they alias.
     *
     * Imagine a case where we're evaluating an expression like `int t = this.q`. `this`'s type needs to be known
     * to at least some degree to perform alias protection, and the only place to store that is in the context.
     */
    val thisType: PsiType?,
    private val idx: ContextId
) {
    /**
     * All the variables in this context.
     *
     * We map all the expressions to our own context id as now that they're here, they must
     * never be resolved with us either, alongside anything they were previously forbidden to resolve.
     */
    private val variables: Vars = variables.mapValues { expr ->
        expr.value.replaceTypeInTree<VariableExpr<*>> {
            VariableExpr.new(it.key.withAddedContextId(idx))
        }
    }.mapKeys { it.key.withAddedContextId(idx) }

    init {
        assert(variables.keys.filterIsInstance<ReturnKey>().size <= 1) {
            "A context cannot have multiple return keys."
        }
    }

    @JvmInline
    value class ContextId(private val ids: Set<Int>) {
        operator fun plus(other: ContextId): ContextId = ContextId(this.ids + other.ids)
        fun collidesWith(other: ContextId): Boolean = this.ids.any { it in other.ids }

        companion object {
            var ID_INDEX = 0
            fun new(): ContextId = ContextId(setOf(ID_INDEX++))
        }
    }

    /**
     * [KeyBackreference]s must be very carefully resolved; they cannot be resolved in any context
     * they were created in. Only [Context]s have the ability to create and resolve them.
     */
    data class KeyBackreference(private val key: UnknownKey, private val contextId: ContextId) : Qualifier {
        override fun toString(): String = "$key'"
        override fun equals(other: Any?): Boolean = other is KeyBackreference && this.key == other.key
        override fun hashCode(): Int = key.hashCode()

        override val ind: SetIndicator<*> = key.ind
        override fun withAddedContextId(newId: ContextId): KeyBackreference =
            KeyBackreference(key.withAddedContextId(newId), contextId + newId)

        /**
         * This shouldn't be used unless you know for certain you're in the evaluation stage;
         * using this in the method processing stage may lead to you resolving a key using your
         * own context, which is a recipe for disaster.
         */
        fun grabTheKeyYesIKnowWhatImDoingICanGuaranteeImInTheEvaluateStage(): UnknownKey = key

        override fun safelyResolveUsing(context: Context): Expr<*> {
            assert(!contextId.collidesWith(context.idx)) {
                "Cannot resolve a KeyBackreference in the context it was created in."
            }

            return if (key is QualifiedFieldKey && key.qualifier is KeyBackreference) {
                val q = key.qualifier.safelyResolveUsing(context)
                q.getField(context, key.field)
            } else {
                context.getVar(key)
            }
        }

        override fun toLeafExpr(): LeafExpr<*> = VariableExpr.new(this)

        fun isPlaceholder(): Boolean = key.isPlaceholder()
    }

    companion object {
        fun brandNew(thisType: PsiType?): Context = Context(emptyMap(), thisType, ContextId.new())

        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and cannot resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         */
        fun combine(a: Context, b: Context, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): Context {
            assertEquals(
                a.thisType,
                b.thisType,
                "Cannot combine contexts with different 'this' types."
            )
            return Context(
                (a.variables.keys + b.variables.keys)
                    .associateWith { key ->
                        val aVal = a.variables[key]
                        val bVal = b.variables[key]

                        RootExpression.combine(
                            VariableExpr.new(KeyBackreference(key, a.idx + b.idx)),
                            aVal,
                            bVal,
                            how
                        )
                    },
                a.thisType,
                a.idx + b.idx
            )
        }
    }

    val returnValue: Expr<*>?
        get() = variables.filterKeys { it is ReturnKey }.values.firstOrNull()?.getDynExpr()

    override fun toString(): String {
        val nonPlaceholderVariablesString =
            variables.filterKeys { !it.isPlaceholder() }.entries.joinToString("\n") { entry ->
                "${entry.key}:\n${entry.value.toString().prependIndent()}"
            }
        val placeholderVariablesString =
            variables.filterKeys { it.isPlaceholder() }.entries.joinToString("\n") { entry ->
                "${entry.key}:\n${entry.value.toString().prependIndent()}"
            }

        return "Context: {\n" +
                "${nonPlaceholderVariablesString.prependIndent()}\n" +
                "${placeholderVariablesString.prependIndent()}\n" +
                "}"
    }

    fun getVar(key: UnknownKey): Expr<*> {
        // If we have it, return it.
        variables[key]?.let { return it.getDynExpr() }

        // If we don't, before we create a new variable expression, we need to check in case there's a placeholder
        if (key is QualifiedFieldKey) {
            val placeholderQualifierKey =
                KeyBackreference(PlaceholderKey(key.qualifier.ind as ObjectSetIndicator), this.idx)

            val replacementQualified = VariableExpr.new(KeyBackreference(key, this.idx))
            val replacementRaw = key.qualifier.toLeafExpr()
            val placeholderVersionOfTheKey = QualifiedFieldKey(placeholderQualifierKey, key.field)
            val p = KeyBackreference(placeholderVersionOfTheKey, this.idx)

            variables[placeholderVersionOfTheKey]?.let {
                val replacedExpr = it.replaceTypeInTree<VariableExpr<*>> { expr ->
                    when (expr.key) {
                        p -> replacementQualified
                        placeholderQualifierKey -> replacementRaw
                        else -> null
                    }
                }

                return replacedExpr.getDynExpr()
            }
        }

        // OK, now we really do have no choice
        return VariableExpr.new(KeyBackreference(key, idx))
    }

    /**
     * Sets the given l-value expression to the provided [rExpr], returning a new context.
     * This operation may not just update a single variable; if the l-value expression
     * is a field expression, we may end up doing quite a bit of work.
     */
    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): Context {
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)
        assert(rExpr.iterateTree().none { it is LValueExpr }) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }
        return withVar(lExpr, RootExpression.new(rExpr))
    }

    /**
     * This first private [withVar] deals with complex qualifier expressions.
     *
     * The core of this does look a bit scary, so I'll try to walk you through it:
     * Essentially, a qualifier may not just be a simple VariableExpression with a HeapKey.
     * In the simplest case it is, and this all becomes a lot easier, but in the general case
     * it may be any complicated expression.
     * Let's go with the following example:
     * ```
     * a = new C(2);
     * b = new C(3);
     * ((x > 0) ? a : b).x = 5
     * ```
     * Now, the only objects we should be touching with our operation are `a` and `b`, so we gather
     * them first into a set. That part's simple enough.
     * Then, for the variables we want to modify, we take our qualifier as specified above, and replace
     * `a` and `b` with either:
     *      a) The value already at that object, effectively turning `b` into `b.x`
     *      b) The value that we're setting this field to
     *  depending on whether the object we're modifying is the object being replaced in the expression.
     *  The result of this is that for something like `((x > 0) ? a : b).x = 5`, the variables end up
     *  like so:
     *      `a.x = { (x > 0) ? 5 : 3 }`
     *      `b.x = { (x > 0) ? 2 : 5 }`
     *  which is exactly as desired.
     */
    fun withVar(lExpr: LValueExpr<*>, rExpr: RootExpression<*>): Context {
        if (lExpr is LValueKeyExpr) {
            return withVar(lExpr.key, rExpr)
        } else if (lExpr !is LValueFieldExpr) {
            throw IllegalArgumentException("This cannot happen")
        }

        val qualifierExpr = lExpr.qualifier
        val field = lExpr.field

        val qualifiersMentionedInQualifierExpr: Set<Qualifier> =
            qualifierExpr.iterateTree()
                .filterIsInstance<LeafExpr<*>>()
                .map { it.underlying }
                .filterIsInstance<Qualifier>()
                .toSet()

        var newContext = this
        // For every distinct qualifier we mention...
        for (qualifier in qualifiersMentionedInQualifierExpr) {
            val thisVarKey = QualifiedFieldKey(qualifier, field)
            // grab whatever it's currently set to,
            val existingExpr = newContext.getVar(thisVarKey)

            val newValue = rExpr.mapDynamic {
                // and replace it with the qualifier expression itself, but with each leaf
                // replaced with either what we used to be, or [rExpr].
                qualifierExpr.replaceTypeInLeaves<LeafExpr<*>>(field.ind) { expr ->
                    if (expr.underlying == qualifier) {
                        it
                    } else {
                        existingExpr
                    }
                }.castOrThrow(it.ind)
            }

            // In the simple cases this will just perform a basic assignment, but
            // in reality under the hood it may do other stuff due to aliasing.
            newContext = newContext.withVar(thisVarKey, newValue)
        }

        return newContext
    }

    /**
     * This second private [withVar] handles any potential aliasing.
     * Using this alone should be perfectly correct.
     */
    private fun withVar(key: UnknownKey, rExpr: RootExpression<*>): Context {
        val newVariables = this.variables.toMutableMap()

        fun addExprToNewVariables(key: UnknownKey, expr: RootExpression<*>) {
            // We've checked for aliasing, we've done all of our pre-processing; it's finally
            // time to assign this expression to our variables.

            // First, check if we already have a value assigned to this key. If not, we pretend we did.
            val existingRootExpr = newVariables[key]
                ?: RootExpression.new(VariableExpr.new(KeyBackreference(key, idx)))

            // Stack the new expression on top. Stacking expressions combines their static expressions
            // and takes the top dynamic expression.
            newVariables += (key to existingRootExpr.stackedUnder(expr))
        }

        addExprToNewVariables(key, rExpr)

        if (key !is QualifiedFieldKey) {
            // No need to do anything further if there's no risk of aliasing.
            return Context(newVariables, thisType, idx)
        }

        val qualifier = key.qualifier
        val fieldKey = key.field
        val qualifierInd = key.qualifierInd

        // Collect a list of all objects we know of that could alias with the object we're trying to set.
        val potentialAliasers: Set<QualifiedFieldKey> = variables.keys
            .filterIsInstance<QualifiedFieldKey>()
            .filter {
                !it.isPlaceholder()
                        && qualifier != it.qualifier
                        && fieldKey == it.field
                        && qualifier.ind == it.qualifier.ind
            }
            .toSet() + QualifiedFieldKey(KeyBackreference(PlaceholderKey(qualifierInd), this.idx), fieldKey)

        for (aliasingKey in potentialAliasers) {
            val condition = ComparisonExpr.new(
                aliasingKey.qualifier.toLeafExpr(),
                qualifier.toLeafExpr(),
                ComparisonOp.EQUAL
            )

            // Each of the aliasers' values needs to be updated to take into account this new assignment,
            // as they may have been affected if it turns out they were equal.
            val newRExpr = rExpr.mapDynamic {
                // If the objects turn out to be the same, the aliasing object is set to whatever value we're
                // setting. Otherwise, we leave it alone.
                IfExpr.new(it, getVar(aliasingKey), condition)
            }

            addExprToNewVariables(aliasingKey, newRExpr)
        }

        return Context(newVariables, thisType, idx)
    }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> =
        resolveKnownVariables(RootExpression.new(expr)).getDynExpr()

    /**
     * Resolves all variables in the expression that are known of in this context.
     */
    private fun <T : Any> resolveKnownVariables(expr: RootExpression<T>): RootExpression<T> =
        expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(this)
        }.optimise()

    /**
     * Stacks the later context on top of this one.
     *
     * That is, prioritise the later context and fall back to this one if the key doesn't exist.
     */
    fun stack(other: Context): Context {
        var newContext = this

        for ((key, expr) in other.withoutPlaceholderKeys().variables) {
            // Resolve the expression...
            val expr = resolveKnownVariables(expr)

            // ...and any keys that might also need resolved...
            val lValue = if (key is QualifiedFieldKey) {
                val q = key.qualifier.safelyResolveUsing(this)
                assert(q.ind is ObjectSetIndicator)
                LValueFieldExpr.new(key.field, q)
            } else {
                LValueKeyExpr.new(key)
            }

            // ...and then assign to us.
            newContext = newContext.withVar(lValue, expr)
        }

        // Simple!
        return newContext.withoutTemporaryKeys()
    }

    fun haveHitReturn(): Context =
        this.mapVariables { key, expr ->
            expr.forcedStatic(VariableExpr.new(KeyBackreference(key, idx)))
        }

    private fun withoutTemporaryKeys(): Context = this.filterVariables { !it.temporary }
    private fun withoutPlaceholderKeys(): Context = this.filterVariables { !it.isPlaceholder() }
    fun withoutReturnValue(): Context = this.filterVariables { it !is ReturnKey }
    fun forcedDynamic(): Context = this.mapVariables { _, expr -> expr.forcedDynamic() }

    private fun filterVariables(predicate: (UnknownKey) -> Boolean): Context =
        Context(variables.filterKeys(predicate), thisType, idx)

    private fun mapVariables(transform: (UnknownKey, RootExpression<*>) -> RootExpression<*>): Context =
        Context(variables.mapValues { transform(it.key, it.value) }, thisType, idx)
}