package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

class ConstantExpression<T : MoldableSet<T>>(private val singleElementSet: T) : Expression<T>(singleElementSet::class) {
    override fun toString(): String {
        return singleElementSet.toString()
    }

    override fun evaluate(): T {
        return singleElementSet
    }

    companion object {
        fun fromAny(value: Any): ConstantExpression<*> {
            // When adding to this it is likely you'll also want to add to UnresolvedExpression.fromElement
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
}