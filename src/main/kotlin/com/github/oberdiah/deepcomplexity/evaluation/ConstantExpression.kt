package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator

object ConstantExpression {
    val TRUE = ConstExpr(true, BooleanSetIndicator)
    val FALSE = ConstExpr(false, BooleanSetIndicator)

    fun <T : Number> zero(ind: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr(ind.getZero(), ind)

    fun <T : Number> zero(expr: Expr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Number> one(ind: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr(ind.getOne(), ind)

    fun <T : Number> one(expr: Expr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Any> fromAny(value: T): ConstExpr<T> {
        return ConstExpr(value, SetIndicator.fromValue(value))
    }
}
