package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.inverted
import org.jetbrains.kotlin.analysis.utils.collections.mapToSet

object ExprConstrain {
    /**
     * A pile of constraints is a set of one or more constraints that may be in effect.
     * I.e. constraints joined by OR.
     * At least one of these constraints must be met.
     */
    data class ConstraintsOrPile(val pile: Set<Constraints>) {
        companion object {
            fun unconstrained(): ConstraintsOrPile = ConstraintsOrPile(setOf(Constraints.completelyUnconstrained()))
            fun unreachable(): ConstraintsOrPile = ConstraintsOrPile(setOf(Constraints.unreachable()))
        }

        init {
            require(pile.isNotEmpty()) {
                "ConstraintsOrPile must have at least one constraint."
            }
        }

        fun onlyConstraining(keys: Set<EvaluationKey>): ConstraintsOrPile {
            return ConstraintsOrPile(pile.mapToSet { it.onlyConstraining(keys) })
        }

        @Suppress("Unused")
        val unreachable: Boolean
            get() = pile.all { it.unreachable }

        fun and(other: ConstraintsOrPile): ConstraintsOrPile {
            val outputConstraints: MutableSet<Constraints> = mutableSetOf()

            require(pile.size < 10 && other.pile.size < 10) {
                "Combining constraints with size > 10: $pile , $other. This surely can't be efficient."
            }

            for (lhs in pile) {
                for (rhs in other.pile) {
                    outputConstraints.add(lhs.and(rhs))
                }
            }
            return ConstraintsOrPile(outputConstraints)
        }

        fun or(other: ConstraintsOrPile): ConstraintsOrPile {
            return ConstraintsOrPile(this.pile + other.pile)
        }
    }

    /**
     * Where the expression was previously returning true, it now returns false, and vice versa.
     */
    fun invert(expr: Expr<Boolean>, constraints: ConstraintsOrPile): Expr<Boolean> {
        return when (expr) {
            is BooleanInvertExpr -> expr.expr
            is BooleanExpr -> {
                BooleanExpr.new(
                    expr.lhs.inverted(constraints),
                    expr.rhs.inverted(constraints),
                    when (expr.op) {
                        BooleanOp.AND -> BooleanOp.OR
                        BooleanOp.OR -> BooleanOp.AND
                    }
                )
            }

            is ComparisonExpr<*> -> ComparisonExpr.new(expr.lhs, expr.rhs, expr.comp.invert())
            is ConstExpr -> ConstExpr.new(!expr.value, expr.ind)
            is IfExpr -> IfExpr.new(
                expr.trueExpr.inverted(constraints),
                expr.falseExpr.inverted(constraints),
                expr.thisCondition
            )

            else -> TODO("Not implemented for $expr")
        }
    }

    /**
     * Returns a list of constraints.
     * Typically, it returns a single constraint, but if an OR is involved, it may return multiple
     * as each side of the OR is a separate constraint.
     *
     * This is the only place we should generate constraints. They then get applied to constants when traversing
     * evaluation, and those constants then cling onto bundles and go for a ride.
     */
    fun getConstraints(
        condition: Expr<Boolean>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): ConstraintsOrPile {
        val startTime = System.currentTimeMillis()
        val newConstraints = when (condition) {
            is BooleanExpr -> {
                when (condition.op) {
                    BooleanOp.OR -> {
                        val lhsConstrained = getConstraints(condition.lhs, constraints, assistant)
                        // In the OR case the two clauses don't constrain each other.
                        val rhsConstrained = getConstraints(condition.rhs, constraints, assistant)

                        lhsConstrained.or(rhsConstrained)
                    }

                    BooleanOp.AND -> {
                        // In the AND case, they do.
                        val lhsConstrained = getConstraints(condition.lhs, constraints, assistant)
                        val rhsConstrained = getConstraints(condition.rhs, constraints.and(lhsConstrained), assistant)

                        lhsConstrained.and(rhsConstrained)
                    }
                }
            }

            is ComparisonExpr<*> -> {
                fun <Q : Any> extra(me: ComparisonExpr<Q>): ConstraintsOrPile {
                    // At the moment we don't want these traces to appear in the debug view.
                    // In future if you do, we'll just need to be sure to create a new tracer.falseConstraints()
                    // and tracer.trueConstraints() and use those when calling getConstraints from
                    // the `evaluate` section to keep these separate from the standard evaluations. Currently
                    // there's nowhere to display that information even if we had it, so we don't bother.
                    val lhsBundleSet = me.lhs.evaluate(constraints, assistant)
                    val rhsBundleSet = me.rhs.evaluate(constraints, assistant)

                    return lhsBundleSet.generateConstraintsFrom(
                        rhsBundleSet,
                        condition.comp
                    )
                }

                extra(condition)
            }

            is ConstExpr -> {
                if (condition.value) {
                    // If the boolean is true, we constrain nothing; anything goes.
                    ConstraintsOrPile.unconstrained()
                } else {
                    // If the boolean is false, nothing can pass.
                    ConstraintsOrPile.unreachable()
                }
            }

            is BooleanInvertExpr -> {
                // You might think that this is a bit silly, and we should just invert the produced constraints
                // instead, but there's no easy way to invert constraints due to the uninvertability
                // of sets.
                getConstraints(condition.expr.inverted(constraints), constraints, assistant)
            }

            is IfExpr -> {
                val ifCondition = condition.thisCondition
                val invertedIf = BooleanInvertExpr.new(ifCondition)

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

                getConstraints(convertedToBooleanExpr, constraints, assistant)
            }

            else -> TODO("Not implemented constraints for $condition")
        }
        val endTime = System.currentTimeMillis()
        if (endTime - startTime > 10) {
            println("Warning: getConstraints took ${endTime - startTime}ms")
        }

        return newConstraints
    }
}