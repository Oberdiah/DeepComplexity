package com.github.oberdiah.deepcomplexity.evaluation

import kotlin.reflect.KClass

class IfExpression(
    val trueExpr: IExpr,
    val falseExpr: IExpr,
    val thisCondition: IExprRetBool,
) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return trueExpr.getVariables(resolved) + falseExpr.getVariables(resolved) + thisCondition.getVariables(resolved)
    }

    override fun toString(): String {
        return "if $thisCondition {\n${
            trueExpr.toString().prependIndent()
        }\n} else {\n${
            falseExpr.toString().prependIndent()
        }\n}"
    }
}