package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

object ExprClass {
    /**
     * The class of the set that the expression returns.
     */
    fun getSetClass(expr: IExpr): KClass<*> {
        return when (expr) {
            is IExprRetNum -> NumberSet::class
            is IExprRetBool -> BooleanSet::class
            is IExprRetGeneric -> GenericSet::class
            is IfExpression -> getSetClass(expr.trueExpr)
            is IntersectExpression -> getSetClass(expr.lhs)
            is RepeatExpression -> getSetClass(expr.exprToRepeat)
            is UnionExpression -> getSetClass(expr.lhs)
            is InvertExpression -> getSetClass(expr.expr)
        }
    }

    /**
     * The class of the elements in the set that the expression returns.
     */
    fun getBaseClass(expr: IExpr): KClass<*> {
        return when (expr) {
            is IExprRetBool -> Boolean::class
            is ConstExprGeneric -> expr.singleElementSet.getClass()
            is ConstExprNum -> expr.singleElementSet.getClass()
            is VariableExpression.VariableGeneric -> throw IllegalStateException("Base class for a generic is a strange concept...")
            is VariableExpression.VariableNumber -> expr.clazz
            is ArithmeticExpression -> getBaseClass(expr.lhs)
            is IfExpression -> getBaseClass(expr.trueExpr)
            is IntersectExpression -> getBaseClass(expr.lhs)
            is RepeatExpression -> getBaseClass(expr.exprToRepeat)
            is UnionExpression -> getBaseClass(expr.lhs)
            is NegateExpression -> getBaseClass(expr.expr)
            is NumberLimitsExpression -> getBaseClass(expr.limit)
            is InvertExpression -> getBaseClass(expr.expr)
            is NumIterationTimesExpression -> getBaseClass(expr.constraint)
        }
    }
}