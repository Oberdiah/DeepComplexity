package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class UnionExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return lhs.getVariables(resolved) + rhs.getVariables(resolved)
    }

    override fun getBaseClass(): KClass<*> {
        return lhs.getBaseClass()
    }

    override fun toString(): String {
        return "($lhs ∪ $rhs)"
    }
}