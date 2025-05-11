package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp.OR
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.into

object ExprConstrain {
    /**
     * Returns a list of constraints.
     * Typically, it returns a single constraint, but if an OR is involved, it may return multiple
     * as each side of the OR is a separate constraint.
     */
    fun getConstraints(condition: IExpr<Boolean>): List<Constraints> {
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
                return condition.constSet.bundles.map {
                    when (it.bundle.collapse(it.constraints).into()) {
                        // Unsure if this is correct
                        BooleanSet.TRUE, BooleanSet.BOTH -> Constraints.completelyUnconstrained()
                        BooleanSet.FALSE, BooleanSet.NEITHER -> Constraints.unreachable()
                    }
                }
            }

            is BooleanInvertExpression -> condition.expr.getConstraints().map { it.invert() }
            else -> TODO("Not implemented constraints for $condition")
        }
    }
}