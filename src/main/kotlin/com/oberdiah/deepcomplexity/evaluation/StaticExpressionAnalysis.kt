package com.oberdiah.deepcomplexity.evaluation

object StaticExpressionAnalysis {
    const val SKIP_OPTIMISATIONS = false

    fun attemptToSimplifyBooleanExpr(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
        if (SKIP_OPTIMISATIONS) {
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
        // To disable optimisations for testing purposes:
        if (SKIP_OPTIMISATIONS) {
            return ComparisonExpr.newRaw(lhs, rhs, comp)
        }

        val optimised = when (comp) {
            ComparisonOp.LESS_THAN -> null
            ComparisonOp.LESS_THAN_OR_EQUAL -> null
            ComparisonOp.GREATER_THAN -> null
            ComparisonOp.GREATER_THAN_OR_EQUAL -> null
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
        if (SKIP_OPTIMISATIONS) {
            return IfExpr.newRaw(trueExpr, falseExpr, condition)
        }

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

        if (condition == ConstExpr.TRUE) {
            return trueExpr
        } else if (condition == ConstExpr.FALSE) {
            return falseExpr
        }

        return IfExpr.newRaw(trueExpr, falseExpr, condition)
    }
}