package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet

object ConstantExpression {
    val TRUE = ConstExpr.new(BooleanSet.TRUE.toConstVariance())
    val FALSE = ConstExpr.new(BooleanSet.FALSE.toConstVariance())

    fun <T : Number> zero(setIndicator: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr.new(NumberSet.zero(setIndicator).toConstVariance())

    fun <T : Number> zero(expr: Expr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Number> one(setIndicator: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr.new(NumberSet.one(setIndicator).toConstVariance())

    fun <T : Number> one(expr: Expr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Any> fullExprFromExprAndKey(expr: Expr<T>, key: Context.Key): Expr<T> =
        ConstExpr.new(expr.ind.newVariance(key))

    fun fromAny(value: Any): Expr<*> {
        return ConstExpr.new(
            when (value) {
                is Boolean -> BooleanSet.fromBoolean(value)
                is Number -> NumberSet.newFromConstant(value)
                is String -> TODO()
                else -> TODO()
            }.toConstVariance()
        )
    }
}
