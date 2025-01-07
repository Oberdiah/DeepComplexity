package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR

object ExprToString {
    fun toString(expr: IExpr): String {
        return when (expr) {
            is ArithmeticExpression -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is ComparisonExpression -> "(${expr.lhs} ${expr.comp} ${expr.rhs})"
            is ConstExpr<*> -> expr.singleElementSet.toString()
            is IfExpression -> {
                return "if ${expr.thisCondition} {\n${
                    expr.trueExpr.toString().prependIndent()
                }\n} else {\n${
                    expr.falseExpr.toString().prependIndent()
                }\n}"
            }

            is IntersectExpression -> "(${expr.lhs} ∩ ${expr.rhs})"
            is InvertExpression -> "!${expr.expr}"
            is NegateExpression -> "-${expr.expr}"
            is RepeatExpression -> "[repeat ${expr.numRepeats} times] { ${expr.exprToRepeat} }"
            is UnionExpression -> "(${expr.lhs} ∪ ${expr.rhs})"
            is BooleanExpression -> booleanExprToString(expr)
            is VariableExpression.VariableImpl<*> -> {
                if (expr.myKey == null) return "Unresolved (on-the-fly)"
                return if (expr.isResolved()) expr.resolvedInto.toString() else (expr.myKey.element.toString() + "[$${expr.id}]")
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
}