package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.math.exp
import kotlin.reflect.KClass

object ExprClass {
    /**
     * The class of the set that the expression returns.
     */
    fun <T : IMoldableSet<T>> getSetClass(expr: IExpr<T>): SetClass<T> {
        @Suppress("UNCHECKED_CAST")
        return when (expr) {
            is IfExpression -> expr.trueExpr.getSetClass()
            is IntersectExpression -> expr.lhs.getSetClass()
            is UnionExpression -> expr.lhs.getSetClass()
            is InvertExpression -> expr.expr.getSetClass()
            is ArithmeticExpression -> NumberSetClass
            is BooleanExpression -> BooleanSetClass
            is BooleanInvertExpression -> BooleanSetClass
            is ComparisonExpression -> BooleanSetClass
            is NegateExpression -> NumberSetClass
            is NumIterationTimesExpression -> NumberSetClass
            is NumberLimitsExpression -> NumberSetClass
            is ConstExpr -> expr.singleElementSet.getSetClass()
            is VariableExpression.VariableImpl -> expr.setClazz
        } as SetClass<T>
    }

    /**
     * The class of the elements in the set that the expression returns.
     */
    fun <T : IMoldableSet<T>> getBaseClass(expr: IExpr<T>): KClass<*> {
        return when (expr) {
            is ArithmeticExpression -> expr.lhs.getBaseClass()
            is BooleanExpression -> Boolean::class
            is BooleanInvertExpression -> Boolean::class
            is ComparisonExpression -> Boolean::class
            is ConstExpr -> expr.singleElementSet.getClass()
            is IfExpression -> expr.trueExpr.getBaseClass()
            is IntersectExpression -> expr.lhs.getBaseClass()
            is InvertExpression -> expr.expr.getBaseClass()
            is NegateExpression -> expr.expr.getBaseClass()
            is NumIterationTimesExpression -> expr.variable.getBaseClass()
            is NumberLimitsExpression -> expr.limit.getBaseClass()
            is UnionExpression -> expr.lhs.getBaseClass()
            is VariableExpression.VariableImpl -> expr.baseClazz
        }
    }
}