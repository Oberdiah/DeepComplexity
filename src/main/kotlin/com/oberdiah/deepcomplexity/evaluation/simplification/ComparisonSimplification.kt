package com.oberdiah.deepcomplexity.evaluation.simplification

import com.oberdiah.deepcomplexity.evaluation.*

object ComparisonSimplification {
    private sealed interface Result
    private data class SimplerComparison(val lhs: Expr<*>, val rhs: Expr<*>, val comp: ComparisonOp) : Result {
        val size get() = (lhs.recursiveSubExprs + rhs.recursiveSubExprs).size + 1
    }

    private data class NoLongerAComparison(val expr: Expr<Boolean>) : Result

    /**
     * Turns
     * ```
     * (if c then a else b) OP (if c then d else e)
     * ```
     * into
     * ```
     * if c then (a OP d) else (b OP e)
     * ```
     */
    private fun ifsWithMatchingCondition(cmp: SimplerComparison): Result {
        val lhs = cmp.lhs
        val rhs = cmp.rhs
        if (lhs !is IfExpr || rhs !is IfExpr) return cmp
        if (lhs.thisCondition != rhs.thisCondition) return cmp

        val trueBranch = ComparisonExpr.new(lhs.trueExpr, rhs.trueExpr, cmp.comp)
        val falseBranch = ComparisonExpr.new(lhs.falseExpr, rhs.falseExpr, cmp.comp)
        return NoLongerAComparison(IfExpr.new(trueBranch, falseBranch, lhs.thisCondition))
    }

    /**
     * Turns
     * ```
     * x OP x
     * ```
     * into
     * ```
     * TRUE / FALSE
     * ```
     * depending on OP.
     */
    private fun selfComparison(cmp: SimplerComparison): Result {
        val lhs = cmp.lhs
        val rhs = cmp.rhs
        if (lhs != rhs) return cmp

        return when (cmp.comp) {
            ComparisonOp.LESS_THAN,
            ComparisonOp.GREATER_THAN -> NoLongerAComparison(ConstExpr.FALSE)

            ComparisonOp.LESS_THAN_OR_EQUAL,
            ComparisonOp.GREATER_THAN_OR_EQUAL -> NoLongerAComparison(ConstExpr.TRUE)

            ComparisonOp.EQUAL -> NoLongerAComparison(ConstExpr.TRUE)
            ComparisonOp.NOT_EQUAL -> NoLongerAComparison(ConstExpr.FALSE)
        }
    }


    /**
     * Turns
     * ```
     * 1 == 2
     * ```
     * into
     * ```
     * FALSE
     * ```
     */
    private fun equalityAndInequality(cmp: SimplerComparison): Result {
        return when (cmp.comp) {
            ComparisonOp.EQUAL ->
                if (guaranteedNotEqual(cmp.lhs, cmp.rhs)) NoLongerAComparison(ConstExpr.FALSE) else cmp

            ComparisonOp.NOT_EQUAL ->
                if (guaranteedNotEqual(cmp.lhs, cmp.rhs)) NoLongerAComparison(ConstExpr.TRUE) else cmp

            else -> cmp
        }
    }

    private val OPTIMIZATIONS = listOf<(SimplerComparison) -> Result>(
        ::ifsWithMatchingCondition,
        ::selfComparison,
        ::equalityAndInequality
    )

    /**
     * All of this should be extremely cheap. It's designed to be a quick pre-check to prevent
     * trivial comparisons from going to the full evaluation engine.
     */
    fun <T : Any> attemptToSimplifyComparison(lhs: Expr<T>, rhs: Expr<T>, comp: ComparisonOp): Expr<Boolean> {
        // To disable optimizations for testing purposes:
        if (SKIP_OPTIMIZATIONS) {
            return ComparisonExpr.newRaw(lhs, rhs, comp)
        }

        // SUPER IMPORTANT NOTE:
        // These simplifications should probably not output a larger expression than they consumed. (where larger
        // means the number of unique nodes in the expression tree)
        // This is a dangerous thing to do because it's a soft expectation of the program that
        // the number of unique nodes in an expression tree is at least kinda linear to the size of the input code.
        // In practice at the moment we're not requiring that, but we may one day.

        val indicator = lhs.ind
        require(indicator == rhs.ind)

        var current = SimplerComparison(lhs, rhs, comp)
        optimizationLoop@ while (true) {
            for (optimisation in OPTIMIZATIONS) {
                when (val result = optimisation(current)) {
                    is SimplerComparison -> if (result != current) {
//                            require(result.size <= current.size) {
//                                "If simplification produced a larger expression tree!"
//                            }
                        current = result
                        continue@optimizationLoop
                    }

                    is NoLongerAComparison -> return result.expr
                }
            }
            break
        }

        return ComparisonExpr.newRaw(
            current.lhs.castOrThrow(indicator),
            current.rhs.castOrThrow(indicator),
            current.comp
        )
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
