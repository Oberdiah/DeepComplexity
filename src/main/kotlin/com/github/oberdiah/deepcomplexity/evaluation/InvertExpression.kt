package com.github.oberdiah.deepcomplexity.evaluation

class InvertExpression(val expr: IExprRetBool) : IExprRetBool {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return expr.getVariables(resolved)
    }

    override fun toString(): String {
        return "!$expr"
    }
}