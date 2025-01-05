package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class IntersectExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun getCurrentlyUnresolved(): Set<VariableExpression> {
        return lhs.getCurrentlyUnresolved() + rhs.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return lhs.getSetClass()
    }

    override fun evaluate(): IMoldableSet {
        return lhs.evaluate().intersect(rhs.evaluate())
    }

    override fun toString(): String {
        return "($lhs ∩ $rhs)"
    }
}