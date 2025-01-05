package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class UnionExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return lhs.getVariables(resolved) + rhs.getVariables(resolved)
    }

    override fun getSetClass(): KClass<*> {
        return lhs.getSetClass()
    }

    override fun evaluate(): IMoldableSet {
        return lhs.evaluate().union(rhs.evaluate())
    }

    override fun toString(): String {
        return "($lhs ∪ $rhs)"
    }

    override fun deepClone(): IExpr {
        return UnionExpression(lhs.deepClone(), rhs.deepClone())
    }
}