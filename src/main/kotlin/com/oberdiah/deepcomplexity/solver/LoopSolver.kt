package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.Context
import com.oberdiah.deepcomplexity.context.MethodProcessingKey
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder

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

        // How to do this:
        // 1. Loop over all the variables in the loop's context
        // 2. Check to see whether they refer to each other at all.
        // 3. If they do, we then have to try to pull them apart.
        // 4. We can do lots of stuff for that, inlining etc.

        // Ooo:
        // Look at this:
        // This could be cool
        // We turn
        // i = (i + 1) + 1
        // into
        // i = (i + 1 * n) + 1 * n
        // When cases allow.

        // If we can come up with a list of all such cases, we could be Golden.

        // If i appears more than once, or if there are other variables that are set within the loop,
        // we can ignore for now.

        val newContext = loopContext.forcedDynamic().mapVars {
            val allKeysInLoop = it.keys

            it.mapExpressions(ExprTreeRebuilder.ExprReplacerWithKey { key, expr ->
                expr
            })
        }

        return newContext
    }
}