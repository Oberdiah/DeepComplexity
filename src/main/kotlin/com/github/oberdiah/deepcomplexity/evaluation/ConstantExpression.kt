package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.TypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.ConstrainedSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConstantExpression {
    val TRUE = ConstExpr(BooleanSet.TRUE)
    val FALSE = ConstExpr(BooleanSet.FALSE)

    fun <T : TypedNumberSet<*, T>> zero(setIndicator: NumberSetIndicator<*, T>): ConstExpr<T> =
        ConstExpr(NumberSet.zero(setIndicator))

    fun <T : NumberSet<T>> zero(expr: IExpr<T>): ConstExpr<T> =
        ConstExpr(NumberSet.zero(expr.getSetIndicator()))

    fun <T : ConstrainedSet<T>> fullSetFromExprAndKey(expr: IExpr<T>, key: Context.Key): T =
        expr.getSetIndicator().newFullSet(key)

    fun <T : ConstrainedSet<T>> fullExprFromExprAndKey(expr: IExpr<T>, key: Context.Key): IExpr<T> =
        ConstExpr(fullSetFromExprAndKey(expr, key))

    fun <T : ConstrainedSet<T>> emptySetFromExpr(expr: IExpr<T>): T =
        expr.getSetIndicator().newEmptySet()

    fun <T : ConstrainedSet<T>> emptyExprFromExpr(expr: IExpr<T>): IExpr<T> =
        ConstExpr(emptySetFromExpr(expr))

    fun fromAny(value: Any): IExpr<*> {
        return when (value) {
            is Boolean -> ConstExpr(BooleanSet.fromBoolean(value))
            is Number -> ConstExpr(NumberSet.singleValue(value))
            is String -> ConstExpr(GenericSet.singleValue(value))
            else -> ConstExpr(GenericSet.singleValue(value))
        }
    }
}
