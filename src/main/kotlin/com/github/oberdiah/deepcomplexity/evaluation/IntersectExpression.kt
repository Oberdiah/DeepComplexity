package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

class IntersectExpression(val lhs: Expr, val rhs: Expr) : Expr {
    override fun getCurrentlyUnresolved(): Set<VariableExpression> {
        return lhs.getCurrentlyUnresolved() + rhs.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return lhs.getSetClass()
    }

    override fun evaluate(): MoldableSet {
        return lhs.evaluate().intersect(rhs.evaluate())
    }

    override fun toString(): String {
        return "($lhs ∩ $rhs)"
    }
}