package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.PsiElement

interface Expression<T : MoldableSet<*>> {
    fun evaluate(): T
}

enum class BinaryNumberOperation {
    ADDITION,
    MULTIPLICATION,
}

enum class ComparisonOperation {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
}

class BinaryNumberExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val operation: BinaryNumberOperation
) : Expression<NumberSet> {
    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.binaryOperation(rhs, operation)
    }
}

// Element is either PsiLocalVariable, PsiParameter, or PsiField
class IncomingVariable(val element: PsiElement) : Expression<MoldableSet<Any>> {
    override fun evaluate(): MoldableSet<Any> {
        throw NotImplementedError()
    }
}

class ComparisonExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val comparison: ComparisonOperation
) : Expression<MoldableSet<Boolean>> {
    override fun evaluate(): MoldableSet<Boolean> {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.comparisonOperation(rhs, comparison)
    }
}