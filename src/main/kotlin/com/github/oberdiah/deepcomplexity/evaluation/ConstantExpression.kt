package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.*

object ConstantExpression {
    val TRUE = ConstExpr(BooleanSet.TRUE)
    val FALSE = ConstExpr(BooleanSet.FALSE)

    fun <T : NumberSet<T>> zero(expr: IExpr<T>): ConstExpr<T> = ConstExpr(NumberSet.zero(expr.getSetIndicator()))

    fun <T : IMoldableSet<T>> emptySetFromExpr(expr: IExpr<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (expr.getSetIndicator()) {
            is NumberSetIndicator -> mapNumExprToSet(expr) { NumberSet.empty(it.getSetIndicator()) }!!
            BooleanSetIndicator -> BooleanSet.NEITHER
            GenericSetIndicator -> GenericSet.empty()
        } as T
    }

    fun <T : IMoldableSet<T>> fullSetFromExpr(expr: IExpr<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (expr.getSetIndicator()) {
            is NumberSetIndicator -> mapNumExprToSet(expr) { NumberSet.fullRange(it.getSetIndicator()) }!!
            BooleanSetIndicator -> BooleanSet.BOTH
            GenericSetIndicator -> GenericSet.everyValue()
        } as T
    }

    fun <T : IMoldableSet<T>> fullExprFromExpr(expr: IExpr<T>): IExpr<T> {
        return ConstExpr(fullSetFromExpr(expr))
    }

    fun <T : IMoldableSet<T>> emptyExprFromExpr(expr: IExpr<T>): IExpr<T> {
        return ConstExpr(emptySetFromExpr(expr))
    }

    fun fromAny(value: Any): IExpr<*> {
        return when (value) {
            is Boolean -> ConstExpr(BooleanSet.fromBoolean(value))
            is Number -> ConstExpr(NumberSet.singleValue(value))
            is String -> ConstExpr(GenericSet.singleValue(value))
            else -> ConstExpr(GenericSet.singleValue(value))
        }
    }
}
