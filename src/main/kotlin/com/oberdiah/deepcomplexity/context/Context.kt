package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToObject
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.getField
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
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
        fun withAddedContextId(newId: ContextId): KeyBackreference =
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

        fun isPlaceholder(): Boolean = key.isPlaceholder()
        override val lifetime: UnknownKey.Lifetime
            get() = key.lifetime
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
        return ContextVarsAssistant.getVar(variables, key) {
            KeyBackreference(it, this.idx)
        }
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

    fun withVar(lExpr: LValueExpr<*>, rExpr: RootExpression<*>): Context =
        Context(ContextVarsAssistant.withVar(variables, lExpr, rExpr) {
            KeyBackreference(it, this.idx)
        }, thisType, idx)

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

        val other = other.stripKeys(UnknownKey.Lifetime.BLOCK)

        for ((key, expr) in other.variables) {
            // Resolve the expression...
            val expr = resolveKnownVariables(expr)

            // ...and any keys that might also need resolved...
            val lValue = if (key is QualifiedFieldKey) {
                LValueFieldExpr.new(key.field, key.qualifier.safelyResolveUsing(this).castToObject())
            } else {
                LValueKeyExpr.new(key)
            }

            // ...and then assign to us.
            newContext = newContext.withVar(lValue, expr)
        }

        // Simple!
        return newContext
    }

    fun haveHitReturn(): Context =
        this.mapVariables { key, expr ->
            expr.forcedStatic(VariableExpr.new(KeyBackreference(key, idx)))
        }

    fun stripKeys(lifetime: UnknownKey.Lifetime): Context = this.filterVariables { !it.shouldBeStripped(lifetime) }
    fun withoutReturnValue(): Context = this.filterVariables { it !is ReturnKey }
    fun forcedDynamic(): Context = this.mapVariables { _, expr -> expr.forcedDynamic() }

    private fun filterVariables(predicate: (UnknownKey) -> Boolean): Context =
        Context(variables.filterKeys(predicate), thisType, idx)

    private fun mapVariables(transform: (UnknownKey, RootExpression<*>) -> RootExpression<*>): Context =
        Context(variables.mapValues { transform(it.key, it.value) }, thisType, idx)
}