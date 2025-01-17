package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConstantExpression {
    val TRUE = ConstExpr(BooleanSet.TRUE)
    val FALSE = ConstExpr(BooleanSet.FALSE)

    fun zero(expr: IExpr<NumberSet>): ConstExpr<NumberSet> = ConstExpr(NumberSet.zero(expr.getBaseClass()))

    fun <T : IMoldableSet<T>> emptySetFromExpr(expr: IExpr<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (expr.getSetClass()) {
            NumberSet::class -> NumberSet.empty(ExprClass.getBaseClass(expr))
            BooleanSet::class -> BooleanSet.NEITHER
            GenericSet::class -> GenericSet.empty()
            else -> throw IllegalArgumentException("Unknown set class ${expr.getSetClass()}")
        } as T
    }

    fun <T : IMoldableSet<T>> fullSetFromExpr(expr: IExpr<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (expr.getSetClass()) {
            NumberSet::class -> NumberSet.fullRange(ExprClass.getBaseClass(expr))
            BooleanSet::class -> BooleanSet.BOTH
            GenericSet::class -> GenericSet.everyValue()
            else -> throw IllegalArgumentException("Unknown set class ${expr.getSetClass()}")
        } as T
    }

    fun <T : IMoldableSet<T>> setToConstExpr(set: T): ConstExpr<T> {
        @Suppress("UNCHECKED_CAST")
        return when (set) {
            is BooleanSet -> ConstExpr(set)
            is GenericSet -> ConstExpr(set)
            is NumberSet -> ConstExpr(set)
            else -> {
                throw IllegalArgumentException("Unknown set type $set")
            }
        } as ConstExpr<T>
    }

    fun <T : IMoldableSet<T>> fullExprFromExpr(expr: IExpr<T>): IExpr<T> {
        return setToConstExpr(fullSetFromExpr(expr))
    }

    fun <T : IMoldableSet<T>> emptyExprFromExpr(expr: IExpr<T>): IExpr<T> {
        return setToConstExpr(emptySetFromExpr(expr))
    }

    fun fromAny(value: Any): IExpr<*> {
        return when (value) {
            is Boolean -> fromAny(value)
            is Number -> fromAny(value)
            is String -> ConstExpr(GenericSet.singleValue(value))
            else -> ConstExpr(GenericSet.singleValue(value))
        }
    }

    fun fromAny(bool: Boolean): ConstExpr<BooleanSet> {
        return ConstExpr(BooleanSet.fromBoolean(bool))
    }

    fun fromAny(value: Number): IExpr<NumberSet> {
        return when (value) {
            is Byte -> ConstExpr(NumberSet.singleValue(value))
            is Short -> ConstExpr(NumberSet.singleValue(value))
            is Int -> ConstExpr(NumberSet.singleValue(value))
            is Long -> ConstExpr(NumberSet.singleValue(value))
            is Float -> ConstExpr(NumberSet.singleValue(value))
            is Double -> ConstExpr(NumberSet.singleValue(value))
            else -> throw IllegalArgumentException("Unknown number type")
        }
    }
}
