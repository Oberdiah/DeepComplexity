package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.Key.QualifiedKey
import com.intellij.psi.PsiType
import kotlin.test.assertEquals

typealias Vars = Map<Key.UncertainKey, Expr<*>>

/**
 * A potentially subtle but important point is that an unknown variable in a context never
 * refers to another variable within that same context.
 *
 * For example, in a context `{ x: y + 1, y: 2}`, the variable `x` is not equal to `2 + 1`, it's equal to `y' + 1`.
 *
 * Vars are the currently understood values for things that can be modified.
 *
 * How objects are stored:
 *  - Every object's field will be stored as its own variable.
 *  - An object itself (the heap reference) may also be an unknown. e.g. { x: a }
 *  - As always, unknown variables never point to our own context.
 *    { x: b, b.x: 2 } does not mean that you're allowed to resolve `x.x` to `2`, because `x` is not `b`, it's `b'`.
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
    val variables: Vars = variables.mapValues { expr ->
        expr.value.replaceTypeInTree<VariableExpr<*>> {
            VariableExpr.new(it.key, it.contextId + idx)
        }
    }

    init {
        assert(variables.keys.filterIsInstance<Key.ReturnKey>().size <= 1) {
            "A context cannot have multiple return keys."
        }
    }

    @JvmInline
    value class ContextId(val ids: Set<Int>) {
        operator fun plus(other: ContextId): ContextId = ContextId(this.ids + other.ids)
        fun collidesWith(other: ContextId): Boolean = this.ids.any { it in other.ids }

        companion object {
            var ID_INDEX = 0
            fun new(): ContextId = ContextId(setOf(ID_INDEX++))
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
                                aVal ?: VariableExpr.new(key, a.idx),
                                bVal ?: VariableExpr.new(key, b.idx)
                            )
                        }
                    },
                a.thisType,
                a.idx + b.idx
            )
        }
    }

    val returnValue: Expr<*>?
        get() = variables.filterKeys { it is Key.ReturnKey }.values.firstOrNull()

    val returnKey: Key.ReturnKey?
        get() = variables.keys.filterIsInstance<Key.ReturnKey>().firstOrNull()

    override fun toString(): String {
        val variablesString =
            variables.entries.joinToString("\n") { entry ->
                val expr = entry.value
                val key = entry.key

                "$key:\n${expr.toString().prependIndent()}"
            }
        return "Context: {\n${variablesString.prependIndent()}\n}"
    }

    fun grabVar(key: Key.UncertainKey): Expr<*> {
        return variables[key] ?: VariableExpr.new(key, idx)
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): Context {
        assert(rExpr.iterateTree().none { it is LValueExpr }) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }

        if (lExpr is LValueKeyExpr) {
            val castVar = rExpr.castToUsingTypeCast(lExpr.key.ind, false)
            return Context(variables + (lExpr.key to castVar), thisType, idx)
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

        val qualifier = lExpr.qualifier
        val fieldKey = lExpr.field

        val objectsMentionedInQualifier =
            qualifier.iterateTree()
                .filterIsInstance<LeafExprWithKey>()
                .map { it.key }
                .toSet()

        val newVariables = variables + objectsMentionedInQualifier.map {
            val thisVarKey = QualifiedKey(fieldKey, it)
            val newValue = qualifier.replaceTypeInLeaves<LeafExprWithKey>(fieldKey.ind) { expr ->
                // Now that I look at this, this is slightly suspicious; we're checking a variable key
                // against potentially other variables within the same context.
                if (expr.key == it) {
                    rExpr
                } else {
                    grabVar(thisVarKey)
                }
            }

            (thisVarKey to newValue)
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
            assert(!varExpr.contextId.collidesWith(idx)) {
                "Cannot resolve variables from the same context that created them."
            }

            if (varExpr.key is QualifiedKey && varExpr.key.qualifier is Key.UncertainKey) {
                val q = grabVar(varExpr.key.qualifier)
                q.getField(this, varExpr.key.field)
            } else {
                grabVar(varExpr.key)
            }
        }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, prioritise the later context and fall back to this one if the key doesn't exist.
     */
    fun stack(other: Context): Context {
        // Gotta not resolve returns here, as that wouldn't be the backward resolve that returns are supposed to be.
        // Returns still need to have other variables in their expressions resolved, though.
        val resolvedOther = other.withVariablesResolvedBy(withoutReturnValue())

        // ...then we add that newly resolved return value to the new context,
        var newContext = withAdditionalReturn(resolvedOther.returnKey, resolvedOther.returnValue)

        // finally, we avoid stomping over that return value.
        for ((key, expr) in resolvedOther.withoutReturnValue().variables) {
            val lValue = if (key is QualifiedKey) {
                LValueFieldExpr.new(
                    key.field,
                    when (key.qualifier) {
                        is HeapMarker -> ObjectExpr(key.qualifier)
                        is Key.UncertainKey -> grabVar(key.qualifier)
                    }
                )
            } else {
                // Do nothing, just assign as normal.
                LValueKeyExpr.new(key)
            }

            newContext = newContext.withVar(lValue, expr)
        }

        newContext = newContext.stripTemporaryKeys()

        return newContext
    }

    /**
     * Adds the return onto this context in the manner that returns should be added;
     * that is, if this context already has a return, this new one is substituted into the old.
     */
    fun withAdditionalReturn(key: Key.ReturnKey?, expr: Expr<*>?): Context {
        val retKey = this.returnKey
            ?: key
            ?: return this

        val newRetExpr = returnValue?.resolveKeyAs(retKey, expr)
            ?: expr
            ?: return this

        return Context(variables + (retKey to newRetExpr), thisType, idx)
    }

    private fun stripTemporaryKeys(): Context {
        return Context(variables.filterKeys { !it.temporary }, thisType, idx)
    }

    fun withoutReturnValue(): Context {
        return Context(variables.filterKeys { it !is Key.ReturnKey }, thisType, idx)
    }
}