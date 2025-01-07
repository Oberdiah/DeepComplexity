package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet

object ExprConstrain {
    /**
     * Returns null if the variable is not constrained by the condition,
     * or the constraints were too complex to be determined.
     */
    fun getConstraints(condition: IExprRetBool, variable: VariableExpression): IMoldableSet? {
        val varKey = variable.getKey()
        return when (condition) {
            is BooleanExpression -> {
                val lhsConstrained = condition.lhs.getConstraints(variable) ?: return null
                val rhsConstrained = condition.rhs.getConstraints(variable) ?: return null

                when (condition.op) {
                    AND -> lhsConstrained.intersect(rhsConstrained)
                    OR -> lhsConstrained.union(rhsConstrained)
                }
            }

            is ComparisonExpression -> ConstraintSolver.getVariableConstraints(condition, varKey)
            is ConstExprBool -> {
                when (condition.singleElementSet) {
                    BooleanSet.TRUE, BooleanSet.BOTH -> ConstantExpression.fullSetFromExpr(variable)
                    BooleanSet.FALSE, BooleanSet.NEITHER -> ConstantExpression.emptySetFromExpr(variable)
                }
            }

            is InvertExpression -> condition.expr.getConstraints(variable)?.invert()
            is VariableExpression.VariableBool -> TODO()
        }
    }
}