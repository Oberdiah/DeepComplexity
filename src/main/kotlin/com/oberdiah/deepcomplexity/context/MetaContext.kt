package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.evaluation.ContextExpr
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
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
                how(lhs.flowExpr, rhs.flowExpr),
                Context.combine(lhs.ctx, rhs.ctx, how)
            )
        }
    }

    override fun toString(): String {
        val flowExprString = flowExpr.toString()
        val lines = flowExprString.split("\n")
        val newLines = mutableListOf<String>()
        for (line in lines) {
            if (line.contains(ContextExpr.STRING_PLACEHOLDER)) {
                val indent = "# " + line.takeWhile { it.isWhitespace() }
                newLines.add(ctx.toString().prependIndent(indent))
            } else {
                newLines.add(line)
            }
        }
        return newLines.joinToString("\n")
    }

    val thisType = ctx.thisType
    val returnValue: Expr<*>?
        get() {
            return getVar(ReturnKey(ctx.returnValue?.ind ?: return null))
        }

    private fun mapContexts(operation: (Context) -> Context): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInLeaves<ContextExpr>(ContextExpr().ind) {
                if (it.ctx != null) ContextExpr(operation(it.ctx)) else it
            },
            operation(ctx)
        )
    }

    fun withoutReturnValue() = mapContexts { it.withoutReturnValue() }
    fun stripKeys(lifetime: UnknownKey.Lifetime) = mapContexts { it.stripKeys(lifetime) }
    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> = ctx.resolveKnownVariables(expr)

    fun getVar(key: UnknownKey): Expr<*> = flowExpr.replaceTypeInLeaves<ContextExpr>(key.ind) {
        (it.ctx ?: ctx).getVar(key)
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext =
        MetaContext(flowExpr, ctx.withVar(lExpr, rExpr))

    fun stack(metaContext: MetaContext): MetaContext {
        val otherResolvedFlowExpr =
            metaContext.flowExpr.resolveUnknowns(this).replaceTypeInLeaves<ContextExpr>(ContextExpr().ind) {
                if (it.ctx != null) {
                    ContextExpr(it.ctx.resolveUsingOtherCtx(ctx))
                } else {
                    it
                }
            }

        val afterStack = MetaContext(
            flowExpr.replaceTypeInLeaves<ContextExpr>(ContextExpr().ind) {
                if (it.ctx != null) {
                    it
                } else {
                    otherResolvedFlowExpr
                }
            },
            ctx.stack(metaContext.ctx)
        )

        return afterStack
    }

    fun forcedDynamic(): MetaContext {
        val allKeys = mutableSetOf<UnknownKey>()
        flowExpr.iterateTree(false).forEach {
            if (it is ContextExpr && it.ctx != null) {
                allKeys.addAll(it.ctx.variables.keys)
            }
        }

        val newVars: Vars = allKeys.associateWith { getVar(it) }

        return MetaContext(
            ContextExpr(),
            Context(newVars, thisType, ctx.idx)
        )
    }

    fun haveHitReturn(): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInLeaves<ContextExpr>(ContextExpr().ind) {
                if (it.ctx != null) {
                    it
                } else {
                    ContextExpr(ctx)
                }
            },
            Context.brandNew(thisType)
        )
    }

    fun grabContext(): Context = ctx
}