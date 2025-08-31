package com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.inverted
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.into

object ExprConstrain {
    fun combineConstraints(a: Set<Constraints>, b: Set<Constraints>): Set<Constraints> {
        val outputConstraints: MutableSet<Constraints> = mutableSetOf()
        if (a.size > 10 || b.size > 10) {
            println("Warning: Combining constraints with size > 10: $a, $b")
        }
        for (lhs in a) {
            for (rhs in b) {
                outputConstraints.add(lhs.and(rhs))
            }
        }
        return outputConstraints
    }

    /**
     * Where the expression was previously returning true, it now returns false, and vice versa.
     */
    fun invert(expr: Expr<Boolean>): Expr<Boolean> {
        return when (expr) {
            is BooleanInvertExpression -> expr.expr
            is BooleanExpression -> {
                BooleanExpression(
                    expr.lhs.inverted(),
                    expr.rhs.inverted(),
                    when (expr.op) {
                        BooleanOp.AND -> BooleanOp.OR
                        BooleanOp.OR -> BooleanOp.AND
                    }
                )
            }

            is ComparisonExpression<*> -> {
                fun <Q : Any> extra(me: ComparisonExpression<Q>): Expr<Boolean> =
                    ComparisonExpression(me.lhs, me.rhs, me.comp.invert())
                extra(expr)
            }

            is ConstExpr -> ConstExpr(expr.constSet.invert())
            is IfExpression -> {
                IfExpression(
                    expr.thisCondition,
                    expr.falseExpr.inverted(),
                    expr.trueExpr.inverted()
                )
            }

            else -> TODO("Not implemented for $expr")
        }
    }

    /**
     * Returns a list of constraints.
     * Typically, it returns a single constraint, but if an OR is involved, it may return multiple
     * as each side of the OR is a separate constraint.
     */
    fun getConstraints(condition: Expr<Boolean>, scope: ExprEvaluate.Scope): Set<Constraints> {
        val startTime = System.currentTimeMillis()
        val constraints = when (condition) {
            is BooleanExpression -> {
                val lhsConstrained = getConstraints(condition.lhs, scope)
                val constrainedScope = scope.constrainWith(lhsConstrained)
                val rhsConstrained = getConstraints(condition.rhs, constrainedScope)

                val outputConstraints: MutableSet<Constraints> = mutableSetOf()
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
                fun <Q : Any> extra(me: ComparisonExpression<Q>): Set<Constraints> {
                    val lhsBundleSet = me.lhs.evaluate(scope)
                    val rhsBundleSet = me.rhs.evaluate(scope)

                    return lhsBundleSet.generateConstraintsFrom(
                        rhsBundleSet,
                        condition.comp
                    )
                }

                return extra(condition)
            }

            is ConstExpr -> {
                return condition.constSet.variances.map {
                    when (it.variances.collapse(it.constraints).into()) {
                        // Unsure if this is correct
                        BooleanSet.TRUE, BooleanSet.BOTH -> Constraints.completelyUnconstrained()
                        BooleanSet.FALSE, BooleanSet.NEITHER -> Constraints.unreachable()
                    }
                }.toSet()
            }

            is BooleanInvertExpression -> getConstraints(condition.expr.inverted(), scope)
            is IfExpression -> {
                val ifCondition = condition.thisCondition
                val invertedIf = BooleanInvertExpression(ifCondition)

                // The true and false expressions are also conditions in this context,
                // as this whole thing must be a condition.
                val trueCondition = condition.trueExpr
                val falseCondition = condition.falseExpr

                val convertedToBooleanExpr =
                    BooleanExpression(
                        BooleanExpression(ifCondition, trueCondition, BooleanOp.AND),
                        BooleanExpression(invertedIf, falseCondition, BooleanOp.AND),
                        BooleanOp.OR
                    )

                getConstraints(convertedToBooleanExpr, scope)
            }

            else -> TODO("Not implemented constraints for $condition")
        }
        val endTime = System.currentTimeMillis()
        if (endTime - startTime > 10) {
            println("Warning: getConstraints took ${endTime - startTime}ms")
        }

        return constraints
    }
}