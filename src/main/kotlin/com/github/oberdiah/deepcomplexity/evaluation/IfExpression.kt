package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class IfExpression(
    val trueExpr: IExpr,
    val falseExpr: IExpr,
    val condition: IExprRetBool,
) : IExpr {
    override fun deepClone(): IExpr {
        return IfExpression(trueExpr.deepClone(), falseExpr.deepClone(), condition.deepClone() as IExprRetBool)
    }

    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return trueExpr.getVariables(resolved) + falseExpr.getVariables(resolved) + condition.getVariables(resolved)
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