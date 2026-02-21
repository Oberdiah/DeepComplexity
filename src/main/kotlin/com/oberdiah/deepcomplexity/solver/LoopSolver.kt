package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.Context
import com.oberdiah.deepcomplexity.context.MethodProcessingKey
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import org.jetbrains.kotlin.utils.keysToMap

object LoopSolver {
    private data class SolverInformation(
        val key: MethodProcessingKey,
        val allKeysInLoop: Set<MethodProcessingKey>,
        val numLoopsExpr: Expr<Int>
    )

    /**
     * Given the context for the loop body, and the condition, figure out our new context.
     */
    fun processLoopContext(loopContext: Context, condition: Expr<Boolean>): Context {
        // We'll ignore the control flow (returns and breaks) for now.
        val newContext = loopContext.forcedDynamic().mapVars { vars ->
            val loopVariables = vars.keys.keysToMap { k ->
                ConversionsAndPromotion.castAToB(
                    VariableExpr.new(k),
                    convertExprToLoopableExpr(
                        vars.get(LValueKey.new(k)),
                        vars.keys
                    ),
                    Behaviour.Throw
                ).map { initial, update -> LoopExpr.LoopVar(initial, update) }
            }.mapKeys { (k, _) -> LoopExpr.LoopKey.new(k) }

            val loopCondition = convertExprToLoopableExpr(condition, vars.keys)

            vars.mapExpressions(ExprTreeRebuilder.ExprReplacerWithKey { key, _ ->
                LoopExpr.new(
                    LoopExpr.LoopKey.new(key),
                    loopCondition,
                    loopVariables,
                )
            })
        }

        return newContext
    }

    private fun <T : Any> convertExprToLoopableExpr(expr: Expr<T>, keysToReplace: Set<MethodProcessingKey>): Expr<T> {
        return expr.rewriteTypeInTreeSameType<VariableExpr<*>> {
            if (keysToReplace.contains(it.key)) {
                LoopExpr.LoopLeaf.new(it.key)
            } else {
                it
            }
        }
    }
}