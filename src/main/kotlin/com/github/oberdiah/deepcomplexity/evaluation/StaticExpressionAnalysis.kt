package com.github.oberdiah.deepcomplexity.evaluation

object StaticExpressionAnalysis {
    fun attemptToSimplifyBooleanExpr(rhs: Expr<Boolean>, lhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean>? {
        return when (op) {
            BooleanOp.AND -> {
                if (lhs == ConstExpr.FALSE || rhs == ConstExpr.FALSE) {
                    ConstExpr.FALSE
                } else if (lhs == ConstExpr.TRUE) {
                    rhs
                } else if (rhs == ConstExpr.TRUE) {
                    lhs
                } else {
                    null
                }
            }

            BooleanOp.OR -> {
                if (lhs == ConstExpr.TRUE || rhs == ConstExpr.TRUE) {
                    ConstExpr.TRUE
                } else if (lhs == ConstExpr.FALSE) {
                    rhs
                } else if (rhs == ConstExpr.FALSE) {
                    lhs
                } else {
                    null
                }
            }
        }
    }

    /**
     * All of this should be extremely cheap. It's designed to be a quick pre-check to prevent
     * trivial comparisons from going to the full evaluation engine.
     */
    fun <T : Any> attemptToSimplifyComparison(rhs: Expr<T>, lhs: Expr<T>, comp: ComparisonOp): Expr<Boolean>? {
        // To disable optimisations for testing purposes:
//        if (true) {
//            return null
//        }

        return when (comp) {
            ComparisonOp.LESS_THAN -> null
            ComparisonOp.LESS_THAN_OR_EQUAL -> null
            ComparisonOp.GREATER_THAN -> null
            ComparisonOp.GREATER_THAN_OR_EQUAL -> null
            ComparisonOp.EQUAL -> {
                if (guaranteedEqual(lhs, rhs)) {
                    return ConstExpr.TRUE
                }
                if (guaranteedNotEqual(lhs, rhs)) {
                    return ConstExpr.FALSE
                }
                null
            }

            ComparisonOp.NOT_EQUAL -> {
                if (guaranteedEqual(lhs, rhs)) {
                    return ConstExpr.FALSE
                }
                if (guaranteedNotEqual(lhs, rhs)) {
                    return ConstExpr.TRUE
                }
                null
            }
        }
    }

    private fun guaranteedNotEqual(lhs: Expr<*>, rhs: Expr<*>): Boolean {
        if (lhs is ConstExpr<*> && rhs is ConstExpr<*>) {
            return lhs.value != rhs.value
        }
        return false
    }

    private fun guaranteedEqual(lhs: Expr<*>, rhs: Expr<*>): Boolean {
        // This could do a full lhs == rhs equality check, but I
        // felt it was unnecessary. If you ever encounter a situation
        // where it might simplify stuff, feel free to add it.
        if (lhs is ConstExpr<*> && rhs is ConstExpr<*>) {
            return lhs.value == rhs.value
        }
        if (lhs is VariableExpr<*> && rhs is VariableExpr<*>) {
            return lhs.key == rhs.key
        }
        if (lhs is TypeCastExpr<*, *> && rhs is TypeCastExpr<*, *>) {
            if (lhs.ind == rhs.ind && lhs.explicit == rhs.explicit) {
                return guaranteedEqual(lhs.expr, rhs.expr)
            }
        }
        return false
    }
}