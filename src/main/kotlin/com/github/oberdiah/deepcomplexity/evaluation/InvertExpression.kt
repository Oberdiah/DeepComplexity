package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet

class InvertExpression(val expr: IExprRetBool) : IExprRetBool {
    override fun evaluate(): BooleanSet {
        return expr.evaluate().invert() as BooleanSet
    }

    override fun getConstraints(): Map<VariableExpression, IExpr> {
        return expr.getConstraints().mapValues { (_, expr) -> InvertExpression(expr as IExprRetBool) }
    }

    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return expr.getVariables(resolved)
    }

    override fun toString(): String {
        return "!$expr"
    }
}