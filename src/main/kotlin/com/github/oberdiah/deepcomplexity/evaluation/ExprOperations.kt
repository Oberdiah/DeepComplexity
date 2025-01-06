package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

object ExprOperations {
    fun getSetClass(expr: IExpr): KClass<*> {
        return when (expr) {
            is IExprRetNum -> NumberSet::class
            is IExprRetBool -> Boolean::class
            is IExprRetGeneric -> GenericSet::class
            is IfExpression -> getSetClass(expr.trueExpr)
            is IntersectExpression -> getSetClass(expr.lhs)
            is RepeatExpression -> getSetClass(expr.exprToRepeat)
            is UnionExpression -> getSetClass(expr.lhs)
        }
    }
}