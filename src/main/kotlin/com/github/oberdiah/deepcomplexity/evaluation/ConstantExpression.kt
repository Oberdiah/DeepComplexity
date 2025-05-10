package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.VarianceBundle

object ConstantExpression {
    val TRUE = ConstExpr.new(BooleanSet.TRUE.withEphemeralVariance())
    val FALSE = ConstExpr.new(BooleanSet.FALSE.withEphemeralVariance())

    fun <T : Number> zero(setIndicator: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr.new(NumberSet.zero(setIndicator).withEphemeralVariance())

    fun <T : Number> zero(expr: IExpr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Number> one(setIndicator: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr.new(NumberSet.one(setIndicator).withEphemeralVariance())

    fun <T : Number> one(expr: IExpr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Any> fullSetFromExprAndKey(expr: IExpr<T>, key: Context.Key): VarianceBundle<T> =
        expr.getSetIndicator().newFullBundle().withVariance(key)

    fun <T : Any> fullExprFromExprAndKey(expr: IExpr<T>, key: Context.Key): IExpr<T> =
        ConstExpr.new(fullSetFromExprAndKey(expr, key))

    fun fromAny(value: Any): IExpr<*> {
        return ConstExpr.new(
            when (value) {
                is Boolean -> BooleanSet.fromBoolean(value)
                is Number -> NumberSet.newFromConstant(value)
                is String -> TODO()
                else -> TODO()
            }.withEphemeralVariance()
        )
    }
}
