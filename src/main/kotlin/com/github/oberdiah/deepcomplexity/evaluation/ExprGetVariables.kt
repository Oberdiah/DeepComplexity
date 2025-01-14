package com.github.oberdiah.deepcomplexity.evaluation

object ExprGetVariables {
    fun getVariables(expr: IExpr, resolved: Boolean): Set<VariableExpression> {
        return when (expr) {
            is ConstExpr<*> -> emptySet()
            is VariableExpression.VariableImpl<*> -> {
                val resolvedVariables = expr.resolvedInto?.getVariables(resolved) ?: emptySet()

                if ((expr.isResolved() && resolved) || (!expr.isResolved() && !resolved)) {
                    resolvedVariables + expr
                } else {
                    resolvedVariables
                }
            }

            is BooleanExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is ComparisonExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is BooleanInvertExpression -> expr.expr.getVariables(resolved)
            is NegateExpression -> expr.expr.getVariables(resolved)
            is ArithmeticExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is IfExpression -> expr.trueExpr.getVariables(resolved) +
                    expr.falseExpr.getVariables(resolved) +
                    expr.thisCondition.getVariables(resolved)

            is IntersectExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is RepeatExpression -> expr.numRepeats.getVariables(resolved) + expr.exprToRepeat.getVariables(resolved)
            is UnionExpression -> expr.lhs.getVariables(resolved) + expr.rhs.getVariables(resolved)
            is NumberLimitsExpression -> expr.limit.getVariables(resolved) + expr.shouldFlipCmp.getVariables(resolved)
            is InvertExpression -> expr.expr.getVariables(resolved)
            is NumIterationTimesExpression -> expr.constraint.getVariables(resolved) + expr.variable
        }
    }
}