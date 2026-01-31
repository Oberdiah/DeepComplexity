package com.oberdiah.deepcomplexity.evaluation

object ExprToString {
    fun toString(expr: Expr<*>): String {
        val tagsMap = ExpressionTagger.buildTags(expr)

        if (tagsMap.isEmpty()) {
            return toStringWithTags(expr, tagsMap)
        } else {
            val mainExpr = toStringWithTags(expr, tagsMap)
            return "${ExpressionTagger.tagsToString(tagsMap)}\nResult = $mainExpr"
        }
    }

    fun toStringWithTags(expr: Expr<*>, tagsMap: TagsMap): String {
        tagsMap[expr]?.let { return it }

        return when (expr) {
            is ArithmeticExpr -> "(${toStringWithTags(expr.lhs, tagsMap)} ${expr.op} ${
                toStringWithTags(
                    expr.rhs,
                    tagsMap
                )
            })"

            is ComparisonExpr<*> -> "(${toStringWithTags(expr.lhs, tagsMap)} ${expr.comp} ${
                toStringWithTags(
                    expr.rhs,
                    tagsMap
                )
            })"

            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                "if ${toStringWithTags(expr.thisCondition, tagsMap)} {\n${
                    toStringWithTags(expr.trueExpr, tagsMap).prependIndent()
                }\n} else {\n${
                    toStringWithTags(expr.falseExpr, tagsMap).prependIndent()
                }\n}"
            }

            is BooleanInvertExpr -> "!${toStringWithTags(expr.expr, tagsMap)}"
            is NegateExpr -> "-${toStringWithTags(expr.expr, tagsMap)}"
            is UnionExpr -> "(${toStringWithTags(expr.lhs, tagsMap)} ∪ ${toStringWithTags(expr.rhs, tagsMap)})"
            is BooleanExpr -> "(${toStringWithTags(expr.lhs, tagsMap)} ${expr.op} ${
                toStringWithTags(
                    expr.rhs,
                    tagsMap
                )
            })"

            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${toStringWithTags(expr.expr, tagsMap)}"
                } else {
                    toStringWithTags(expr.expr, tagsMap)
                }
            }

            is VariableExpr -> expr.key.toString()
            is VarsExpr -> expr.vars.toString()
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
        }
    }
}

