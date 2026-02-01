package com.oberdiah.deepcomplexity.evaluation.simplification

import com.oberdiah.deepcomplexity.evaluation.*

object IfSimplification {
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
                return IfExpr.new(
                    simplifiedTrueExpr.trueExpr,
                    simplifiedFalseExpr,
                    BooleanExpr.new(condition, simplifiedTrueExpr.thisCondition, BooleanOp.AND)
                )
            }
            // if c then (if t then x else y) else x ==> if (c AND !t) then y else x
            if (simplifiedFalseExpr == simplifiedTrueExpr.trueExpr) {
                return IfExpr.new(
                    simplifiedTrueExpr.falseExpr,
                    simplifiedFalseExpr,
                    BooleanExpr.new(condition, BooleanInvertExpr.new(simplifiedTrueExpr.thisCondition), BooleanOp.AND)
                )
            }
        }

        if (simplifiedFalseExpr is IfExpr) {
            // if c then x else (if t then x else y) ==> if (c OR t) then x else y
            if (simplifiedTrueExpr == simplifiedFalseExpr.trueExpr) {
                return IfExpr.new(
                    simplifiedTrueExpr,
                    simplifiedFalseExpr.falseExpr,
                    BooleanExpr.new(condition, simplifiedFalseExpr.thisCondition, BooleanOp.OR)
                )
            }
            // if c then y else (if t then x else y) ==> if (c OR !t) then y else x
            if (simplifiedTrueExpr == simplifiedFalseExpr.falseExpr) {
                return IfExpr.new(
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
                return IfExpr.new(
                    simplifiedTrueExpr.trueExpr,
                    IfExpr.new(simplifiedTrueExpr.falseExpr, simplifiedFalseExpr.falseExpr, condition),
                    simplifiedTrueExpr.thisCondition
                )
            }
        }

        return IfExpr.newRaw(simplifiedTrueExpr, simplifiedFalseExpr, condition)
    }
}