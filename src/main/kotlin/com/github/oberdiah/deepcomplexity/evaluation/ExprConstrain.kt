package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.evaluation.VariableExpression.VariableKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ExprConstrain {
    // One day this should be expanded to take IMoldableSet instead.
    fun <T : IMoldableSet> constrain(expr: IExprRetBool, varKey: VariableKey, set: T): T {
        @Suppress("UNCHECKED_CAST")
        return when (set) {
            is NumberSet -> constrainNumber(expr, varKey, set) as T
            else -> throw IllegalArgumentException("Only NumberSet is supported for now.")
        }
    }

    private fun constrainNumber(expr: IExprRetBool, varKey: VariableKey, set: NumberSet): NumberSet {
        return when (expr) {
            is BooleanExpression -> {
                val lhsConstrained = expr.lhs.constrain(varKey, set)
                val rhsConstrained = expr.rhs.constrain(varKey, set)

                when (expr.op) {
                    AND -> lhsConstrained.intersect(rhsConstrained)
                    OR -> lhsConstrained.union(rhsConstrained)
                } as NumberSet
            }

            is ComparisonExpression -> set // A lot of heavy lifting here
            is ConstExprBool -> {
                when (expr.singleElementSet) {
                    BooleanSet.TRUE, BooleanSet.BOTH -> set
                    BooleanSet.FALSE, BooleanSet.NEITHER -> NumberSet.empty(set.getClass())
                }
            }
            // Maybe? invert() isn't yet fully implemented.
            is InvertExpression -> set //constrain(expr.expr, varKey, set.invert() as NumberSet).invert() as NumberSet
            is VariableExpression.VariableBool -> TODO()
        }
    }
}