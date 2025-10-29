package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.inverted

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
            is BooleanInvertExpr -> expr.expr
            is BooleanExpr -> {
                BooleanExpr.new(
                    expr.lhs.inverted(),
                    expr.rhs.inverted(),
                    when (expr.op) {
                        BooleanOp.AND -> BooleanOp.OR
                        BooleanOp.OR -> BooleanOp.AND
                    }
                )
            }

            is ComparisonExpr<*> -> ComparisonExpr.new(expr.lhs, expr.rhs, expr.comp.invert())
            is ConstExpr -> ConstExpr(!expr.value, expr.ind)
            is IfExpr -> IfExpr.new(
                expr.trueExpr.inverted(),
                expr.falseExpr.inverted(),
                expr.thisCondition
            )

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
            is BooleanExpr -> {
                val outputConstraints: MutableSet<Constraints> = mutableSetOf()
                when (condition.op) {
                    BooleanOp.OR -> {
                        val lhsConstrained = getConstraints(condition.lhs, scope)
                        // In the OR case the two clauses don't constrain each other.
                        val rhsConstrained = getConstraints(condition.rhs, scope)

                        outputConstraints.addAll(lhsConstrained)
                        outputConstraints.addAll(rhsConstrained)
                    }

                    BooleanOp.AND -> {
                        // In the AND case, they do.
                        val lhsConstrained = getConstraints(condition.lhs, scope)
                        val constrainedScope = scope.constrainWith(lhsConstrained)
                        val rhsConstrained = getConstraints(condition.rhs, constrainedScope)

                        for (lhs in lhsConstrained) {
                            for (rhs in rhsConstrained) {
                                outputConstraints.add(lhs.and(rhs))
                            }
                        }
                    }
                }
                outputConstraints
            }

            is ComparisonExpr<*> -> {
                fun <Q : Any> extra(me: ComparisonExpr<Q>): Set<Constraints> {
                    val lhsBundleSet = me.lhs.evaluate(scope)
                    val rhsBundleSet = me.rhs.evaluate(scope)

                    return lhsBundleSet.generateConstraintsFrom(
                        rhsBundleSet,
                        condition.comp
                    )
                }

                extra(condition)
            }

            is ConstExpr -> {
                setOf(
                    if (condition.value) {
                        // If the boolean is true, we constrain nothing; anything goes.
                        Constraints.completelyUnconstrained()
                    } else {
                        // If the boolean is false, nothing can pass.
                        Constraints.unreachable()
                    }
                )
            }

            is BooleanInvertExpr -> getConstraints(condition.expr.inverted(), scope)
            is IfExpr -> {
                val ifCondition = condition.thisCondition
                val invertedIf = BooleanInvertExpr(ifCondition)

                // The true and false expressions are also conditions in this context,
                // as this whole thing must be a condition.
                val trueCondition = condition.trueExpr
                val falseCondition = condition.falseExpr

                val convertedToBooleanExpr =
                    BooleanExpr.new(
                        BooleanExpr.new(ifCondition, trueCondition, BooleanOp.AND),
                        BooleanExpr.new(invertedIf, falseCondition, BooleanOp.AND),
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