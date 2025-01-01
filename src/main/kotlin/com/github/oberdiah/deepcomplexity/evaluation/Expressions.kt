package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiParameter

sealed interface Expression<out T : MoldableSet> {
    fun evaluate(): T
}

enum class BinaryNumberOperation {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION,
}

enum class ComparisonOperation {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
}

class ArithmeticExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val operation: BinaryNumberOperation
) : Expression<NumberSet> {
    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.arithmeticOperation(rhs, operation)
    }
}

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we don't yet know the value of, but would
// if we stepped out far enough.
class UnresolvedVariable(val element: PsiElement) : Expression<MoldableSet> {
    init {
        if (!(element is PsiLocalVariable || element is PsiParameter || element is PsiField)) {
            throw IllegalArgumentException("Element must be a PsiLocalVariable, PsiParameter, or PsiFiel0d (got ${element::class})")
        }
    }

    override fun evaluate(): MoldableSet {
        throw NotImplementedError()
    }
}

class ComparisonExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val comparison: ComparisonOperation
) : Expression<BooleanSet> {
    override fun evaluate(): BooleanSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.comparisonOperation(rhs, comparison)
    }
}