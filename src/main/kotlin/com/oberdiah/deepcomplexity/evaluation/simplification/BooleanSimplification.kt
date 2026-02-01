package com.oberdiah.deepcomplexity.evaluation.simplification

import com.oberdiah.deepcomplexity.evaluation.*

object BooleanSimplification {
    fun attemptToSimplifyBooleanExpr(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
        if (SKIP_OPTIMIZATIONS) {
            return BooleanExpr.newRaw(lhs, rhs, op)
        }

        if (lhs == rhs) {
            // Note: This is OK even though boolean expressions normally short-circuit.
            // That's because the expression is truly a boolean expression by this point; it
            // can't have side effects.
            return lhs
        }

        return when (op) {
            BooleanOp.AND -> {
                if (rhs == ConstExpr.FALSE || lhs == ConstExpr.FALSE) {
                    ConstExpr.FALSE
                } else if (rhs == ConstExpr.TRUE) {
                    lhs
                } else if (lhs == ConstExpr.TRUE) {
                    rhs
                } else {
                    BooleanExpr.newRaw(lhs, rhs, op)
                }
            }

            BooleanOp.OR -> {
                if (rhs == ConstExpr.TRUE || lhs == ConstExpr.TRUE) {
                    ConstExpr.TRUE
                } else if (rhs == ConstExpr.FALSE) {
                    lhs
                } else if (lhs == ConstExpr.FALSE) {
                    rhs
                } else {
                    BooleanExpr.newRaw(lhs, rhs, op)
                }
            }
        }
    }
}