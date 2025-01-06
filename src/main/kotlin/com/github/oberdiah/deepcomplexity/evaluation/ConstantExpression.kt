package com.github.oberdiah.deepcomplexity.evaluation

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
            is Number -> ConstExprNum(NumberSet.singleValue(value))
            is String -> ConstExprGeneric(GenericSet.singleValue(value))
            else -> ConstExprGeneric(GenericSet.singleValue(value))
        }
    }

    fun fromAny(bool: Boolean): IExprRetBool {
        return ConstExprBool(BooleanSet.fromBoolean(bool))
    }

    fun fromAny(num: Number): IExprRetNum {
        return ConstExprNum(NumberSet.singleValue(num))
    }

    abstract class ConstExpr<T>(protected val singleElementSet: T) : IExpr {
        override fun toString(): String {
            return singleElementSet.toString()
        }

        override fun getVariables(resolved: Boolean): Set<VariableExpression> {
            return setOf()
        }
    }

    class ConstExprNum(singleElementSet: NumberSet) : ConstExpr<NumberSet>(singleElementSet), IExprRetNum {
        override fun evaluate(condition: IExprRetBool): NumberSet {
            return singleElementSet
        }
    }

    class ConstExprBool(singleElementSet: BooleanSet) : ConstExpr<BooleanSet>(singleElementSet), IExprRetBool {
        override fun evaluate(condition: IExprRetBool): BooleanSet {
            return singleElementSet
        }

        override fun getConstraints(): Map<VariableExpression, IExpr> {
            return mapOf()
        }
    }

    class ConstExprGeneric(singleElementSet: GenericSet) : ConstExpr<GenericSet>(singleElementSet), IExprRetGeneric {
        override fun evaluate(condition: IExprRetBool): GenericSet {
            return singleElementSet
        }
    }
}
