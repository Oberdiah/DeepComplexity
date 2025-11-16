package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToContext
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToObject
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import kotlin.test.assertEquals

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
 *    Confusingly it is also OK to resolve `b.x` using { b: a', a'.x: 2 } to `2` - although `b` is `a'` and not `a`,
 *    `a.x` is also actually `a'.x`.
 *  - ObjectExprs can be considered a ConstExpr, except that their values can be used to build QualifiedKeys.
 */
class MetaContext private constructor(
    val i: InnerCtx,
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
    val idx: ContextId
) {
    companion object {
        fun brandNew(thisType: PsiType?): MetaContext {
            val contextIdx = ContextId.new()
            return MetaContext(InnerCtx.new(contextIdx), thisType, contextIdx)
        }

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            assertEquals(lhs.thisType, rhs.thisType, "Differing 'this' types in contexts.")
            return MetaContext(
                InnerCtx.combine(
                    lhs.idx + rhs.idx,
                    lhs.i, rhs.i,
                    { lhs, rhs -> how(lhs, rhs).castToContext() },
                    { vars1, vars2 ->
                        Vars.combine(vars1, vars2) { lhsVars, rhsVars ->
                            (lhsVars.keys + rhsVars.keys)
                                .associateWith { key ->
                                    val doNothingExpr = VariableExpr.new(KeyBackreference(key, lhs.idx + rhs.idx))

                                    val lhsExpr = lhsVars[key] ?: doNothingExpr
                                    val rhsExpr = rhsVars[key] ?: doNothingExpr

                                    val finalDynExpr = how(lhsExpr, rhsExpr)

                                    finalDynExpr.castOrThrow(doNothingExpr.ind)
                                }
                        }
                    }
                ),
                lhs.thisType,
                lhs.idx + rhs.idx
            )
        }
    }

    val returnValue: Expr<*>? = i.dynamicVars.returnValue

    override fun toString(): String = i.toString()
    fun forcedDynamic(): MetaContext = MetaContext(i.forcedDynamic(), thisType, idx)
    fun haveHitReturn(): MetaContext = MetaContext(i.forcedStatic(), thisType, idx)

    fun getVar(key: UnknownKey): Expr<*> = i.dynamicVars.get(key)

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext {
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)
        assert(rExpr.iterateTree<LValueExpr<*>>().none()) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }
        return MetaContext(i.mapDynamicVars { vars -> vars.with(lExpr, rExpr) }, thisType, idx)
    }

    private fun mapVars(operation: (Vars) -> Vars): MetaContext =
        MetaContext(i.mapAllVars(operation), thisType, idx)

    fun withoutReturnValue() = mapVars { vars -> vars.filterKeys { it !is ReturnKey } }

    fun stripKeys(lifetime: UnknownKey.Lifetime) =
        mapVars { vars -> vars.filterKeys { !it.shouldBeStripped(lifetime) } }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> {
        return expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(this)
        }.optimise()
    }

    fun stack(other: MetaContext): MetaContext {
        val other = other
            .stripKeys(UnknownKey.Lifetime.BLOCK)
            .mapVars { other ->
                var newVars = i.dynamicVars
                other.forEach { key, expr ->
                    // First, resolve any unknown variables in the expression...
                    val expr = resolveKnownVariables(expr)

                    // ...and in any keys that might also need resolved...
                    val lValue = if (key is QualifiedFieldKey) {
                        LValueFieldExpr.new(key.field, key.qualifier.safelyResolveUsing(this).castToObject())
                    } else {
                        LValueKeyExpr.new(key)
                    }

                    // ...and then assign to us.
                    newVars = newVars.with(lValue, expr)
                }

                // Simple!
                newVars
            }

        val afterStack = MetaContext(
            InnerCtx.combine(
                idx + other.idx,
                i, other.i,
                { thisExpr, otherExpr ->
                    thisExpr.replaceTypeInTree<VarsExpr> {
                        if (it.vars != null) it else resolveKnownVariables(otherExpr)
                    }
                },
                { _, otherVars -> otherVars }
            ),
            thisType,
            idx + other.idx
        )

        return afterStack
    }
}