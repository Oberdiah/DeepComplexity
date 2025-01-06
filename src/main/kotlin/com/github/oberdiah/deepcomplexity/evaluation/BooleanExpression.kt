package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanExpression.BooleanOperation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

class BooleanExpression(
    val lhs: IExprRetBool,
    val rhs: IExprRetBool,
    val operation: BooleanOperation
) : IExprRetBool {
//    override fun constrain(varKey: VariableExpression.VariableKey, set: IMoldableSet): IMoldableSet {
//        val lhsConstrained = lhs.constrain(varKey, set)
//        val rhsConstrained = rhs.constrain(varKey, set)
//
//        return when (operation) {
//            AND -> lhsConstrained.intersect(rhsConstrained)
//            OR -> lhsConstrained.union(rhsConstrained)
//        }
//    }

    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return lhs.getVariables(resolved) + rhs.getVariables(resolved)
    }

    override fun toString(): String {
        if (lhs == ConstantExpression.TRUE) {
            return when (operation) {
                AND -> rhs.toString()
                OR -> "TRUE"
            }
        } else if (lhs == ConstantExpression.FALSE) {
            return when (operation) {
                AND -> "FALSE"
                OR -> rhs.toString()
            }
        } else if (rhs == ConstantExpression.TRUE) {
            return when (operation) {
                AND -> lhs.toString()
                OR -> "TRUE"
            }
        } else if (rhs == ConstantExpression.FALSE) {
            return when (operation) {
                AND -> "FALSE"
                OR -> lhs.toString()
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