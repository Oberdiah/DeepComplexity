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
import com.oberdiah.deepcomplexity.staticAnalysis.VarsMarker
import com.oberdiah.deepcomplexity.utilities.Utilities
import com.oberdiah.deepcomplexity.utilities.Utilities.betterPrependIndent
import kotlin.test.assertEquals

class MetaContext(
    private val flowExpr: Expr<VarsMarker>,
    private val vars: Vars,
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
            return MetaContext(VarsExpr(), mapOf(), thisType, contextIdx)
        }

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            assertEquals(lhs.thisType, rhs.thisType, "Differing 'this' types in contexts.")
            return MetaContext(
                how(lhs.flowExpr, rhs.flowExpr).castToContext(),
                (lhs.vars.keys + rhs.vars.keys)
                    .associateWith { key ->
                        val doNothingExpr = VariableExpr.new(KeyBackreference(key, lhs.idx + rhs.idx))

                        val rhsExpr = rhs.vars[key] ?: doNothingExpr
                        val lhsExpr = lhs.vars[key] ?: doNothingExpr

                        val finalDynExpr = how(lhsExpr, rhsExpr)

                        finalDynExpr.castOrThrow(doNothingExpr.ind)
                    },
                lhs.thisType,
                lhs.idx + rhs.idx
            )
        }
    }

    override fun toString(): String = flowExpr.toString()
        .lines()
        .joinToString("\n") { line ->
            if (line.contains(VarsExpr.STRING_PLACEHOLDER)) {
                val indent = "# " + line.takeWhile { c -> c.isWhitespace() }
                Utilities.varsToString(vars).betterPrependIndent(indent)
            } else {
                line
            }
        }

    val keys = flowExpr.iterateTree<VarsExpr>().flatMap { (it.vars ?: vars).keys }.toSet()
    val returnValue: Expr<*>? = vars.filterKeys { it is ReturnKey }.values.firstOrNull()

    private fun mapVars(operation: (Vars) -> Vars): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInTree<VarsExpr> {
                if (it.vars != null) VarsExpr(operation(it.vars)) else it
            },
            operation(vars),
            thisType,
            idx
        )
    }

    fun withoutReturnValue() = mapVars { vars -> vars.filterKeys { it !is ReturnKey } }

    fun stripKeys(lifetime: UnknownKey.Lifetime) =
        mapVars { vars ->
            vars.filterKeys { it: UnknownKey -> !it.shouldBeStripped(lifetime) }
        }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> {
        return expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(this)
        }.optimise()
    }

    fun getVar(key: UnknownKey): Expr<*> = ContextVarsAssistant.getVar(vars, key) {
        KeyBackreference(it, idx)
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext {
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)
        assert(rExpr.iterateTree<LValueExpr<*>>().none()) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }
        return MetaContext(flowExpr, ContextVarsAssistant.withVar(vars, lExpr, rExpr) {
            KeyBackreference(it, idx)
        }, thisType, idx)
    }

    fun stack(other: MetaContext): MetaContext {
        val other = other.stripKeys(UnknownKey.Lifetime.BLOCK)
        val stacked = other.mapVars { other ->
            var newVars = vars
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
            flowExpr.replaceTypeInTree<VarsExpr> {
                if (it.vars != null) {
                    it
                } else {
                    resolveKnownVariables(stacked.flowExpr)
                }
            },
            stacked.vars,
            thisType,
            idx + other.idx
        )

        return afterStack
    }

    fun forcedDynamic(): MetaContext {
        return MetaContext(
            VarsExpr(),
            keys.associateWith { key ->
                flowExpr.replaceTypeInLeaves<VarsExpr>(key.ind) {
                    val context = (it.vars ?: vars)
                    ContextVarsAssistant.getVar(context, key) { key ->
                        KeyBackreference(key, idx)
                    }
                }
            },
            thisType,
            idx
        )
    }

    fun haveHitReturn(): MetaContext {
        return MetaContext(
            flowExpr.replaceTypeInTree<VarsExpr> {
                if (it.vars != null) it else VarsExpr(vars)
            },
            mapOf(),
            thisType,
            idx
        )
    }
}