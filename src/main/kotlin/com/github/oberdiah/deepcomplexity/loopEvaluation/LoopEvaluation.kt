package com.github.oberdiah.deepcomplexity.loopEvaluation

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context

object LoopEvaluation {
    /**
     * Given the context for the loop body, and the condition, figure out our new context.
     */
    fun processLoopContext(context: Context, condition: IExprRetBool) {
        val allElements = context.getVariables().keys
        for ((key, expr) in context.getVariables()) {
            // Unresolved expressions not in this context are of no interest to us — they can't affect this loop.
            val allUnresolved = expr.getVariables(false)
                .filter { it.getKey().element in allElements }

            if (allUnresolved.isEmpty()) continue

            if (allUnresolved.size == 1) {
                val unresolved = allUnresolved.first()
                if (unresolved.getKey().element == key) {
                    if (expr is ArithmeticExpression) {
                        val repeated = repeatArithmeticExpression(expr, condition)

                        if (repeated != null) {
                            context.assignVar(key, repeated)
                        } else {
                            // D:
                            context.assignVar(key, GaveUpExpression.fromExpr(expr))
                        }
                    } else {
                        // :(
                        context.assignVar(key, GaveUpExpression.fromExpr(expr))
                    }
                } else {
                    // We might be able to deal with this with a bit more work, but
                    // I'm not going to bother for now.
                    context.assignVar(key, GaveUpExpression.fromExpr(expr))
                }
            } else {
                // We can't deal with this in general.
                // Some edge cases might be doable in certain situations, for now I'm not going to bother.
                context.assignVar(key, GaveUpExpression.fromExpr(expr))
            }
        }
    }

    private fun repeatArithmeticExpression(expr: ArithmeticExpression, condition: IExprRetBool): ArithmeticExpression? {
        // Note: Currently x = (x * 2) * 2 is not handled with this but it could be with some
        // re-arranging/preparation in the pipeline somewhere

        val lhsIsUnresolved = expr.lhs is VariableExpression
        val rhsIsUnresolved = expr.rhs is VariableExpression

        if (!lhsIsUnresolved && !rhsIsUnresolved) {
            // We can only deal with surface-level unresolved expressions
            return null
        }
        if (lhsIsUnresolved && rhsIsUnresolved) {
            // This is the caller's fault
            throw IllegalArgumentException("Both sides of the expression are unresolved, which shouldn't happen.")
        }

        val constantSide = if (lhsIsUnresolved) expr.rhs else expr.lhs
        val unresolvedSide = if (lhsIsUnresolved) expr.lhs else expr.rhs

        if ((expr.op == SUBTRACTION || expr.op == DIVISION) && rhsIsUnresolved) {
            // We can't deal with this case e.g. x = (5 - x) has no obvious solution
            return null
        }

        return when (expr.op) {
            ADDITION, SUBTRACTION -> {
                ArithmeticExpression(
                    unresolvedSide,
                    ArithmeticExpression(
                        constantSide,
                        TODO(),
                        MULTIPLICATION
                    ),
                    expr.op
                )
            }

            else -> TODO("These could be implemented with some sort of pow(), but I've not bothered for now.")
        }
    }
}