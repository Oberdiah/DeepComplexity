package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

class ConstraintExpression(
    val exprToConstrain: Expr,
    val constraints: Expr
) : Expr {
    override fun evaluate(): MoldableSet {
        val expr = exprToConstrain.evaluate()
        val constraint = constraints.evaluate()

        return expr.intersect(constraint)
    }

    override fun getCurrentlyUnresolved(): Set<VariableExpression> {
        return exprToConstrain.getCurrentlyUnresolved() + constraints.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return exprToConstrain.getSetClass()
    }
}