package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR

object ExprToString {
    fun <T : Any> toString(expr: IExpr<T>): String {
        return when (expr) {
            is ArithmeticExpression -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is ComparisonExpression<*> -> "(${expr.lhs} ${expr.comp} ${expr.rhs})"
            is ConstExpr<*> -> expr.constSet.toString()
            is IfExpression -> {
                return "if ${expr.thisCondition} {\n${
                    expr.trueExpr.toString().prependIndent()
                }\n} else {\n${
                    expr.falseExpr.toString().prependIndent()
                }\n}"
            }

            is IntersectExpression -> "(${expr.lhs} ∩ ${expr.rhs})"
            is BooleanInvertExpression -> "!${expr.expr}"
            is InvertExpression -> "!${expr.expr}"
            is NegateExpression -> "-${expr.expr}"
            is UnionExpression -> "(${expr.lhs} ∪ ${expr.rhs})"
            is BooleanExpression -> booleanExprToString(expr)
            is VariableExpression -> {
                return if (expr.isResolved()) expr.resolvedInto.toString() else expr.myKey.key.toString()
            }

            is NumberLimitsExpression -> "(${expr.cmp}${expr.limit})"
            is NumIterationTimesExpression -> "(initial: ${expr.variable}, update: ${expr.terms} condition: ${expr.constraint})"
            is TypeCastExpression<*, *> -> {
                return if (expr.explicit) {
                    "(${expr.setInd}) ${expr.expr}"
                } else {
                    "${expr.expr}"
                }
            }
        }
    }

    private fun booleanExprToString(expr: BooleanExpression): String {
        if (expr.lhs == ConstantExpression.TRUE) {
            return when (expr.op) {
                AND -> expr.rhs.toString()
                OR -> "TRUE"
            }
        } else if (expr.lhs == ConstantExpression.FALSE) {
            return when (expr.op) {
                AND -> "FALSE"
                OR -> expr.rhs.toString()
            }
        } else if (expr.rhs == ConstantExpression.TRUE) {
            return when (expr.op) {
                AND -> expr.lhs.toString()
                OR -> "TRUE"
            }
        } else if (expr.rhs == ConstantExpression.FALSE) {
            return when (expr.op) {
                AND -> "FALSE"
                OR -> expr.lhs.toString()
            }
        }

        return "(${expr.lhs} ${expr.op} ${expr.rhs})"
    }

    fun <T : Any> toDebugString(expr: IExpr<T>): String {
        val myResult = "<| ${expr.evaluate(ConstantExpression.TRUE).toDebugString()} |>"
        return when (expr) {
            is ArithmeticExpression -> "(${expr.lhs.dStr()} ${expr.op} ${expr.rhs.dStr()}) = $myResult"
            is ComparisonExpression<*> -> "(${expr.lhs.dStr()} ${expr.comp} ${expr.rhs.dStr()}) = $myResult"
            is ConstExpr<*> -> expr.constSet.toString()
            is IfExpression -> {
                return "(if ${expr.thisCondition} {\n${
                    expr.trueExpr.dStr().prependIndent()
                }\n} else {\n${
                    expr.falseExpr.dStr().prependIndent()
                }\n}) = $myResult"
            }

            is IntersectExpression -> "(${expr.lhs.dStr()} ∩ ${expr.rhs.dStr()}) = $myResult"
            is BooleanInvertExpression -> "!${expr.expr.dStr()} = $myResult"
            is InvertExpression -> "!${expr.expr.dStr()} = $myResult"
            is NegateExpression -> "-${expr.expr.dStr()} = $myResult"
            is UnionExpression -> "(${expr.lhs.dStr()} ∪ ${expr.rhs.dStr()}) = $myResult"
            is BooleanExpression -> booleanExprToString(expr)
            is VariableExpression -> {
                return if (expr.isResolved()) expr.resolvedInto?.dStr() ?: "null" else expr.myKey.key.toString()
            }

            is NumberLimitsExpression -> "(${expr.cmp}${expr.limit.dStr()}) = $myResult"
            is NumIterationTimesExpression -> "(initial: ${expr.variable.dStr()}, update: ${expr.terms} condition: ${expr.constraint}) = $myResult"
            is TypeCastExpression<*, *> -> {
                return if (expr.explicit) {
                    "(${expr.setInd}) ${expr.expr.dStr()}"
                } else {
                    expr.expr.dStr()
                }
            }
        }
    }
}