package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.IExpr.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConstantExpression {
    val TRUE = ConstExprBool(BooleanSet.TRUE)
    val FALSE = ConstExprBool(BooleanSet.FALSE)

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
