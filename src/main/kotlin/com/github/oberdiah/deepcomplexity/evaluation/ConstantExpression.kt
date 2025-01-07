package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.IExpr.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConstantExpression {
    val TRUE = ConstExprBool(BooleanSet.TRUE)
    val FALSE = ConstExprBool(BooleanSet.FALSE)

    fun zero(expr: IExprRetNum): IExprRetNum = ConstExprNum(NumberSet.zero(expr.getBaseClass()))

    fun emptySetFromExpr(expr: IExpr): IMoldableSet {
        return when (expr.getSetClass()) {
            NumberSet::class -> NumberSet.empty(ExprClass.getBaseClass(expr))
            BooleanSet::class -> BooleanSet.NEITHER
            GenericSet::class -> GenericSet.empty()
            else -> throw IllegalArgumentException("Unknown set class ${expr.getSetClass()}")
        }
    }

    fun fullSetFromExpr(expr: IExpr): IMoldableSet {
        return when (expr.getSetClass()) {
            NumberSet::class -> NumberSet.fullRange(ExprClass.getBaseClass(expr))
            BooleanSet::class -> BooleanSet.BOTH
            GenericSet::class -> GenericSet.everyValue()
            else -> throw IllegalArgumentException("Unknown set class ${expr.getSetClass()}")
        }
    }

    fun setToConstExpr(set: IMoldableSet): ConstExpr<*> {
        return when (set) {
            is BooleanSet -> ConstExprBool(set)
            is GenericSet -> ConstExprGeneric(set)
            is NumberSet.NumberSetImpl<*> -> ConstExprNum(set)
        }
    }

    fun fullExprFromExpr(expr: IExpr): IExpr {
        return setToConstExpr(fullSetFromExpr(expr))
    }

    fun emptyExprFromExpr(expr: IExpr): IExpr {
        return setToConstExpr(emptySetFromExpr(expr))
    }

    fun fromAny(value: Any): IExpr {
        // When adding to this it is likely you'll also want to add to Unresolved.fromElement
        return when (value) {
            is Boolean -> ConstExprBool(BooleanSet.fromBoolean(value))
            is Number -> fromAny(value)
            is String -> ConstExprGeneric(GenericSet.singleValue(value))
            else -> ConstExprGeneric(GenericSet.singleValue(value))
        }
    }

    fun fromAny(bool: Boolean): IExprRetBool {
        return ConstExprBool(BooleanSet.fromBoolean(bool))
    }

    fun fromAny(value: Number): IExprRetNum {
        return when (value) {
            is Byte -> ConstExprNum(NumberSet.singleValue(value))
            is Short -> ConstExprNum(NumberSet.singleValue(value))
            is Int -> ConstExprNum(NumberSet.singleValue(value))
            is Long -> ConstExprNum(NumberSet.singleValue(value))
            is Float -> ConstExprNum(NumberSet.singleValue(value))
            is Double -> ConstExprNum(NumberSet.singleValue(value))
            else -> throw IllegalArgumentException("Unknown number type")
        }
    }
}
