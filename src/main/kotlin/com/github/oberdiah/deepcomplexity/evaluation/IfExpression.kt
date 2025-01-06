package com.github.oberdiah.deepcomplexity.evaluation

import kotlin.reflect.KClass

class IfExpression(
    val trueExpr: IExpr,
    val falseExpr: IExpr,
    val thisCondition: IExprRetBool,
) : IExpr {
    override fun toString(): String {
        return "if $thisCondition {\n${
            trueExpr.toString().prependIndent()
        }\n} else {\n${
            falseExpr.toString().prependIndent()
        }\n}"
    }
}