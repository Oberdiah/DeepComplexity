package com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.BooleanBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.into

object ExprConstrain {
    /**
     * Returns a list of constraints.
     * Typically, it returns a single constraint, but if an OR is involved, it may return multiple
     * as each side of the OR is a separate constraint.
     */
    fun getConstraints(condition: Expr<Boolean>): List<Constraints> {
        return when (condition) {
            is BooleanExpression -> {
                val lhsConstrained = condition.lhs.getConstraints()
                val rhsConstrained = condition.rhs.getConstraints()

                val outputConstraints: MutableList<Constraints> = mutableListOf()
                when (condition.op) {
                    BooleanOp.OR -> {
                        outputConstraints.addAll(lhsConstrained)
                        outputConstraints.addAll(rhsConstrained)
                    }

                    BooleanOp.AND -> {
                        for (lhs in lhsConstrained) {
                            for (rhs in rhsConstrained) {
                                outputConstraints.add(lhs.and(rhs))
                            }
                        }
                    }
                }
                outputConstraints
            }

            is ComparisonExpression<*> -> {
                fun <Q : Number> extra(me: ComparisonExpression<Q>): List<Constraints> {
                    val lhsBundleSet = me.lhs.evaluate(ExprEvaluate.Scope())
                    val rhsBundleSet = me.rhs.evaluate(ExprEvaluate.Scope())

                    return lhsBundleSet.generateConstraintsFrom(
                        rhsBundleSet,
                        condition.comp
                    )
                }

                return extra(condition)
            }

            is ConstExpr -> {
                return condition.constSet.bundles.map {
                    when (it.variances.collapse(it.constraints).into()) {
                        // Unsure if this is correct
                        BooleanBundle.TRUE, BooleanBundle.BOTH -> Constraints.completelyUnconstrained()
                        BooleanBundle.FALSE, BooleanBundle.NEITHER -> Constraints.unreachable()
                    }
                }
            }

            is BooleanInvertExpression -> condition.expr.getConstraints().map { it.invert() }
            else -> TODO("Not implemented constraints for $condition")
        }
    }
}