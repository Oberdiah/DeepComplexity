package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.utilities.Utilities

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

            is VariableExpr -> expr.resolvesTo.toString()
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
            is VariableExpr -> expr.resolvesTo.toString()
            is TypeCastExpr<*, *> -> toExprKeyString(expr.expr)
            is VarsExpr -> "CtxExpr"
            is ExpressionChain<*> -> "'${toExprKeyString(expr.support)}+${toExprKeyString(expr.expr)}'"
            is ExpressionChainPointer<*> -> expr.toString()
        }
    }

    fun <T : Any> toDebugString(expr: Expr<T>): String {
        // Note: Although this looks sketchy, it's actually better than the old system of evaluating
        // this bundle on the fly as it's more representative of what actually happened.
        // Unless we generate more than 2 billion expressions, which we won't, this is absolutely fine.
        // Even then, we won't collide unless those are the same exact expression. In short, it won't happen.
        // The other failure mode is that we accidentally change the expression tree somehow, and that won't
        // happen as it's read-only when entering the evaluate method.
        val preCalculatedBundle =
            Utilities.TEST_GLOBALS.EXPR_HASH_BUNDLES[expr.completelyUniqueValueForDebugUseOnly]
        val myResult = "<| ${preCalculatedBundle?.toDebugString() ?: "Not evaluated"} |>"
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
            is VariableExpr -> expr.resolvesTo.toString()
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