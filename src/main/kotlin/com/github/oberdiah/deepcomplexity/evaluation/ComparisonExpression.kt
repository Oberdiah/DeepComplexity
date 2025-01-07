package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

class ComparisonExpression(
    val lhs: IExprRetNum,
    val rhs: IExprRetNum,
    val comparison: ComparisonOperation
) : IExprRetBool {
    override fun toString(): String {
        return "($lhs $comparison $rhs)"
    }
}