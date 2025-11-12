package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.evaluation.ContextExpr
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LValueExpr

class MetaContext(
    private val flowExpr: Expr<*>,
    private val ctx: Context
) {
    companion object {
        fun brandNew(thisType: PsiType?): MetaContext =
            MetaContext(ContextExpr(), Context.brandNew(thisType))

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            return MetaContext(
                lhs.flowExpr,
                Context.combine(lhs.ctx, rhs.ctx, how)
            )
        }
    }

    val thisType = ctx.thisType
    val returnValue = ctx.returnValue
    fun withoutReturnValue() = MetaContext(flowExpr, ctx.withoutReturnValue())
    fun stripKeys(lifetime: UnknownKey.Lifetime) = MetaContext(flowExpr, ctx.stripKeys(lifetime))
    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> = ctx.resolveKnownVariables(expr)

    fun getVar(key: UnknownKey): Expr<*> = ctx.getVar(key)

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext =
        MetaContext(flowExpr, ctx.withVar(lExpr, rExpr))

    fun stack(metaContext: MetaContext) =
        MetaContext(flowExpr, ctx.stack(metaContext.ctx))

    fun forcedDynamic(): MetaContext {
        return this
    }

    fun haveHitReturn(): MetaContext {
        return this
    }

    fun grabContext(): Context = ctx
}