package com.github.oberdiah.deepcomplexity.loopEvaluation

import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression
import com.github.oberdiah.deepcomplexity.evaluation.ExprRetBool
import com.github.oberdiah.deepcomplexity.evaluation.GaveUpExpression
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context

object LoopEvaluation {
    /**
     * Given the context for the loop body, and the condition, figure out our new context.
     */
    fun processLoopContext(context: Context, condition: ExprRetBool) {
        val numTimes = TODO()

        val allElements = context.getVariables().keys
        for ((key, expr) in context.getVariables()) {
            // Unresolved expressions not in this context are of no interest to us — they can't affect this loop.
            val allUnresolved = expr.getCurrentlyUnresolved()
                .filter { it.getKey().element in allElements }

            if (allUnresolved.isEmpty()) continue

            if (allUnresolved.size == 1) {
                val unresolved = allUnresolved.first()
                if (unresolved.getKey().element == key) {
                    if (expr is ArithmeticExpression) {
                        val wrapped = ArithmeticExpression.repeatExpression(expr, numTimes)

                        if (wrapped != null) {
                            context.assignVar(key, wrapped)
                        } else {
                            // D:
                            context.assignVar(key, GaveUpExpression(expr))
                        }
                    } else {
                        // :(
                        context.assignVar(key, GaveUpExpression(expr))
                    }
                } else {
                    // We might be able to deal with this with a bit more work, but
                    // I'm not going to bother for now.
                    context.assignVar(key, GaveUpExpression(expr))
                }
            } else {
                // We can't deal with this in general.
                // Some edge cases might be doable in certain situations, for now I'm not going to bother.
                context.assignVar(key, GaveUpExpression(expr))
            }
        }
    }
}