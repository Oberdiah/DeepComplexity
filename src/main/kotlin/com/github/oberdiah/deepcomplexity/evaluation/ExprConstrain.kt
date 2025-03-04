package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet

object ExprConstrain {
    /**
     * Returns null if the variable is not constrained by the condition,
     * or the constraints were too complex to be determined.
     */
    fun <T : IMoldableSet<T>> getConstraints(condition: IExpr<BooleanSet>, variable: VariableExpression<T>): IExpr<T>? {
        return when (condition) {
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

            is ComparisonExpression<*> -> {
                assert(variable.getSetIndicator() is NumberSetIndicator<*, *>) {
                    "Variable must be a number set. This requires more thought if we've hit this."
                }

                // Safety: We've asserted above that the variable is a NumberSet.
                @Suppress("UNCHECKED_CAST")
                ConstraintSolver.getVariableConstraints(
                    condition,
                    variable as VariableExpression<out FullyTypedNumberSet<*, *>>
                )
                    ?.let { it.tryCastTo(variable.getSetIndicator())!! }
            }

            is ConstExpr -> {
                when (condition.singleElementSet) {
                    BooleanSet.TRUE, BooleanSet.BOTH -> ConstantExpression.fullExprFromExprAndKey(
                        variable,
                        // I'm pretty sure we shouldn't use the variable key here. This is the constraint
                        // on that variable.
                        Context.Key.EphemeralKey.new()
                    )

                    BooleanSet.FALSE, BooleanSet.NEITHER -> ConstantExpression.emptyExprFromExpr(variable)
                }
            }

            is BooleanInvertExpression -> condition.expr.getConstraints(variable)?.let { return InvertExpression(it) }
            else -> TODO("Not implemented constraints for $condition")
        }
    }
}