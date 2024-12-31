package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

class ConstantExpression<T>(private val singleElementSet: MoldableSet<T>) : Expression<MoldableSet<T>> {
    companion object {
        fun fromAny(value: Any): ConstantExpression<*> {
            return when (value) {
                is Boolean -> ConstantExpression(BooleanSet.fromBoolean(value))
                is Byte -> ConstantExpression(NumberSet.singleValue(value))
                is Short -> ConstantExpression(NumberSet.singleValue(value))
                is Int -> ConstantExpression(NumberSet.singleValue(value))
                is Long -> ConstantExpression(NumberSet.singleValue(value))
                is Float -> ConstantExpression(NumberSet.singleValue(value))
                is Double -> ConstantExpression(NumberSet.singleValue(value))
                is String -> ConstantExpression(GenericSet.singleValue(value))
                else -> ConstantExpression(GenericSet.singleValue(value))
            }
        }
    }

    override fun evaluate(): MoldableSet<T> {
        return singleElementSet
    }
}