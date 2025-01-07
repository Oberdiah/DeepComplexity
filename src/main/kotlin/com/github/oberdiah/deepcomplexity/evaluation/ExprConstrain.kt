package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOperation.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOperation.OR
import com.github.oberdiah.deepcomplexity.evaluation.VariableExpression.VariableKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ExprConstrain {
    // One day this should be expanded to take IMoldableSet instead.
    fun constrain(expr: IExprRetBool, varKey: VariableKey, set: NumberSet): NumberSet {
        return when (expr) {
            is BooleanExpression -> {
                val lhsConstrained = constrain(expr.lhs, varKey, set)
                val rhsConstrained = constrain(expr.rhs, varKey, set)

                when (expr.operation) {
                    AND -> lhsConstrained.intersect(rhsConstrained)
                    OR -> lhsConstrained.union(rhsConstrained)
                } as NumberSet
            }

            is ComparisonExpression -> set // A lot of heavy lifting here
            is ConstantExpression.ConstExprBool -> {
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