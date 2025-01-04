package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression.BinaryNumberOperation.*
import com.github.oberdiah.deepcomplexity.evaluation.UnresolvedExpression.Unresolved
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType
import kotlin.reflect.KClass

class ArithmeticExpression(
    val lhs: ExprRetNum,
    val rhs: ExprRetNum,
    val operation: BinaryNumberOperation
) : ExprRetNum {
    companion object {
        fun repeatExpression(expr: ArithmeticExpression, times: ExprRetNum): ArithmeticExpression? {
            val lhsIsUnresolved = expr.lhs is Unresolved
            val rhsIsUnresolved = expr.rhs is Unresolved

            if (!lhsIsUnresolved && !rhsIsUnresolved) {
                // We can only deal with surface-level unresolved expressions
                return null
            }
            if (lhsIsUnresolved && rhsIsUnresolved) {
                // This is the caller's fault
                throw IllegalArgumentException("Both sides of the expression are unresolved, which shouldn't happen.")
            }

            val resolvedSide = if (lhsIsUnresolved) expr.rhs else expr.lhs
            val unresolvedSide = if (lhsIsUnresolved) expr.lhs else expr.rhs

            return when (expr.operation) {
                ADDITION, SUBTRACTION -> ArithmeticExpression(
                    unresolvedSide,
                    ArithmeticExpression(
                        resolvedSide,
                        times,
                        MULTIPLICATION
                    ),
                    expr.operation
                )

                else -> TODO("These could be implemented with some sort of pow(), but I've not bothered for now.")
            }
        }
    }

    override fun getCurrentlyUnresolved(): Set<Unresolved> {
        return lhs.getCurrentlyUnresolved() + rhs.getCurrentlyUnresolved()
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