package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType
import kotlin.reflect.KClass

class ArithmeticExpression(
    val lhs: IExprRetNum,
    val rhs: IExprRetNum,
    val operation: BinaryNumberOperation
) : IExprRetNum {
    override fun toString(): String {
        return "($lhs $operation $rhs)"
    }
}