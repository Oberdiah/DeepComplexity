package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet

class InvertExpression(val expr: IExprRetBool) : IExprRetBool {
    override fun evaluate(condition: IExprRetBool): BooleanSet {
        return expr.evaluate(condition).invert() as BooleanSet
    }

    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return expr.getVariables(resolved)
    }

    override fun toString(): String {
        return "!$expr"
    }
}