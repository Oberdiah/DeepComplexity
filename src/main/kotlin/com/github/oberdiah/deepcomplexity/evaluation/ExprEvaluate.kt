package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ExprEvaluate {
    fun evaluate(expr: IExpr, condition: IExprRetBool): IMoldableSet {
        return when (expr) {
            is IExprRetNum -> evaluate(expr, condition)
            is IExprRetBool -> evaluate(expr, condition)
            is IExprRetGeneric -> evaluate(expr, condition)

            is IntersectExpression -> evaluate(expr.lhs, condition).intersect(evaluate(expr.rhs, condition))
            is UnionExpression -> evaluate(expr.lhs, condition).union(evaluate(expr.rhs, condition))

            is IfExpression -> {
                val ifCondition = expr.thisCondition
                val evaluatedCond = evaluate(ifCondition, condition)
                val trueCondition = BooleanExpression(ifCondition, condition, BooleanOp.AND)
                val falseCondition = BooleanExpression(InvertExpression(ifCondition), condition, BooleanOp.AND)
                return when (evaluatedCond) {
                    TRUE -> evaluate(expr.trueExpr, trueCondition)
                    FALSE -> evaluate(expr.falseExpr, falseCondition)
                    BOTH -> {
                        val trueValue = evaluate(expr.trueExpr, trueCondition)
                        val falseValue = evaluate(expr.falseExpr, falseCondition)
                        trueValue.union(falseValue)
                    }

                    NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
                }
            }

            is RepeatExpression -> TODO("Not yet implemented")
        }
    }

    fun evaluate(expr: IExprRetNum, condition: IExprRetBool): NumberSet {
        return when (expr) {
            is ArithmeticExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                return lhs.arithmeticOperation(rhs, expr.op)
            }

            is NegateExpression -> evaluate(expr.expr, condition).negate()
            is ConstExprNum -> expr.singleElementSet
            is VariableExpression.VariableNumber -> {
                expr.resolvedInto?.let {
                    return evaluate(it, condition)
                }

                // If we're here we're at the end of the chain, assume a full range.
                val range = NumberSet.fullRange(expr.clazz)
                val constrainedRange = ExprConstrain.getConstraints(condition, expr)

                return if (constrainedRange != null) {
                    range.intersect(constrainedRange) as NumberSet
                } else {
                    range
                }
            }

            is NumberLimitsExpression -> {
                val rhs = expr.limit.evaluate(condition)
                when (expr.shouldFlipCmp.evaluate(condition)) {
                    TRUE -> rhs.getSetSatisfying(expr.cmp)
                    FALSE -> rhs.getSetSatisfying(expr.cmp.flip())
                    BOTH -> rhs.getSetSatisfying(expr.cmp)
                        .union(rhs.getSetSatisfying(expr.cmp.flip())) as NumberSet

                    NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                }
            }
        }
    }

    fun evaluate(expr: IExprRetBool, condition: IExprRetBool): BooleanSet {
        return when (expr) {
            is BooleanExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                return lhs.booleanOperation(rhs, expr.op)
            }

            is ComparisonExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                return lhs.comparisonOperation(rhs, expr.comp)
            }

            is ConstExprBool -> expr.singleElementSet
            is InvertExpression -> evaluate(expr, condition).invert() as BooleanSet
            is VariableExpression.VariableBool -> {
                expr.resolvedInto?.let {
                    return evaluate(it, condition)
                }
                TODO("Not implemented constraints on boolean variables yet")
            }
        }
    }

    fun evaluate(expr: IExprRetGeneric, condition: IExprRetBool): GenericSet {
        return when (expr) {
            is ConstExprGeneric -> expr.singleElementSet
            is VariableExpression.VariableGeneric -> {
                expr.resolvedInto?.let {
                    return evaluate(it, condition)
                }
                TODO("Not implemented constraints on generic variables yet")
            }
        }
    }
}