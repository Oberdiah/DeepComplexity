package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

class BooleanExpression(
    val lhs: IExprRetBool,
    val rhs: IExprRetBool,
    val operation: BooleanOperation
) : IExprRetBool {
    override fun evaluate(condition: IExprRetBool): BooleanSet {
        val lhs = lhs.evaluate(condition)
        val rhs = rhs.evaluate(condition)

        return lhs.booleanOperation(rhs, operation)
    }

    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return lhs.getVariables(resolved) + rhs.getVariables(resolved)
    }

    override fun toString(): String {
        if (lhs == ConstantExpression.TRUE) {
            return when (operation) {
                BooleanOperation.AND -> rhs.toString()
                BooleanOperation.OR -> "TRUE"
            }
        } else if (lhs == ConstantExpression.FALSE) {
            return when (operation) {
                BooleanOperation.AND -> "FALSE"
                BooleanOperation.OR -> rhs.toString()
            }
        } else if (rhs == ConstantExpression.TRUE) {
            return when (operation) {
                BooleanOperation.AND -> lhs.toString()
                BooleanOperation.OR -> "TRUE"
            }
        } else if (rhs == ConstantExpression.FALSE) {
            return when (operation) {
                BooleanOperation.AND -> "FALSE"
                BooleanOperation.OR -> lhs.toString()
            }
        }

        return "($lhs $operation $rhs)"
    }

    enum class BooleanOperation {
        AND,
        OR;

        override fun toString(): String {
            return when (this) {
                AND -> "&&"
                OR -> "||"
            }
        }

        companion object {
            fun fromJavaTokenType(tokenType: IElementType): BooleanOperation? {
                return when (tokenType) {
                    JavaTokenType.ANDAND -> AND
                    JavaTokenType.OROR -> OR
                    else -> null
                }
            }
        }
    }
}