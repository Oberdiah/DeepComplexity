package com.oberdiah.deepcomplexity.evaluation.simplification

import com.oberdiah.deepcomplexity.evaluation.*

object ComparisonSimplification {

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
}