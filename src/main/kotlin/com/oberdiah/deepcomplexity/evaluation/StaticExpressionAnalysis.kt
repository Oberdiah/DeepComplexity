package com.oberdiah.deepcomplexity.evaluation

object StaticExpressionAnalysis {
    const val SKIP_OPTIMIZATIONS = false

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

    /**
     * All of this should be extremely cheap. It's designed to be a quick pre-check to prevent
     * trivial comparisons from going to the full evaluation engine.
     */
    fun <T : Any> attemptToSimplifyComparison(lhs: Expr<T>, rhs: Expr<T>, comp: ComparisonOp): Expr<Boolean> {
        // To disable optimizations for testing purposes:
        if (SKIP_OPTIMIZATIONS) {
            return ComparisonExpr.newRaw(lhs, rhs, comp)
        }

        val optimised = when (comp) {
            ComparisonOp.LESS_THAN,
            ComparisonOp.GREATER_THAN -> {
                if (lhs == rhs) {
                    return ConstExpr.FALSE
                }
                null
            }

            ComparisonOp.LESS_THAN_OR_EQUAL,
            ComparisonOp.GREATER_THAN_OR_EQUAL -> {
                if (lhs == rhs) {
                    return ConstExpr.TRUE
                }
                null
            }

            ComparisonOp.EQUAL -> {
                if (rhs == lhs) {
                    return ConstExpr.TRUE
                }
                if (guaranteedNotEqual(rhs, lhs)) {
                    return ConstExpr.FALSE
                }
                null
            }

            ComparisonOp.NOT_EQUAL -> {
                if (rhs == lhs) {
                    return ConstExpr.FALSE
                }
                if (guaranteedNotEqual(rhs, lhs)) {
                    return ConstExpr.TRUE
                }
                null
            }
        }

        return optimised ?: ComparisonExpr.newRaw(lhs, rhs, comp)
    }

    private fun guaranteedNotEqual(lhs: Expr<*>, rhs: Expr<*>): Boolean {
        // Note, this cannot just be lhs != rhs, because Var(x) and Var(y) may actually be the same value,
        // despite evaluating to different expressions.
        if (lhs is ConstExpr<*> && rhs is ConstExpr<*>) {
            return lhs != rhs
        }
        return false
    }

    fun <A : Any> attemptToSimplifyIfExpr(trueExpr: Expr<A>, falseExpr: Expr<A>, condition: Expr<Boolean>): Expr<A> {
        if (SKIP_OPTIMIZATIONS) {
            return IfExpr.newRaw(trueExpr, falseExpr, condition)
        }

        val trueExpr = if (trueExpr is IfExpr && trueExpr.thisCondition == condition) {
            trueExpr.trueExpr
        } else {
            trueExpr
        }
        val falseExpr = if (falseExpr is IfExpr && falseExpr.thisCondition == condition) {
            // If my condition is equal to the condition of the false branch's inner `if`'s condition, we know that that
            // inner `if` can never be true, so we can safely return its false branch.
            falseExpr.falseExpr
        } else {
            falseExpr
        }

        if (trueExpr == falseExpr) {
            return trueExpr
        }

        if (trueExpr is IfExpr) {
            // If we've got two nested ifs with the same false branch, we can merge them into a single if.
            if (falseExpr == trueExpr.falseExpr) {
                return IfExpr.newRaw(
                    trueExpr.trueExpr,
                    falseExpr,
                    BooleanExpr.new(condition, trueExpr.thisCondition, BooleanOp.AND)
                )
            }
        }

        if (condition == ConstExpr.TRUE) {
            return trueExpr
        } else if (condition == ConstExpr.FALSE) {
            return falseExpr
        }

        return IfExpr.newRaw(trueExpr, falseExpr, condition)
    }
}