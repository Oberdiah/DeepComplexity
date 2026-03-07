package com.oberdiah.deepcomplexity.evaluation.simplification

import com.oberdiah.deepcomplexity.evaluation.*


object IfSimplification {
    private sealed interface Result
    private data class SimplerIf(val trueExpr: Expr<*>, val falseExpr: Expr<*>, val cond: Expr<Boolean>) : Result {
        val size get() = (trueExpr.recursiveSubExprs + falseExpr.recursiveSubExprs + cond.recursiveSubExprs).size + 1
    }

    private data class NoLongerAnIf(val expr: Expr<*>) : Result

    /**
     * Turns
     * ```
     * if (!cond) {
     *     trueExpr
     * } else {
     *     falseExpr
     * }
     * ```
     * into
     * ```
     * if (cond) {
     *     falseExpr
     * } else {
     *     trueExpr
     * }
     * ```
     */
    private fun uninvertCond(iff: SimplerIf): SimplerIf {
        return if (iff.cond is BooleanInvertExpr) {
            SimplerIf(iff.falseExpr, iff.trueExpr, iff.cond.expr)
        } else {
            iff
        }
    }

    /**
     * Turns
     * ```
     * if (cond) {
     *     if (cond) {
     *         x
     *     } else {
     *         y
     *     }
     * } else {
     *     z
     * }
     * ```
     * into
     * ```
     * if (cond) {
     *     x
     * } else {
     *     z
     * }
     * ```
     */
    private fun nestedIfWithMatchingCondition(iff: SimplerIf): SimplerIf {
        fun simplify(branch: Expr<*>, takeTrueExpr: Boolean) = when (branch) {
            is IfExpr -> when (val cond = branch.thisCondition) {
                iff.cond -> if (takeTrueExpr) branch.trueExpr else branch.falseExpr
                is BooleanInvertExpr if (cond.expr == iff.cond) ->
                    if (takeTrueExpr) branch.falseExpr else branch.trueExpr

                else -> branch
            }

            else -> branch
        }

        val simplifiedTrueExpr = simplify(iff.trueExpr, takeTrueExpr = true)
        val simplifiedFalseExpr = simplify(iff.falseExpr, takeTrueExpr = false)

        return if (simplifiedTrueExpr == iff.trueExpr && simplifiedFalseExpr == iff.falseExpr) {
            iff
        } else {
            SimplerIf(simplifiedTrueExpr, simplifiedFalseExpr, iff.cond)
        }
    }


    private fun equalBranches(iff: SimplerIf): Result {
        return if (iff.trueExpr == iff.falseExpr) {
            NoLongerAnIf(iff.trueExpr)
        } else {
            iff
        }
    }

    private fun trivialCondition(iff: SimplerIf): Result {
        return when (iff.cond) {
            ConstExpr.TRUE -> NoLongerAnIf(iff.trueExpr)
            ConstExpr.FALSE -> NoLongerAnIf(iff.falseExpr)
            else -> iff
        }
    }

    /**
     * Turns this (and its three other variants):
     * ```
     * if (c) {
     *     if (t) {
     *         x
     *     } else {
     *         y
     *     }
     * } else {
     *     y
     * }
     * ```
     * into (or equivalent)
     * ```
     * if (c && t) {
     *     x
     * } else {
     *     y
     * }
     * ```
     */
    private fun mergeNestedIfs(iff: SimplerIf): SimplerIf {
        val thenExpr = iff.trueExpr
        if (thenExpr is IfExpr) {
            // if c then (if t then x else y) else y ==> if (c AND t) then x else y
            if (iff.falseExpr == thenExpr.falseExpr) {
                return SimplerIf(
                    trueExpr = thenExpr.trueExpr,
                    falseExpr = iff.falseExpr,
                    cond = BooleanOpExpr.new(iff.cond, thenExpr.thisCondition, BooleanOp.AND)
                )
            }
            // if c then (if t then x else y) else x ==> if (c AND !t) then y else x
            if (iff.falseExpr == thenExpr.trueExpr) {
                return SimplerIf(
                    trueExpr = thenExpr.falseExpr,
                    falseExpr = iff.falseExpr,
                    cond = BooleanOpExpr.new(
                        iff.cond,
                        BooleanInvertExpr.new(thenExpr.thisCondition),
                        BooleanOp.AND
                    )
                )
            }
        }

        val elseExpr = iff.falseExpr
        if (elseExpr is IfExpr) {
            // if c then x else (if t then x else y) ==> if (c OR t) then x else y
            if (iff.trueExpr == elseExpr.trueExpr) {
                return SimplerIf(
                    trueExpr = iff.trueExpr,
                    falseExpr = elseExpr.falseExpr,
                    cond = BooleanOpExpr.new(iff.cond, elseExpr.thisCondition, BooleanOp.OR)
                )
            }
            // if c then y else (if t then x else y) ==> if (c OR !t) then y else x
            if (iff.trueExpr == elseExpr.falseExpr) {
                return SimplerIf(
                    trueExpr = iff.trueExpr,
                    falseExpr = elseExpr.trueExpr,
                    cond = BooleanOpExpr.new(
                        iff.cond,
                        BooleanInvertExpr.new(elseExpr.thisCondition),
                        BooleanOp.OR
                    )
                )
            }
        }

        return iff
    }


    /**
     * Turns this (and its other variant):
     *
     * ```
     * if (c) {
     *     if (t) {
     *         x
     *     } else {
     *         y
     *     }
     * } else {
     *     if (t) {
     *         z
     *     } else {
     *         y
     *     }
     * }
     * ```
     * into (or equivalent)
     * ```
     * if (t) {
     *     if (c) {
     *         x
     *     } else {
     *         z
     *     }
     * } else {
     *     y
     * }
     *
     * ```
     */
    private fun factorCommonNestedThen(iff: SimplerIf): SimplerIf {
        val thenExpr = iff.trueExpr
        val elseExpr = iff.falseExpr
        if (thenExpr !is IfExpr || elseExpr !is IfExpr) {
            return iff
        }

        if (thenExpr.thisCondition != elseExpr.thisCondition) {
            return iff
        }

        val innerCond = thenExpr.thisCondition

        val thenInnerTrue = thenExpr.trueExpr
        val thenInnerFalse = thenExpr.falseExpr
        val elseInnerTrue = elseExpr.trueExpr
        val elseInnerFalse = elseExpr.falseExpr

        // inner true branch matches
        if (thenInnerTrue == elseInnerTrue) {
            return SimplerIf(
                trueExpr = thenInnerTrue,
                falseExpr = IfExpr.new(thenInnerFalse, elseInnerFalse, iff.cond),
                cond = innerCond
            )
        }

        // inner false branch matches
        if (thenInnerFalse == elseInnerFalse) {
            return SimplerIf(
                trueExpr = IfExpr.new(thenInnerTrue, elseInnerTrue, iff.cond),
                falseExpr = thenInnerFalse,
                cond = innerCond
            )
        }

        return iff
    }

    private val OPTIMIZATIONS = listOf<(SimplerIf) -> Result>(
        ::uninvertCond,
        ::nestedIfWithMatchingCondition,
        ::equalBranches,
        ::trivialCondition,
        ::mergeNestedIfs,
        ::factorCommonNestedThen
    )

    fun <A : Any> attemptToSimplifyIfExpr(
        trueBranchExpr: Expr<A>,
        falseBranchExpr: Expr<A>,
        condition: Expr<Boolean>
    ): Expr<A> {
        if (SKIP_OPTIMIZATIONS) {
            return IfExpr.newRaw(trueBranchExpr, falseBranchExpr, condition)
        }

        // SUPER IMPORTANT NOTE:
        // These simplifications should probably not output a larger expression than they consumed. (where larger
        // means the number of unique nodes in the expression tree)
        // This is a dangerous thing to do because it's a soft expectation of the program that
        // the number of unique nodes in an expression tree is at least kinda linear to the size of the input code.
        // (in practice I believe the best we'll be able to do is quadratic but at least not exponential)
        // At the moment we're not requiring it as a hard constraint, but we may one day.

        val indicator = trueBranchExpr.ind
        require(indicator == falseBranchExpr.ind)

        var current = SimplerIf(trueBranchExpr, falseBranchExpr, condition)
        optimizationLoop@ while (true) {
            for (optimisation in OPTIMIZATIONS) {
                when (val result = optimisation(current)) {
                    is SimplerIf -> {
                        if (result != current) {
                            current = result
                            continue@optimizationLoop
                        }
                    }

                    is NoLongerAnIf -> {
                        return result.expr.coerceTo(indicator)
                    }
                }
            }
            break
        }

        val final = IfExpr.newRaw(
            current.trueExpr.coerceTo(indicator),
            current.falseExpr.coerceTo(indicator),
            current.cond
        )
        return final
    }
}