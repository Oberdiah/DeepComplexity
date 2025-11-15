package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.context.Context.ContextId
import com.oberdiah.deepcomplexity.context.Context.KeyBackreference
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToContext
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToObject
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.staticAnalysis.ContextMarker
import com.oberdiah.deepcomplexity.utilities.Utilities
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
    val thisType: PsiType?,
    val idx: ContextId
) {
    companion object {
        fun brandNew(thisType: PsiType?): MetaContext {
            val contextIdx = ContextId.new()
            return MetaContext(ContextExpr(), Context(mapOf(), contextIdx), thisType, contextIdx)
        }

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            assertEquals(lhs.thisType, rhs.thisType, "Differing 'this' types in contexts.")
            return MetaContext(
                how(lhs.flowExpr, rhs.flowExpr).castToContext(),
                Context(
                    (lhs.ctx.variables.keys + rhs.ctx.variables.keys)
                        .associateWith { key ->
                            val doNothingExpr = VariableExpr.new(KeyBackreference(key, lhs.ctx.idx + rhs.ctx.idx))

                            val rhsExpr = rhs.ctx.variables[key] ?: doNothingExpr
                            val lhsExpr = lhs.ctx.variables[key] ?: doNothingExpr

                            val finalDynExpr = how(lhsExpr, rhsExpr)

                            finalDynExpr.castOrThrow(doNothingExpr.ind)
                        },
                    lhs.ctx.idx + rhs.ctx.idx
                ),
                lhs.thisType,
                lhs.idx + rhs.idx
            )
        }
    }

    override fun toString(): String = flowExpr.toString()
        .lines()
        .joinToString("\n") { line ->
            if (line.contains(ContextExpr.STRING_PLACEHOLDER)) {
                val indent = "# " + line.takeWhile { c -> c.isWhitespace() }
                Utilities.varsToString(ctx.variables).betterPrependIndent(indent)
            } else {
                line
            }
        }

    val keys = flowExpr.iterateTree<ContextExpr>().flatMap { (it.ctx ?: ctx).variables.keys }.toSet()

    val returnValue: Expr<*>? = ctx.variables.filterKeys { it is ReturnKey }.values.firstOrNull()

    private fun mapContexts(operation: (Context) -> Context): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInTree<ContextExpr> {
                if (it.ctx != null) ContextExpr(operation(it.ctx)) else it
            },
            operation(ctx),
            thisType,
            idx
        )
    }

    fun withoutReturnValue() = mapContexts { ctx ->
        Context(
            ctx.variables.filterKeys { it: UnknownKey -> it !is ReturnKey },
            ctx.idx
        )
    }

    fun stripKeys(lifetime: UnknownKey.Lifetime) =
        mapContexts { ctx ->
            Context(
                ctx.variables.filterKeys { it: UnknownKey -> !it.shouldBeStripped(lifetime) },
                ctx.idx
            )
        }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> {
        return expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(this)
        }.optimise()
    }

    fun getVar(key: UnknownKey): Expr<*> = ContextVarsAssistant.getVar(ctx.variables, key) {
        KeyBackreference(it, ctx.idx)
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext {
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)
        assert(rExpr.iterateTree<LValueExpr<*>>().none()) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }
        return MetaContext(flowExpr, Context(ContextVarsAssistant.withVar(ctx.variables, lExpr, rExpr) {
            KeyBackreference(it, ctx.idx)
        }, ctx.idx), thisType, idx)
    }

    fun stack(other: MetaContext): MetaContext {
        val other = other.stripKeys(UnknownKey.Lifetime.BLOCK)
        val stacked = other.mapContexts { other ->
            var newContext = ctx
            for ((key, expr) in other.variables) {
                // Resolve the expression...
                val expr = this.resolveKnownVariables(expr)

                // ...and any keys that might also need resolved...
                val lValue = if (key is QualifiedFieldKey) {
                    LValueFieldExpr.new(key.field, key.qualifier.safelyResolveUsing(this).castToObject())
                } else {
                    LValueKeyExpr.new(key)
                }

                // ...and then assign to us.
                newContext = Context(ContextVarsAssistant.withVar(newContext.variables, lValue, expr) {
                    KeyBackreference(it, newContext.idx)
                }, newContext.idx)
            }
            // Simple!
            newContext
        }

        val afterStack = MetaContext(
            flowExpr.replaceTypeInTree<ContextExpr> {
                if (it.ctx != null) {
                    it
                } else {
                    resolveKnownVariables(stacked.flowExpr)
                }
            },
            stacked.ctx,
            thisType,
            idx + other.idx
        )

        return afterStack
    }

    fun forcedDynamic(): MetaContext {
        val newVars: Vars = keys.associateWith { key ->
            flowExpr.replaceTypeInLeaves<ContextExpr>(key.ind) {
                val context = (it.ctx ?: ctx)
                ContextVarsAssistant.getVar(context.variables, key) {
                    KeyBackreference(it, context.idx)
                }
            }
        }

        return MetaContext(
            ContextExpr(),
            Context(newVars, idx),
            thisType,
            idx
        )
    }

    fun haveHitReturn(): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInTree<ContextExpr> {
                if (it.ctx != null) it else ContextExpr(ctx)
            },
            Context(mapOf(), idx),
            thisType,
            idx
        )
    }
}