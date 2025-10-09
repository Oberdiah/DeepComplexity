package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.intellij.psi.PsiType
import kotlin.test.assertEquals

typealias Vars = Map<UnknownKey, Expr<*>>

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
            VariableExpr.new(it.key.addContextId(idx))
        }
    }.mapKeys { it.key.addContextId(idx) }

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
        override fun addContextId(newId: ContextId): KeyBackreference =
            KeyBackreference(key.addContextId(newId), contextId + newId)

        override fun isNew(): Boolean = key.isNewlyCreated()
        fun isReturnExpr(): Boolean = key is ReturnKey

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

            return if (key is QualifiedKey && key.qualifier is KeyBackreference) {
                val q = key.qualifier.safelyResolveUsing(context)
                q.getField(context, key.field)
            } else {
                context.getVar(key)
            }
        }

        override fun toLeafExpr(): LeafExpr<*> = VariableExpr.new(this)

        fun isPlaceholder(): Boolean {
            return when (key) {
                is QualifiedKey -> key.isPlaceholder()
                else -> key is PlaceholderKey
            }
        }
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

                        // This equality is probably not very cheap.
                        // I'm sure that can be improved in the future.
                        if (aVal == bVal) {
                            // Safety: We know that at least one is not null, so both must be non-null in here.
                            aVal!!
                        } else if (aVal != null && bVal != null) {
                            how(aVal, bVal)
                        } else if (key.isNewlyCreated()) {
                            // Safety: We know at least one is not null.
                            aVal ?: bVal!!
                        } else {
                            how(
                                aVal ?: VariableExpr.new(KeyBackreference(key, a.idx)),
                                bVal ?: VariableExpr.new(KeyBackreference(key, b.idx))
                            )
                        }
                    },
                a.thisType,
                a.idx + b.idx
            )
        }
    }

    val returnValue: Expr<*>?
        get() = variables.filterKeys { it is ReturnKey }.values.firstOrNull()

    val returnKey: ReturnKey?
        get() = variables.keys.filterIsInstance<ReturnKey>().firstOrNull()

    val returnPair: Pair<ReturnKey, Expr<*>>?
        get() = returnKey?.let { Pair(it, returnValue!!) }

    override fun toString(): String {
        val variablesString =
            variables.entries.joinToString("\n") { entry ->
                val expr = entry.value
                val key = entry.key

                "$key:\n${expr.toString().prependIndent()}"
            }
        return "Context: {\n${variablesString.prependIndent()}\n}"
    }

    fun getVar(key: UnknownKey): Expr<*> {
        // If we have it, return it.
        variables[key]?.let { return it }

        // If we don't, before we create a new variable expression, we need to check in case there's a placeholder
        if (key is QualifiedKey) {
            val placeholderQualifierKey =
                KeyBackreference(PlaceholderKey(key.qualifier.ind as ObjectSetIndicator), this.idx)
            val placeholderVersionOfTheKey = QualifiedKey(key.field, placeholderQualifierKey)

            variables[placeholderVersionOfTheKey]?.let {
                val replacementQualified = VariableExpr.new(KeyBackreference(key, this.idx))
                val replacementRaw = key.qualifier.toLeafExpr()
                val p = KeyBackreference(placeholderVersionOfTheKey, this.idx)

                val replacedExpr = it.replaceTypeInTree<VariableExpr<*>> { expr ->
                    if (expr.key == p) {
                        replacementQualified
                    } else if (expr.key == placeholderQualifierKey) {
                        replacementRaw
                    } else {
                        null
                    }
                }

                return replacedExpr
            }
        }

        // OK, now we really do have no choice
        return VariableExpr.new(KeyBackreference(key, idx))
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): Context {
        assert(rExpr.iterateTree().none { it is LValueExpr }) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }

        if (lExpr is LValueKeyExpr) {
            return withVar(lExpr.key, rExpr)
        } else if (lExpr !is LValueFieldExpr) {
            throw IllegalArgumentException("This cannot happen")
        }

        /**
         * This does look a bit scary, so I'll try to walk you through it:
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
         * them first into [varKeysInvolved]. That part's simple enough.
         *
         * Then, for the variables we want to modify, we take our qualifier as specified above, and replace
         * `a` and `b` with either:
         *      a) The value already at that object, effectively turning `b` into `b.x`
         *      b) The value that we're setting this field to
         *  depending on whether the object we're modifying is the object being replaced in the expression.
         *
         *  The result of this is that for something like `((x > 0) ? a : b).x = 5`, the variables end up
         *  like so:
         *      `a.x = { (x > 0) ? 5 : 3 }`
         *      `b.x = { (x > 0) ? 2 : 5 }`
         *  which is exactly as desired.
         */

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
            val thisVarKey = QualifiedKey(field, qualifier)
            // grab whatever it's currently set to,
            val existingExpr = getVar(thisVarKey)
            // and replace it with the qualifier expression itself, but with each leaf
            // replaced with either what we used to be, or [rExpr].
            val newValue = qualifierExpr.replaceTypeInLeaves<LeafExpr<*>>(field.ind) { expr ->
                if (expr.underlying == qualifier) {
                    rExpr
                } else {
                    existingExpr
                }
            }

            newContext = newContext.withVar(thisVarKey, newValue)
        }
        
        return newContext
    }

    private fun withVar(key: UnknownKey, uncastExpr: Expr<*>): Context {
        val rExpr = uncastExpr.castToUsingTypeCast(key.ind, false)

        if (key !is QualifiedKey) {
            return Context(variables + (key to rExpr), thisType, idx)
        }

        val newVariables = (variables + (key to rExpr)).toMutableMap()

        val qualifier = key.qualifier
        val fieldKey = key.field
        val qualifierInd = key.qualifierInd

        val candidates: Set<Qualifier> = variables.keys
            .filterIsInstance<QualifiedKey>()
            .filter { !it.isPlaceholder() }
            .map { it.qualifier }
            .filter { qualifier != it && qualifier.ind == it.ind }
            .toSet() + KeyBackreference(PlaceholderKey(qualifierInd), this.idx)

        for (k in candidates) {
            fun <T : Any, Q : Any> inner(exprInd: SetIndicator<T>, qualifierInd: SetIndicator<Q>): Expr<T> {
                val trueExpr = rExpr.tryCastTo(exprInd)!!
                val falseExpr = getVar(QualifiedKey(fieldKey, k)).tryCastTo(exprInd)!!

                val condition = ComparisonExpr(
                    k.toLeafExpr().tryCastTo(qualifierInd)!!,
                    qualifier.toLeafExpr().tryCastTo(qualifierInd)!!,
                    ComparisonOp.EQUAL
                )

                return IfExpr(trueExpr, falseExpr, condition)
            }

            val newRExpr = inner(rExpr.ind, qualifier.ind)

            newVariables[QualifiedKey(fieldKey, k)] = newRExpr
        }

        return Context(newVariables, thisType, idx)
    }

    private fun withVariablesResolvedBy(resolver: Context): Context {
        return Context(
            variables.mapValues { (_, expr) ->
                resolver.resolveKnownVariables(expr)
            },
            thisType,
            idx
        )
    }

    /**
     * Resolves all variables in the expression that are known of in this context.
     */
    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> =
        expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(this)
        }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, prioritise the later context and fall back to this one if the key doesn't exist.
     */
    fun stack(other: Context): Context {
        // Gotta not resolve returns here, as that wouldn't be the backward resolve that returns are supposed to be.
        // Returns still need to have other variables in their expressions resolved, though.
        val resolvedOther = other.stripPlaceholderKeys().withVariablesResolvedBy(withoutReturnValue())

        // ...then we add that newly resolved return value to the new context,
        var newContext = resolvedOther.returnPair?.let { withAdditionalReturn(it.first, it.second) } ?: this

        // finally, we avoid stomping over that newly created return value.
        for ((key, expr) in resolvedOther.withoutReturnValue().variables) {
            val lValue = if (key is QualifiedKey) {
                LValueFieldExpr.new(key.field, key.qualifier.safelyResolveUsing(this))
            } else {
                // Do nothing, just assign as normal.
                LValueKeyExpr.new(key)
            }

            newContext = newContext.withVar(lValue, expr)
        }

        return newContext.stripTemporaryKeys()
    }

    /**
     * Adds the return onto this context in the manner that returns should be added;
     * that is, if this context already has a return, this new one is substituted into the old.
     */
    fun withAdditionalReturn(returnKey: ReturnKey, expr: Expr<*>): Context {
        val newRetExpr = returnValue?.let { returnValue ->
            returnValue.replaceTypeInTree<VariableExpr<*>> {
                if (it.key.isReturnExpr()) expr else null
            }
        } ?: expr

        return Context(variables + (returnKey to newRetExpr), thisType, idx)
    }

    private fun stripTemporaryKeys(): Context {
        return Context(variables.filterKeys { !it.temporary }, thisType, idx)
    }

    private fun stripPlaceholderKeys(): Context {
        return Context(
            variables.filterKeys {
                val isPlaceholder = it is PlaceholderKey || (it is QualifiedKey && it.isPlaceholder())
                !isPlaceholder
            },
            thisType,
            idx
        )
    }

    fun withoutReturnValue(): Context {
        return Context(variables.filterKeys { it !is ReturnKey }, thisType, idx)
    }
}