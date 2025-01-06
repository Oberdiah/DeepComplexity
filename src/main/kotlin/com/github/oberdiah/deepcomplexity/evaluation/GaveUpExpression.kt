package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

interface GaveUpExpression : IExpr {
    companion object {
        fun fromExpr(expr: IExpr): GaveUpExpression {
            return when (expr.getSetClass()) {
                NumberSet::class -> GaveUpExprNum
                BooleanSet::class -> GaveUpExprBool
                GenericSet::class -> GaveUpExprGeneric
                else -> throw IllegalArgumentException("Unknown set class")
            }
        }
    }

    abstract class GaveUpExpr : GaveUpExpression {
        override fun getVariables(resolved: Boolean): Set<VariableExpression> {
            return setOf()
        }

        override fun toString(): String {
            return "X"
        }
    }

    data object GaveUpExprNum : GaveUpExpr(), IExprRetNum {
        override fun evaluate(condition: IExprRetBool): NumberSet {
            return NumberSet.gaveUp()
        }

        override fun getSetClass(): KClass<*> {
            return NumberSet::class
        }
    }

    data object GaveUpExprBool : GaveUpExpr(), IExprRetBool {
        override fun evaluate(condition: IExprRetBool): BooleanSet {
            return BooleanSet.BOTH
        }

        override fun getSetClass(): KClass<*> {
            return BooleanSet::class
        }
    }

    data object GaveUpExprGeneric : GaveUpExpr(), IExprRetGeneric {
        override fun evaluate(condition: IExprRetBool): GenericSet {
            return TODO()
        }

        override fun getSetClass(): KClass<*> {
            return GenericSet::class
        }
    }
}