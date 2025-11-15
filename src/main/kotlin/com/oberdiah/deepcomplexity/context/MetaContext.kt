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
import kotlin.test.assertEquals

class MetaContext(
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
            return MetaContext(InnerCtx.new(), thisType, contextIdx)
        }

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            assertEquals(lhs.thisType, rhs.thisType, "Differing 'this' types in contexts.")
            return MetaContext(
                InnerCtx(
                    how(lhs.i.staticExpr, rhs.i.staticExpr).castToContext(),
                    (lhs.i.dynamicVars.keys + rhs.i.dynamicVars.keys)
                        .associateWith { key ->
                            val doNothingExpr = VariableExpr.new(KeyBackreference(key, lhs.idx + rhs.idx))

                            val rhsExpr = rhs.i.dynamicVars[key] ?: doNothingExpr
                            val lhsExpr = lhs.i.dynamicVars[key] ?: doNothingExpr

                            val finalDynExpr = how(lhsExpr, rhsExpr)

                            finalDynExpr.castOrThrow(doNothingExpr.ind)
                        }
                ),
                lhs.thisType,
                lhs.idx + rhs.idx
            )
        }
    }

    override fun toString(): String = i.toString()

    val returnValue: Expr<*>? = i.dynamicVars.filterKeys { it is ReturnKey }.values.firstOrNull()

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

    fun getVar(key: UnknownKey): Expr<*> = ContextVarsAssistant.getVar(i.dynamicVars, key) {
        KeyBackreference(it, idx)
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext {
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)
        assert(rExpr.iterateTree<LValueExpr<*>>().none()) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }
        return MetaContext(
            i.mapDynamicVars { vars ->
                ContextVarsAssistant.withVar(vars, lExpr, rExpr) {
                    KeyBackreference(it, idx)
                }
            },
            thisType,
            idx
        )
    }

    fun stack(other: MetaContext): MetaContext {
        val other = other.stripKeys(UnknownKey.Lifetime.BLOCK)
        val stacked = other.mapVars { other ->
            var newVars = i.dynamicVars
            for ((key, expr) in other) {
                // Resolve the expression...
                val expr = this.resolveKnownVariables(expr)

                // ...and any keys that might also need resolved...
                val lValue = if (key is QualifiedFieldKey) {
                    LValueFieldExpr.new(key.field, key.qualifier.safelyResolveUsing(this).castToObject())
                } else {
                    LValueKeyExpr.new(key)
                }

                // ...and then assign to us.
                newVars = ContextVarsAssistant.withVar(newVars, lValue, expr) {
                    KeyBackreference(it, idx)
                }
            }
            // Simple!
            newVars
        }

        val afterStack = MetaContext(
            InnerCtx(
                i.staticExpr.replaceTypeInTree<VarsExpr> {
                    if (it.vars != null) {
                        it
                    } else {
                        resolveKnownVariables(stacked.i.staticExpr)
                    }
                },
                stacked.i.dynamicVars
            ),
            thisType,
            idx + other.idx
        )

        return afterStack
    }

    fun forcedDynamic(): MetaContext {
        return MetaContext(
            InnerCtx(
                VarsExpr(),
                i.keys.associateWith { key ->
                    i.staticExpr.replaceTypeInLeaves<VarsExpr>(key.ind) {
                        val context = (it.vars ?: i.dynamicVars)
                        ContextVarsAssistant.getVar(context, key) { key ->
                            KeyBackreference(key, idx)
                        }
                    }
                }
            ),
            thisType,
            idx
        )
    }

    fun haveHitReturn(): MetaContext {
        return MetaContext(
            InnerCtx(
                i.staticExpr.replaceTypeInTree<VarsExpr> {
                    if (it.vars != null) it else VarsExpr(i.dynamicVars)
                },
                mapOf()
            ),
            thisType,
            idx
        )
    }
}