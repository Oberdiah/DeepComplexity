package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.BooleanBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.NumberBundle

object ConstantExpression {
    val TRUE = ConstExpr.new(BooleanBundle.TRUE.toConstVariance())
    val FALSE = ConstExpr.new(BooleanBundle.FALSE.toConstVariance())

    fun <T : Number> zero(setIndicator: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr.new(NumberBundle.zero(setIndicator).toConstVariance())

    fun <T : Number> zero(expr: Expr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Number> one(setIndicator: NumberSetIndicator<T>): ConstExpr<T> =
        ConstExpr.new(NumberBundle.one(setIndicator).toConstVariance())

    fun <T : Number> one(expr: Expr<T>): ConstExpr<T> =
        zero(expr.getNumberSetIndicator())

    fun <T : Any> fullExprFromExprAndKey(expr: Expr<T>, key: Context.Key): Expr<T> =
        ConstExpr.new(expr.ind.newVariance(key))

    fun fromAny(value: Any): Expr<*> {
        return ConstExpr.new(
            when (value) {
                is Boolean -> BooleanBundle.fromBoolean(value)
                is Number -> NumberBundle.newFromConstant(value)
                is String -> TODO()
                else -> TODO()
            }.toConstVariance()
        )
    }
}
