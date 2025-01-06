package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class UnionExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun toString(): String {
        return "($lhs ∪ $rhs)"
    }
}