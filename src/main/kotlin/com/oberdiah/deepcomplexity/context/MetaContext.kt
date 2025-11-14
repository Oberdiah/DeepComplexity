package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.evaluation.ContextExpr
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToContext
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.evaluation.LValueExpr
import com.oberdiah.deepcomplexity.staticAnalysis.ContextMarker
import com.oberdiah.deepcomplexity.utilities.Utilities.betterPrependIndent
import kotlin.test.assertEquals

class MetaContext(
    private val flowExpr: Expr<ContextMarker>,
    private val ctx: Context,
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
    val thisType: PsiType?
) {
    companion object {
        fun brandNew(thisType: PsiType?): MetaContext =
            MetaContext(ContextExpr(), Context.brandNew(), thisType)

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            assertEquals(
                lhs.thisType,
                rhs.thisType,
                "Cannot combine contexts with different 'this' types."
            )
            return MetaContext(
                how(lhs.flowExpr, rhs.flowExpr).castToContext(),
                Context.combine(lhs.ctx, rhs.ctx, how),
                lhs.thisType
            )
        }
    }

    override fun toString(): String = flowExpr.toString()
        .lines()
        .joinToString("\n") { line ->
            if (line.contains(ContextExpr.STRING_PLACEHOLDER)) {
                val indent = "# " + line.takeWhile { c -> c.isWhitespace() }
                ctx.toString().betterPrependIndent(indent)
            } else {
                line
            }
        }

    val returnValue: Expr<*>? = ctx.returnValue

    private fun mapContexts(operation: (Context) -> Context): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInTree<ContextExpr> {
                if (it.ctx != null) ContextExpr(operation(it.ctx)) else it
            },
            operation(ctx),
            thisType
        )
    }

    fun grabContext(): Context = ctx
    fun withoutReturnValue() = mapContexts { it.withoutReturnValue() }
    fun stripKeys(lifetime: UnknownKey.Lifetime) = mapContexts { it.stripKeys(lifetime) }
    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> = ctx.resolveKnownVariables(expr)
    fun getVar(key: UnknownKey): Expr<*> = ctx.getVar(key)
    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext =
        MetaContext(flowExpr, ctx.withVar(lExpr, rExpr), thisType)

    fun stack(other: MetaContext): MetaContext {
        val stacked = other.mapContexts { ctx.stack(it) }

        val afterStack = MetaContext(
            flowExpr.replaceTypeInTree<ContextExpr> {
                if (it.ctx != null) {
                    it
                } else {
                    resolveKnownVariables(stacked.flowExpr)
                }
            },
            stacked.ctx,
            thisType
        )

        return afterStack
    }

    fun forcedDynamic(): MetaContext {
        val allKeys = mutableSetOf<UnknownKey>()
        allKeys.addAll(ctx.variables.keys)
        flowExpr.iterateTree(false).forEach {
            if (it is ContextExpr && it.ctx != null) {
                allKeys.addAll(it.ctx.variables.keys)
            }
        }

        val newVars: Vars = allKeys.associateWith { key ->
            flowExpr.replaceTypeInLeaves<ContextExpr>(key.ind) {
                (it.ctx ?: ctx).getVar(key)
            }
        }

        return MetaContext(
            ContextExpr(),
            Context(newVars, ctx.idx),
            thisType
        )
    }

    fun haveHitReturn(): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInTree<ContextExpr> {
                if (it.ctx != null) {
                    it
                } else {
                    ContextExpr(ctx)
                }
            },
            Context.brandNew(),
            thisType
        )
    }
}