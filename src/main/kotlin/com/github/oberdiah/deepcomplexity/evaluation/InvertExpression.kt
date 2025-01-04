package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet

class InvertExpression(val expr: ExprRetBool) : ExprRetBool {
    override fun evaluate(): BooleanSet {
        return expr.evaluate().invert() as BooleanSet
    }

    override fun getConstraints(): Map<VariableExpression, Expr> {
        return expr.getConstraints().mapValues { (_, expr) -> InvertExpression(expr as ExprRetBool) }
    }

    override fun getCurrentlyUnresolved(): Set<VariableExpression> {
        return expr.getCurrentlyUnresolved()
    }
}