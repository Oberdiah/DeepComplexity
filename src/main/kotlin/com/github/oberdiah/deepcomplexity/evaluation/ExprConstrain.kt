package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet

object ExprConstrain {
    /**
     * Returns a list of constraints.
     * Typically, returns a single constraint, but if an OR is involved, may return multiple
     * as each side of the OR is a separate constraint.
     */
    fun getConstraints(condition: IExpr<BooleanSet>): List<Constraints> {
        return when (condition) {
            is BooleanExpression -> {
                val lhsConstrained = condition.lhs.getConstraints()
                val rhsConstrained = condition.rhs.getConstraints()

                val outputConstraints: MutableList<Constraints> = mutableListOf()
                // oo yeah, O(n^2)!
                for (lhs in lhsConstrained) {
                    for (rhs in rhsConstrained) {
                        when (condition.op) {
                            AND -> {
                                outputConstraints.add(lhs.and(rhs))
                            }

                            OR -> {
                                outputConstraints.add(lhs)
                                outputConstraints.add(rhs)
                            }
                        }
                    }
                }
                outputConstraints
            }

            is ComparisonExpression<*> -> listOf(
                ConstraintSolver.getConstraints(condition)
            )

            is ConstExpr -> {
                return when (condition.singleElementSet) {
                    BooleanSet.TRUE, BooleanSet.BOTH -> listOf(Constraints.completelyUnconstrained())
                    BooleanSet.FALSE, BooleanSet.NEITHER -> listOf(Constraints.unreachable())
                }
            }

            is BooleanInvertExpression -> condition.expr.getConstraints().map { it.invert() }
            else -> TODO("Not implemented constraints for $condition")
        }
    }
}