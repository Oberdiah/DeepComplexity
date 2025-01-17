package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.math.exp
import kotlin.reflect.KClass

object ExprClass {
    /**
     * The class of the set that the expression returns.
     */
    fun <T : IMoldableSet<T>> getSetClass(expr: IExpr<T>): KClass<*> {
        return when (expr) {
            is IfExpression -> expr.trueExpr.getSetClass()
            is IntersectExpression -> expr.lhs.getSetClass()
            is UnionExpression -> expr.lhs.getSetClass()
            is InvertExpression -> expr.expr.getSetClass()
            is ArithmeticExpression -> NumberSet::class
            is BooleanExpression -> BooleanSet::class
            is BooleanInvertExpression -> BooleanSet::class
            is ComparisonExpression -> BooleanSet::class
            is NegateExpression -> NumberSet::class
            is NumIterationTimesExpression -> NumberSet::class
            is NumberLimitsExpression -> NumberSet::class
            is ConstExpr -> expr.singleElementSet.getSetClass()
            is VariableExpression.VariableImpl -> expr.setClazz
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