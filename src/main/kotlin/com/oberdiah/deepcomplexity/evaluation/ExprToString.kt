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
                "ExpressionChain(\n\tsupportKey = ${
                    expr.supportKey.toString().prependIndent().trim()
                }\n\tsupport = ${expr.support.toString().prependIndent().trim()}\n\texpr = ${
                    expr.expr.toString().prependIndent().trim()
                }\n)"
            }

            is ExpressionChainPointer<*> -> "ExpressionChainPointer(${expr.supportKey})"
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
}