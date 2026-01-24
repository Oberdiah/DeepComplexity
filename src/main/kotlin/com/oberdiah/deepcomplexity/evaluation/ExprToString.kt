package com.oberdiah.deepcomplexity.evaluation

object ExprToString {
    fun <T : Any> toString(expr: Expr<T>, tagsMap: TagsMap = emptyMap()): String {
        tagsMap[expr]?.let { return it }

        return when (expr) {
            is ArithmeticExpr -> "(${toString(expr.lhs, tagsMap)} ${expr.op} ${toString(expr.rhs, tagsMap)})"
            is ComparisonExpr<*> -> "(${toString(expr.lhs, tagsMap)} ${expr.comp} ${toString(expr.rhs, tagsMap)})"
            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                "if ${toString(expr.thisCondition, tagsMap)} {\n${
                    toString(expr.trueExpr, tagsMap).prependIndent()
                }\n} else {\n${
                    toString(expr.falseExpr, tagsMap).prependIndent()
                }\n}"
            }

            is BooleanInvertExpr -> "!${toString(expr.expr, tagsMap)}"
            is NegateExpr -> "-${toString(expr.expr, tagsMap)}"
            is UnionExpr -> "(${toString(expr.lhs, tagsMap)} ∪ ${toString(expr.rhs, tagsMap)})"
            is BooleanExpr -> "(${toString(expr.lhs, tagsMap)} ${expr.op} ${toString(expr.rhs, tagsMap)})"
            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${toString(expr.expr, tagsMap)}"
                } else {
                    toString(expr.expr, tagsMap)
                }
            }

            is VariableExpr -> expr.key.toString()
            is VarsExpr -> expr.vars.toString()
            is TagsExpr<*> -> {
                toString(expr.expr, tagsMap + expr.tags)
            }
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
            is BooleanExpr -> "(${toString(expr.lhs)} ${expr.op} ${toString(expr.rhs)})"
            is VariableExpr -> expr.key.toString()
            is TypeCastExpr<*, *> -> toExprKeyString(expr.expr)
            is VarsExpr -> "CtxExpr"
            is TagsExpr<*> -> toExprKeyString(expr.expr)
        }
    }
}

