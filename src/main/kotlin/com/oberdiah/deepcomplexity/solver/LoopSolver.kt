package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.Context
import com.oberdiah.deepcomplexity.context.UnknownKey
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.staticAnalysis.IntIndicator

object LoopSolver {
    private data class SolverInformation(
        val key: UnknownKey,
        val allKeysInLoop: Set<UnknownKey>,
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
            val myKeys = it.keys

            it.mapExpressions(ExprTreeRebuilder.ExprReplacerWithKey { key, expr ->
                val solverInformation = SolverInformation(
                    key,
                    myKeys,
                    ConstExpr.new(1, IntIndicator)
                )
                loopExpression(expr, solverInformation)
            })
        }

        return newContext
    }

    /**
     * Let's try to define this recursively. The output of this expression is the same expression again,
     * but looped.
     */
    private fun loopExpression(expr: Expr<*>, solverInformation: SolverInformation): Expr<*> {
        when (expr) {
            is ArithmeticExpr<*> -> {
                when (expr.op) {
                    BinaryNumberOp.ADDITION -> TODO()
                    BinaryNumberOp.SUBTRACTION -> TODO()
                    BinaryNumberOp.MULTIPLICATION -> TODO()
                    BinaryNumberOp.DIVISION -> TODO()
                    BinaryNumberOp.MODULO -> TODO()
                }
            }

            is BooleanExpr -> TODO()
            is BooleanInvertExpr -> TODO()
            is ComparisonExpr<*> -> TODO()
            is IfExpr<*> -> TODO()
            is ConstExpr<*> -> TODO()
            is VariableExpr<*> -> TODO()
            is NegateExpr<*> -> TODO()
            is TypeCastExpr<*, *> -> TODO()
            is UnionExpr<*> -> TODO()
            is VarsExpr -> TODO()
        }
    }
}