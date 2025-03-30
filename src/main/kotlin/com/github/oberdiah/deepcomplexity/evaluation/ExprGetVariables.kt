package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet

object ExprGetVariables {
    fun <T : IMoldableSet<T>> getVariables(expr: IExpr<T>, resolved: Boolean): Set<VariableExpression<*>> {
        return when (expr) {
            is ConstExpr -> emptySet()
            is VariableExpression -> {
                val resolvedVariables = expr.resolvedInto?.getVariables(resolved) ?: emptySet()

                if ((expr.isResolved() && resolved) || (!expr.isResolved() && !resolved)) {
                    resolvedVariables + expr
                } else {
                    resolvedVariables
                }
            }

            is BooleanExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is ComparisonExpression<*> -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is BooleanInvertExpression -> expr.expr.getVariables(resolved)
            is NegateExpression -> expr.expr.getVariables(resolved)
            is ArithmeticExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is IfExpression -> expr.trueExpr.getVariables(resolved) +
                    expr.falseExpr.getVariables(resolved) +
                    expr.thisCondition.getVariables(resolved)

            is IntersectExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is UnionExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is NumberLimitsExpression -> expr.limit.getVariables(resolved) + expr.shouldFlipCmp.getVariables(resolved)
            is InvertExpression -> expr.expr.getVariables(resolved)
            is NumIterationTimesExpression -> setOf(expr.variable)
            is TypeCastExpression<*, *> -> expr.expr.getVariables(resolved)
        }
    }
}