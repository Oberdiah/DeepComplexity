package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class IfExpression(
    val trueExpr: IExpr,
    val falseExpr: IExpr,
    val condition: IExprRetBool,
) : IExpr {
    override fun getCurrentlyUnresolved(): Set<VariableExpression> {
        return trueExpr.getCurrentlyUnresolved() + falseExpr.getCurrentlyUnresolved() + condition.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return trueExpr.getSetClass()
    }

    override fun evaluate(): IMoldableSet {
        val condition = condition.evaluate()
        return when (condition) {
            TRUE -> trueExpr.evaluate()
            FALSE -> falseExpr.evaluate()
            BOTH -> trueExpr.evaluate().union(falseExpr.evaluate())
            NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
        }
    }

    override fun toString(): String {
        return "if $condition {\n${
            trueExpr.toString().prependIndent()
        }\n} else {\n${
            falseExpr.toString().prependIndent()
        }\n}"
    }
}