package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class IntersectExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return lhs.getVariables(resolved) + rhs.getVariables(resolved)
    }

    override fun getSetClass(): KClass<*> {
        return lhs.getSetClass()
    }

    override fun evaluate(): IMoldableSet {
        return lhs.evaluate().intersect(rhs.evaluate())
    }

    override fun deepClone(): IExpr {
        return IntersectExpression(lhs.deepClone(), rhs.deepClone())
    }

    override fun toString(): String {
        return "($lhs ∩ $rhs)"
    }
}