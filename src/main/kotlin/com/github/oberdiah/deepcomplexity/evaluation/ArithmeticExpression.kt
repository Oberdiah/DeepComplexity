package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

class ArithmeticExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val operation: BinaryNumberOperation
) : Expression<NumberSet>(NumberSet::class) {
    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.arithmeticOperation(rhs, operation)
    }

    enum class BinaryNumberOperation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION;

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