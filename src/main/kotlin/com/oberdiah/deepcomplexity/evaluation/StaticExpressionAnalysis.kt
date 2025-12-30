package com.oberdiah.deepcomplexity.evaluation

object StaticExpressionAnalysis {
    fun attemptToSimplifyBooleanExpr(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
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
        // To disable optimisations for testing purposes:
//        if (true) {
//            return ComparisonExpr.newRaw(lhs, rhs, comp)
//        }

        val optimised = when (comp) {
            ComparisonOp.LESS_THAN -> null
            ComparisonOp.LESS_THAN_OR_EQUAL -> null
            ComparisonOp.GREATER_THAN -> null
            ComparisonOp.GREATER_THAN_OR_EQUAL -> null
            ComparisonOp.EQUAL -> {
                if (guaranteedEqual(rhs, lhs)) {
                    return ConstExpr.TRUE
                }
                if (guaranteedNotEqual(rhs, lhs)) {
                    return ConstExpr.FALSE
                }
                null
            }

            ComparisonOp.NOT_EQUAL -> {
                if (guaranteedEqual(rhs, lhs)) {
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
            return lhs.resolvesTo == rhs.resolvesTo
        }
        if (lhs is TypeCastExpr<*, *> && rhs is TypeCastExpr<*, *>) {
            if (lhs.ind == rhs.ind && lhs.explicit == rhs.explicit) {
                return guaranteedEqual(lhs.expr, rhs.expr)
            }
        }
        return false
    }

    fun <A : Any> attemptToSimplifyIfExpr(trueExpr: Expr<A>, falseExpr: Expr<A>, condition: Expr<Boolean>): Expr<A> {
        /**
         * These simplifications do actually allow our tests to pass at the moment;
         * without their inlining some impossible conditions pollute the expression tree,
         * and the tests don't like that much.
         * For example, the case
         * ```
         * if (x > 5) {
         *     foo = new MyClass(20);
         * }
         * ```
         * leads to `#1.x = (x > 5) ? 20 : #1.x'`.
         * Now, we know for certain that #1 is never going to appear earlier in the expression tree;
         * it was created in the branch itself.
         * We previously had an optimisation where we'd check for assigning to defined objects
         * inside branches and remove those early, but that complicated the code and I'm
         * concerned that further down the line with globals it might lead to problems.
         * So for now, we just rely on the if statement simplifications to hide them from us.
         */

        // EXPR_EQUALITY_PERF_ISSUE
        val trueExpr = if (trueExpr is IfExpr && trueExpr.thisCondition == condition) {
            trueExpr.trueExpr
        } else {
            trueExpr
        }
        // EXPR_EQUALITY_PERF_ISSUE
        val falseExpr = if (falseExpr is IfExpr && falseExpr.thisCondition == condition) {
            // If my condition is equal to the condition of the false branch's inner `if`'s condition, we know that that
            // inner `if` can never be true, so we can safely return its false branch.
            falseExpr.falseExpr
        } else {
            falseExpr
        }

        // This equality is probably not very cheap.
        // I'm sure that can be improved in the future.
        // EXPR_EQUALITY_PERF_ISSUE
        if (trueExpr == falseExpr) {
            return trueExpr
        }

        if (condition == ConstExpr.TRUE) {
            return trueExpr
        } else if (condition == ConstExpr.FALSE) {
            return falseExpr
        }

        return IfExpr.newRaw(trueExpr, falseExpr, condition)
    }
}