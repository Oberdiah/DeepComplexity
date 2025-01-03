package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType
import kotlin.reflect.KClass

class ComparisonExpression(
    val lhs: ExprRetNum,
    val rhs: ExprRetNum,
    val comparison: ComparisonOperation
) : ExprRetBool {
    override fun getSetClass(): KClass<*> {
        return BooleanSet::class
    }

    override fun getUnresolved(): Set<UnresolvedExpression.Unresolved> {
        return lhs.getUnresolved() + rhs.getUnresolved()
    }

    override fun evaluate(): BooleanSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.comparisonOperation(rhs, comparison)
    }

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