package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.inverted
import org.jetbrains.kotlin.preloading.ProfilingInstrumenterExample.a
import org.jetbrains.kotlin.preloading.ProfilingInstrumenterExample.b

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

        @Suppress("Unused")
        val unreachable: Boolean
            get() = pile.all { it.unreachable }

        fun and(other: ConstraintsOrPile): ConstraintsOrPile {
            val outputConstraints: MutableSet<Constraints> = mutableSetOf()

            if (pile.size > 10 || other.pile.size > 10) {
                println("Warning: Combining constraints with size > 10: $a, $b")
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
    fun invert(expr: Expr<Boolean>, scope: ExprEvaluate.Scope): Expr<Boolean> {
        return when (expr) {
            is BooleanInvertExpr -> expr.expr
            is BooleanExpr -> {
                BooleanExpr.new(
                    expr.lhs.inverted(scope),
                    expr.rhs.inverted(scope),
                    when (expr.op) {
                        BooleanOp.AND -> BooleanOp.OR
                        BooleanOp.OR -> BooleanOp.AND
                    }
                )
            }

            is ComparisonExpr<*> -> ComparisonExpr.new(expr.lhs, expr.rhs, expr.comp.invert())
            is ConstExpr -> ConstExpr.new(!expr.value, expr.ind)
            is IfExpr -> IfExpr.new(
                expr.trueExpr.inverted(scope),
                expr.falseExpr.inverted(scope),
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
    fun getConstraints(condition: Expr<Boolean>, scope: ExprEvaluate.Scope): ConstraintsOrPile {
        val startTime = System.currentTimeMillis()
        val constraints = when (condition) {
            is BooleanExpr -> {
                when (condition.op) {
                    BooleanOp.OR -> {
                        val lhsConstrained = getConstraints(condition.lhs, scope)
                        // In the OR case the two clauses don't constrain each other.
                        val rhsConstrained = getConstraints(condition.rhs, scope)

                        lhsConstrained.or(rhsConstrained)
                    }

                    BooleanOp.AND -> {
                        // In the AND case, they do.
                        val lhsConstrained = getConstraints(condition.lhs, scope)
                        val constrainedScope = scope.constrainWith(lhsConstrained)
                        val rhsConstrained = getConstraints(condition.rhs, constrainedScope)

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
                    val lhsBundleSet = me.lhs.evaluate(scope, Tracer())
                    val rhsBundleSet = me.rhs.evaluate(scope, Tracer())

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
                getConstraints(condition.expr.inverted(scope), scope)
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