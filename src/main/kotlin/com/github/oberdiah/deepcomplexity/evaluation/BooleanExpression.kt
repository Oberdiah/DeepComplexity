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

    override fun getConstraints(): Map<VariableExpression, IExpr> {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
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