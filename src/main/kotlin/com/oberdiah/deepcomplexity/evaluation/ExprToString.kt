package com.oberdiah.deepcomplexity.evaluation

object ExprToString {
    fun <T : Any> toString(expr: Expr<T>): String {
        return when (expr) {
            is ArithmeticExpr -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is ComparisonExpr<*> -> "(${expr.lhs} ${expr.comp} ${expr.rhs})"
            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                "if ${expr.thisCondition} {\n${
                    expr.trueExpr.toString().prependIndent()
                }\n} else {\n${
                    expr.falseExpr.toString().prependIndent()
                }\n}"
            }

            is BooleanInvertExpr -> "!${expr.expr}"
            is NegateExpr -> "-${expr.expr}"
            is UnionExpr -> "(${expr.lhs} ∪ ${expr.rhs})"
            is BooleanExpr -> "(${expr.lhs} ${expr.op} ${expr.rhs})"
            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${expr.expr}"
                } else {
                    "${expr.expr}"
                }
            }

            is VariableExpr -> expr.key.toString()
            is VarsExpr -> expr.vars.toString()
            is ExpressionChain<*> -> {
                "${expr.supportKey} = ${expr.support}\n${expr.expr}"
            }

            is ExpressionChainPointer<*> -> expr.supportKey.toString()
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
            is TypeCastExpr<*, *> -> toExprKeyString(expr.expr)
            is VarsExpr -> "CtxExpr"
            is ExpressionChain<*> -> "'${toExprKeyString(expr.support)}+${toExprKeyString(expr.expr)}'"
            is ExpressionChainPointer<*> -> expr.toString()
        }
    }

    fun <T : Any> toDebugString(expr: Expr<T>): String {
        // Was previously preCalculatedBundle?.toDebugString() ?: "Not evaluated"
        val myResult = "<| ${"BROKEN FOR NOW"} |>"
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
            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${expr.expr.dStr()}"
                } else {
                    expr.expr.dStr()
                }
            }

            is VarsExpr -> "CtxExpr"
            is ExpressionChain<*> -> {
                "${expr.supportKey} = ${expr.support.dStr()}\n${expr.expr.dStr()}"
            }

            is ExpressionChainPointer<*> -> expr.supportKey.toString()
        }
    }
}