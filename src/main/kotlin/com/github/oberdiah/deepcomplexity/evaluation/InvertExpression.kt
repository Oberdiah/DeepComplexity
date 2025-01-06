package com.github.oberdiah.deepcomplexity.evaluation

class InvertExpression(val expr: IExprRetBool) : IExprRetBool {
    override fun toString(): String {
        return "!$expr"
    }
}