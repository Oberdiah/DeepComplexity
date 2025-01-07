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

    enum class ComparisonOperation {
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL;

        override fun toString(): String {
            return when (this) {
                LESS_THAN -> "<"
                LESS_THAN_OR_EQUAL -> "<="
                GREATER_THAN -> ">"
                GREATER_THAN_OR_EQUAL -> ">="
            }
        }

        companion object {
            fun fromJavaTokenType(tokenType: IElementType): ComparisonOperation? {
                return when (tokenType) {
                    JavaTokenType.LT -> LESS_THAN
                    JavaTokenType.LE -> LESS_THAN_OR_EQUAL
                    JavaTokenType.GT -> GREATER_THAN
                    JavaTokenType.GE -> GREATER_THAN_OR_EQUAL
                    else -> null
                }
            }
        }
    }
}