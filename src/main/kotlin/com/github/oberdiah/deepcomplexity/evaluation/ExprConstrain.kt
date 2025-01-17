package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet

object ExprConstrain {
    /**
     * Returns null if the variable is not constrained by the condition,
     * or the constraints were too complex to be determined.
     */
    fun getConstraints(condition: IExprRetBool, variable: VariableExpression): IExpr? {
        val varKey = variable.getKey()
        return when (condition) {
            is DynamicBooleanCastExpression -> (condition.expr as? IExprRetBool
                ?: throw IllegalStateException("Tried to cast ${condition.expr} to a boolean set but failed."))
                .getConstraints(variable)

            is BooleanExpression -> {
                val lhsConstrained = condition.lhs.getConstraints(variable)
                val rhsConstrained = condition.rhs.getConstraints(variable)

                if (lhsConstrained == null && rhsConstrained == null) {
                    return null
                } else if (lhsConstrained == null) {
                    return rhsConstrained
                } else if (rhsConstrained == null) {
                    return lhsConstrained
                }

                when (condition.op) {
                    AND -> IntersectExpression(lhsConstrained, rhsConstrained)
                    OR -> UnionExpression(lhsConstrained, rhsConstrained)
                }
            }

            is ComparisonExpression -> ConstraintSolver.getVariableConstraints(condition, varKey)

            is ConstExprBool -> {
                when (condition.singleElementSet) {
                    BooleanSet.TRUE, BooleanSet.BOTH -> ConstantExpression.fullExprFromExpr(variable)
                    BooleanSet.FALSE, BooleanSet.NEITHER -> ConstantExpression.emptyExprFromExpr(variable)
                }
            }

            is BooleanInvertExpression -> condition.expr.getConstraints(variable)?.let { return InvertExpression(it) }

            is VariableExpression.VariableBool -> TODO()
        }
    }
}