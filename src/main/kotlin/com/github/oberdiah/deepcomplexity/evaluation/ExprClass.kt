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

    /**
     * The class of the set that the expression returns.
     */
    fun <T : IMoldableSet<T>> getSetClass(expr: IExpr<T>): SetClass {
        @Suppress("UNCHECKED_CAST")
        return when (expr) {
            is IfExpression -> expr.trueExpr.getSetClass()
            is IntersectExpression -> expr.lhs.getSetClass()
            is UnionExpression -> expr.lhs.getSetClass()
            is InvertExpression -> expr.expr.getSetClass()

            is ArithmeticExpression -> NumberSetClass
            is BooleanExpression -> BooleanSetClass
            is BooleanInvertExpression -> BooleanSetClass
            is ComparisonExpression<*> -> BooleanSetClass
            is NegateExpression -> NumberSetClass
            is NumIterationTimesExpression -> NumberSetClass
            is NumberLimitsExpression -> NumberSetClass

            is ConstExpr -> expr.singleElementSet.getSetClass()
            is VariableExpression -> expr.setClazz
        }
    }

    /**
     * The class of the elements in the set that the expression returns.
     */
    fun <T : IMoldableSet<T>> getBaseClass(expr: IExpr<T>): KClass<*> {
        return when (expr) {
            is ArithmeticExpression -> expr.lhs.getBaseClass()
            is BooleanExpression -> Boolean::class
            is BooleanInvertExpression -> Boolean::class
            is ComparisonExpression<*> -> Boolean::class
            is ConstExpr -> expr.singleElementSet.getClass()
            is IfExpression -> expr.trueExpr.getBaseClass()
            is IntersectExpression -> expr.lhs.getBaseClass()
            is InvertExpression -> expr.expr.getBaseClass()
            is NegateExpression -> expr.expr.getBaseClass()
            is NumIterationTimesExpression -> expr.variable.getBaseClass()
            is NumberLimitsExpression -> expr.limit.getBaseClass()
            is UnionExpression -> expr.lhs.getBaseClass()
            is VariableExpression -> expr.baseClazz
        }
    }
}