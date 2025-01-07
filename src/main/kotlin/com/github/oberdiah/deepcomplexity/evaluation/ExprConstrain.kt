package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet

object ExprConstrain {
    fun getConstraints(condition: IExprRetBool, variable: VariableExpression): IMoldableSet {
        val varKey = variable.getKey()
        return when (condition) {
            is BooleanExpression -> {
                val lhsConstrained = condition.lhs.getConstraints(variable)
                val rhsConstrained = condition.rhs.getConstraints(variable)

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

            is InvertExpression -> condition.expr.getConstraints(variable).invert()
            is VariableExpression.VariableBool -> TODO()
        }
    }
}