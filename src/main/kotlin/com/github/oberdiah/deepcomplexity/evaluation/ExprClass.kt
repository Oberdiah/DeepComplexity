package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

object ExprClass {
    fun <T : IMoldableSet<T>> getSetIndicator(expr: IExpr<T>): SetIndicator<T> {
        @Suppress("UNCHECKED_CAST")
        return when (expr) {
            is IfExpression -> expr.trueExpr.getSetIndicator()
            is IntersectExpression -> expr.lhs.getSetIndicator()
            is UnionExpression -> expr.lhs.getSetIndicator()
            is InvertExpression -> expr.expr.getSetIndicator()

            is ArithmeticExpression -> expr.lhs.getSetIndicator()
            is NegateExpression -> expr.expr.getSetIndicator()
            is NumIterationTimesExpression -> expr.getSetIndicator()
            is NumberLimitsExpression -> expr.limit.getSetIndicator()

            is BooleanExpression -> BooleanSetIndicator
            is BooleanInvertExpression -> BooleanSetIndicator
            is ComparisonExpression<*> -> BooleanSetIndicator

            is ConstExpr -> expr.singleElementSet.getSetIndicator()
            is VariableExpression -> expr.setInd
        } as SetIndicator<T>
    }
}