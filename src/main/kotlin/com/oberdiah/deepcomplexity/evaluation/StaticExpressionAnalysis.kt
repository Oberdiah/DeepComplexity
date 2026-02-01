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

    fun <A : Any> attemptToSimplifyIfExpr(
        trueBranchExpr: Expr<A>,
        falseBranchExpr: Expr<A>,
        condition: Expr<Boolean>
    ): Expr<A> {
        if (SKIP_OPTIMIZATIONS) {
            return IfExpr.newRaw(trueBranchExpr, falseBranchExpr, condition)
        }

        if (condition is BooleanInvertExpr) {
            // The condition is just inverted, so we can simplify that.
            return attemptToSimplifyIfExpr(falseBranchExpr, trueBranchExpr, condition.expr)
        }

        val invertedCondition = BooleanInvertExpr.new(condition)

        val simplifiedTrueExpr = when (trueBranchExpr) {
            is IfExpr if trueBranchExpr.thisCondition == condition -> trueBranchExpr.trueExpr
            is IfExpr if trueBranchExpr.thisCondition == invertedCondition -> trueBranchExpr.falseExpr
            else -> trueBranchExpr
        }

        val simplifiedFalseExpr = when (falseBranchExpr) {
            is IfExpr if falseBranchExpr.thisCondition == condition -> falseBranchExpr.falseExpr
            is IfExpr if falseBranchExpr.thisCondition == invertedCondition -> falseBranchExpr.trueExpr
            else -> falseBranchExpr
        }

        if (simplifiedTrueExpr == simplifiedFalseExpr) {
            return simplifiedTrueExpr
        }
        if (condition == ConstExpr.TRUE) {
            return simplifiedTrueExpr
        }
        if (condition == ConstExpr.FALSE) {
            return simplifiedFalseExpr
        }

        // Merge nested ifs.
        if (simplifiedTrueExpr is IfExpr) {
            // if c then (if t then x else y) else y ==> if (c AND t) then x else y
            if (simplifiedFalseExpr == simplifiedTrueExpr.falseExpr) {
                return IfExpr.newRaw(
                    simplifiedTrueExpr.trueExpr,
                    simplifiedFalseExpr,
                    BooleanExpr.new(condition, simplifiedTrueExpr.thisCondition, BooleanOp.AND)
                )
            }
            // if c then (if t then x else y) else x ==> if (c AND !t) then y else x
            if (simplifiedFalseExpr == simplifiedTrueExpr.trueExpr) {
                return IfExpr.newRaw(
                    simplifiedTrueExpr.falseExpr,
                    simplifiedFalseExpr,
                    BooleanExpr.new(condition, BooleanInvertExpr.new(simplifiedTrueExpr.thisCondition), BooleanOp.AND)
                )
            }
        }

        if (simplifiedFalseExpr is IfExpr) {
            // if c then x else (if t then x else y) ==> if (c OR t) then x else y
            if (simplifiedTrueExpr == simplifiedFalseExpr.trueExpr) {
                return IfExpr.newRaw(
                    simplifiedTrueExpr,
                    simplifiedFalseExpr.falseExpr,
                    BooleanExpr.new(condition, simplifiedFalseExpr.thisCondition, BooleanOp.OR)
                )
            }
            // if c then y else (if t then x else y) ==> if (c OR !t) then y else x
            if (simplifiedTrueExpr == simplifiedFalseExpr.falseExpr) {
                return IfExpr.newRaw(
                    simplifiedTrueExpr,
                    simplifiedFalseExpr.trueExpr,
                    BooleanExpr.new(condition, BooleanInvertExpr.new(simplifiedFalseExpr.thisCondition), BooleanOp.OR)
                )
            }
        }

        // if c then (if t then x else y) else (if t then x else z) ==> if t then x else (if c then y else z)
        if (simplifiedTrueExpr is IfExpr && simplifiedFalseExpr is IfExpr) {
            if (simplifiedTrueExpr.thisCondition == simplifiedFalseExpr.thisCondition &&
                simplifiedTrueExpr.trueExpr == simplifiedFalseExpr.trueExpr
            ) {
                return IfExpr.newRaw(
                    simplifiedTrueExpr.trueExpr,
                    IfExpr.newRaw(simplifiedTrueExpr.falseExpr, simplifiedFalseExpr.falseExpr, condition),
                    simplifiedTrueExpr.thisCondition
                )
            }
        }

        return IfExpr.newRaw(simplifiedTrueExpr, simplifiedFalseExpr, condition)
    }
}