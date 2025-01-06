package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanExpression.BooleanOperation
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class IfExpression(
    val trueExpr: IExpr,
    val falseExpr: IExpr,
    val thisCondition: IExprRetBool,
) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return trueExpr.getVariables(resolved) + falseExpr.getVariables(resolved) + thisCondition.getVariables(resolved)
    }

    override fun getSetClass(): KClass<*> {
        return trueExpr.getSetClass()
    }

    override fun evaluate(condition: IExprRetBool): IMoldableSet {
        val evaluatedCond = thisCondition.evaluate(condition)

        val trueCondition = BooleanExpression(thisCondition, condition, BooleanOperation.AND)
        val falseCondition = BooleanExpression(InvertExpression(thisCondition), condition, BooleanOperation.AND)
        return when (evaluatedCond) {
            TRUE -> trueExpr.evaluate(trueCondition)
            FALSE -> falseExpr.evaluate(falseCondition)
            BOTH -> trueExpr.evaluate(trueCondition).union(falseExpr.evaluate(falseCondition))
            NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
        }
    }

    override fun toString(): String {
        return "if $thisCondition {\n${
            trueExpr.toString().prependIndent()
        }\n} else {\n${
            falseExpr.toString().prependIndent()
        }\n}"
    }
}