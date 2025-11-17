package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToContext
import com.oberdiah.deepcomplexity.evaluation.LValue
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.evaluation.VarsExpr
import kotlin.test.assertEquals

/**
 * A potentially subtle but important point is that an unknown variable in a context never
 * refers to another variable within that same context.
 *
 * For example, in a context `{ x: y' + 1, y: 2}`, the variable `x` is not equal to `2 + 1`, it's equal to `y' + 1`.
 *
 * We encode this with the [KeyBackreference] class, representing an unknown that this context will never manage
 * to resolve.
 *
 * How objects are stored:
 *  - Every object's field will be stored as its own variable.
 *  - An object itself (the heap reference) may also be an unknown. e.g. { x: a }
 *  - Critically: HeapMarkers are not unknowns. They are constants that point to a specific object, so they do
 *    not have a previous or future state. E.g. in { #1.x: 2, x: #1 }, it is completely OK to resolve `x.x` to `2`.
 *    This means it is also OK to resolve `b.x` using { b: a', a'.x: 2 } to `2` - although `b` is `a'` and not `a`,
 *    `a.x` is also actually `a'.x`.
 *  - ObjectExprs can be considered a ConstExpr, except that their values can be used to build QualifiedKeys.
 */
class Context private constructor(
    private val inner: InnerCtx,
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
    companion object {
        fun brandNew(thisType: PsiType?): Context {
            val contextIdx = ContextId.new()
            return Context(InnerCtx.new(contextIdx), thisType, contextIdx)
        }

        /**
         * Merges the two contexts, combining variables with identical [UnknownKey]s using [how]. This is
         * primarily used to combine two branches of an if-statement.
         */
        fun combine(lhs: Context, rhs: Context, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): Context {
            assertEquals(lhs.thisType, rhs.thisType, "Differing 'this' types in contexts.")
            return Context(
                InnerCtx.combine(
                    lhs.inner, rhs.inner,
                    { lhs, rhs -> how(lhs, rhs).castToContext() },
                    { vars1, vars2 ->
                        Vars.combine(vars1, vars2) { expr1, expr2 ->
                            how(expr1, expr2)
                        }
                    }
                ),
                lhs.thisType,
                lhs.idx + rhs.idx
            )
        }
    }

    val returnValue: Expr<*>? = inner.dynamicVars.returnValue

    override fun toString(): String = inner.toString()
    fun forcedDynamic(): Context = Context(inner.forcedDynamic(idx), thisType, idx)
    fun haveHitReturn(): Context = Context(inner.forcedStatic(idx), thisType, idx)

    fun <T : Any> get(lValue: LValue<T>): Expr<T> = inner.dynamicVars.get(lValue)
    fun get(key: UnknownKey): Expr<*> = inner.dynamicVars.get(key)

    fun withVar(lExpr: LValue<*>, rExpr: Expr<*>): Context =
        Context(inner.mapDynamicVars { vars -> vars.with(lExpr, rExpr) }, thisType, idx)


    private fun mapVars(operation: (Vars) -> Vars): Context =
        Context(inner.mapAllVars(operation), thisType, idx)

    fun withoutReturnValue() = mapVars { vars -> vars.filterKeys { it !is ReturnKey } }

    fun stripKeys(lifetime: UnknownKey.Lifetime) =
        mapVars { vars -> vars.filterKeys { !it.shouldBeStripped(lifetime) } }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> {
        return expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(inner.dynamicVars)
        }.replaceTypeInTree<VarsExpr> { varsExpr ->
            varsExpr.map { it.resolveUsing(this) }
        }.optimise()
    }

    fun stack(other: Context): Context {
        val otherInner = other
            .stripKeys(UnknownKey.Lifetime.BLOCK)
            .inner
            .resolveUsing(this)
            .mapAllVars { other -> inner.dynamicVars.stack(other) }

        val afterStack = Context(
            InnerCtx.combine(
                inner, otherInner,
                { thisExpr, otherExpr ->
                    thisExpr.replaceTypeInTree<VarsExpr> {
                        if (it.vars != null) it else otherExpr
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