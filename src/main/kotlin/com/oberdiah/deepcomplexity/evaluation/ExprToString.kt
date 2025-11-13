package com.oberdiah.deepcomplexity.evaluation

object ExprToString {
    fun <T : Any> toString(expr: Expr<T>): String {
        return when (expr) {
            is ArithmeticExpr -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is ComparisonExpr<*> -> "(${expr.lhs} ${expr.comp} ${expr.rhs})"
            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                return "if ${expr.thisCondition} {\n${
                    expr.trueExpr.toString().prependIndent()
                }\n} else {\n${
                    expr.falseExpr.toString().prependIndent()
                }\n}"
            }

            is BooleanInvertExpr -> "!${expr.expr}"
            is NegateExpr -> "-${expr.expr}"
            is UnionExpr -> "(${expr.lhs} ∪ ${expr.rhs})"
            is BooleanExpr -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is NumIterationTimesExpr -> "(initial: ${expr.variable}, update: ${expr.terms} condition: ${expr.constraint})"
            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${expr.expr}"
                } else {
                    "${expr.expr}"
                }
            }

            is VariableExpr -> expr.key.toString()
            is LValueFieldExpr -> "${expr.qualifier}.${expr.field}"
            is LValueKeyExpr -> "${expr.key}"
            is ContextExpr -> if (expr.ctx != null) expr.ctx.toString() else ContextExpr.STRING_PLACEHOLDER
        }
    }

    fun <T : Any> toExprKeyString(expr: Expr<T>): String {
        return when (expr) {
            is ArithmeticExpr -> "'${expr.op}'"
            is ComparisonExpr<*> -> "'${expr.comp}'"
            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> "'if'"
            is BooleanInvertExpr -> "'!'"
            is NegateExpr -> "'-'"
            is UnionExpr -> "'∪'"
            is BooleanExpr -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is VariableExpr -> expr.key.toString()
            is NumIterationTimesExpr -> "'for'"
            is TypeCastExpr<*, *> -> toExprKeyString(expr.expr)
            is LValueFieldExpr<*> -> throw Exception("LValueFieldExpr should really not be evaluated!")
            is LValueKeyExpr<*> -> throw Exception("LValueKeyExpr should really not be evaluated!")
            is ContextExpr -> throw Exception("ContextExpr should really not be evaluated!")
        }
    }

    fun <T : Any> toDebugString(expr: Expr<T>): String {
        val myResult = "<| ${ExprEvaluate.evaluate(expr, ExprEvaluate.Scope(), true).toDebugString()} |>"
        return when (expr) {
            is ArithmeticExpr -> {
                val lhsStr = expr.lhs.dStr()
                val rhsStr = expr.rhs.dStr()

                if (!lhsStr.contains("|>") && !rhsStr.contains("|>")) {
                    "(${lhsStr} ${expr.op} ${rhsStr}) = $myResult"
                } else {
                    "${lhsStr.prependIndent("| ")}\n" +
                            "|-> ${expr.op}\n" +
                            "${rhsStr.prependIndent("| ")}\n" +
                            "| = $myResult"
                }
            }

            is ComparisonExpr<*> -> "(${expr.lhs.dStr()} ${expr.comp} ${expr.rhs.dStr()}) = $myResult"
            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                "if ${expr.thisCondition} {\n${
                    expr.trueExpr.dStr().prependIndent()
                }\n} else {\n${
                    expr.falseExpr.dStr().prependIndent()
                }\n} = $myResult"
            }

            is BooleanInvertExpr -> "!${expr.expr.dStr()} = $myResult"
            is NegateExpr -> "-${expr.expr.dStr()}"
            is UnionExpr -> "(${expr.lhs.dStr()} ∪ ${expr.rhs.dStr()}) = $myResult"
            is BooleanExpr -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is VariableExpr -> expr.key.toString()
            is NumIterationTimesExpr -> "(initial: ${expr.variable.dStr()}, update: ${expr.terms} condition: ${expr.constraint}) = $myResult"
            is TypeCastExpr<*, *> -> {
                return if (expr.explicit) {
                    "(${expr.ind}) ${expr.expr.dStr()}"
                } else {
                    expr.expr.dStr()
                }
            }

            is LValueFieldExpr -> "${expr.qualifier}.${expr.field}"
            is LValueKeyExpr -> "${expr.key}"
            is ContextExpr -> "CtxExpr"
        }
    }
}