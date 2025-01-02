package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

class ConstantExpression<T : MoldableSet>(private val singleElementSet: T) : Expression<T>(singleElementSet::class) {
    override fun toString(): String {
        return singleElementSet.toString()
    }

    override fun evaluate(): T {
        return singleElementSet
    }

    companion object {
        fun fromAny(value: Any): Expression<MoldableSet> {
            // When adding to this it is likely you'll also want to add to UnresolvedExpression.fromElement
            val q = when (value) {
                is Boolean -> BooleanSet.fromBoolean(value)
                is Byte -> NumberSet.singleValue(value)
                is Short -> NumberSet.singleValue(value)
                is Int -> NumberSet.singleValue(value)
                is Long -> NumberSet.singleValue(value)
                is Float -> NumberSet.singleValue(value)
                is Double -> NumberSet.singleValue(value)
                is String -> GenericSet.singleValue(value)
                else -> GenericSet.singleValue(value)
            }
            return ConstantExpression(q)
        }
    }
}