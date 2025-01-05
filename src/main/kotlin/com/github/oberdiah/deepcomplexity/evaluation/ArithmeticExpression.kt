package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

class ArithmeticExpression(
    val lhs: IExprRetNum,
    val rhs: IExprRetNum,
    val operation: BinaryNumberOperation
) : IExprRetNum {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return lhs.getVariables(resolved) + rhs.getVariables(resolved)
    }

    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.arithmeticOperation(rhs, operation)
    }

    override fun toString(): String {
        return "($lhs $operation $rhs)"
    }

    enum class BinaryNumberOperation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION;

        override fun toString(): String {
            return when (this) {
                ADDITION -> "+"
                SUBTRACTION -> "-"
                MULTIPLICATION -> "*"
                DIVISION -> "/"
            }
        }

        companion object {
            fun fromJavaTokenType(tokenType: IElementType): BinaryNumberOperation? {
                return when (tokenType) {
                    JavaTokenType.PLUSEQ -> ADDITION
                    JavaTokenType.MINUSEQ -> SUBTRACTION
                    JavaTokenType.ASTERISKEQ -> MULTIPLICATION
                    JavaTokenType.DIVEQ -> DIVISION
                    JavaTokenType.PLUS -> ADDITION
                    JavaTokenType.MINUS -> SUBTRACTION
                    JavaTokenType.ASTERISK -> MULTIPLICATION
                    JavaTokenType.DIV -> DIVISION
                    else -> null
                }
            }
        }
    }
}