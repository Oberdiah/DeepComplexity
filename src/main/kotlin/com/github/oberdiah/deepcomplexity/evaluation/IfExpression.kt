package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

class IfExpression<T : MoldableSet<T>>(
    val trueExpr: Expression<T>,
    val falseExpr: Expression<T>,
    val condition: Expression<BooleanSet>,
) : Expression<T> {
    override fun getSetClass(): KClass<*> {
        return trueExpr.getSetClass()
    }

    override fun evaluate(): T {
        val condition = condition.evaluate()
        return when (condition) {
            TRUE -> trueExpr.evaluate()
            FALSE -> falseExpr.evaluate()
            BOTH -> trueExpr.evaluate().union(falseExpr.evaluate())
        }
    }

    override fun toString(): String {
        return "if ($condition) $trueExpr else $falseExpr"
    }
}