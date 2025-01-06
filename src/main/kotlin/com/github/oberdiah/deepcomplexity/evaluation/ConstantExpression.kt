package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

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

    sealed class ConstExpr<T>(val singleElementSet: T) : IExpr {
        override fun toString(): String {
            return singleElementSet.toString()
        }

        override fun getVariables(resolved: Boolean): Set<VariableExpression> {
            return setOf()
        }
    }

    class ConstExprNum(singleElementSet: NumberSet) : ConstExpr<NumberSet>(singleElementSet), IExprRetNum {
        override fun getBaseClass(): KClass<*> {
            return singleElementSet.getClass()
        }
    }

    class ConstExprBool(singleElementSet: BooleanSet) : ConstExpr<BooleanSet>(singleElementSet), IExprRetBool

    class ConstExprGeneric(singleElementSet: GenericSet) : ConstExpr<GenericSet>(singleElementSet), IExprRetGeneric {
        override fun evaluate(condition: IExprRetBool): GenericSet {
            return singleElementSet
        }

        override fun getBaseClass(): KClass<*> {
            return singleElementSet.getClass()
        }
    }
}
