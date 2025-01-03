package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.github.weisj.jsvg.T
import kotlin.reflect.KClass

object ConstantExpression {
    fun fromAny(value: Any): Expr {
        // When adding to this it is likely you'll also want to add to UnresolvedExpression.fromElement
        return when (value) {
            is Boolean -> ConstExprBool(BooleanSet.fromBoolean(value))
            is Byte -> ConstExprNum(NumberSet.singleValue(value))
            is Short -> ConstExprNum(NumberSet.singleValue(value))
            is Int -> ConstExprNum(NumberSet.singleValue(value))
            is Long -> ConstExprNum(NumberSet.singleValue(value))
            is Float -> ConstExprNum(NumberSet.singleValue(value))
            is Double -> ConstExprNum(NumberSet.singleValue(value))
            is String -> ConstExprGeneric(GenericSet.singleValue(value))
            else -> ConstExprGeneric(GenericSet.singleValue(value))
        }
    }

    abstract class ConstExpr<T>(protected val singleElementSet: T) : Expr {
        override fun toString(): String {
            return singleElementSet.toString()
        }

        override fun getUnresolved(): Set<UnresolvedExpression.Unresolved> {
            return setOf()
        }
    }

    class ConstExprNum(singleElementSet: NumberSet) : ConstExpr<NumberSet>(singleElementSet), ExprRetNum {
        override fun evaluate(): NumberSet {
            return singleElementSet
        }
    }

    class ConstExprBool(singleElementSet: BooleanSet) : ConstExpr<BooleanSet>(singleElementSet), ExprRetBool {
        override fun evaluate(): BooleanSet {
            return singleElementSet
        }
    }

    class ConstExprGeneric(singleElementSet: GenericSet) : ConstExpr<GenericSet>(singleElementSet), ExprRetGeneric {
        override fun evaluate(): GenericSet {
            return singleElementSet
        }
    }
}
