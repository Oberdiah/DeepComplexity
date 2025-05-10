package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.BundleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.into

object ConstraintSolver {
    data class EvaluatedCollectedTerms<T : Number>(
        val terms: Map<Int, BundleSet<T>>,
    )

    data class CollectedTerms<T : Number>(
        val setIndicator: SetIndicator<T>,
        /**
         * Map from exponent to coefficient. 0th is constant, 1st is linear, 2nd is quadratic, etc.
         */
        val terms: MutableMap<Int, IExpr<T>> = mutableMapOf(),
    ) {
        fun evaluate(condition: IExpr<Boolean>): EvaluatedCollectedTerms<T> {
            return EvaluatedCollectedTerms(
                terms.mapValues { (_, term) -> term.evaluate(condition) }
            )
        }

        override fun toString(): String {
            if (terms.entries.isEmpty()) {
                return "0"
            }

            return terms.entries.joinToString(" + ") { (exp, term) ->
                when (exp) {
                    0 -> term.toString()
                    1 -> "${term}x"
                    else -> "${term}x^$exp"
                }
            }
        }

        fun <Q : Number> castTo(setIndicator: SetIndicator<Q>): CollectedTerms<Q> =
            CollectedTerms(
                setIndicator,
                // I'm suspicious of this. It feels like the most obvious thing to do
                // but I'm not 100% sure it's actually fine.
                terms.mapValues { (_, term) -> term.performACastTo(setIndicator, true) }.toMutableMap()
            )

        fun negate(): CollectedTerms<T> {
            val newTerms = mutableMapOf<Int, IExpr<T>>()

            for ((exp, term) in terms) {
                newTerms[exp] = NegateExpression(term)
            }

            return CollectedTerms(setIndicator, newTerms)
        }

        fun combine(other: CollectedTerms<T>, op: BinaryNumberOp): CollectedTerms<T> {
            return when (op) {
                ADDITION, SUBTRACTION -> {
                    // Combine terms
                    val newTerms = mutableMapOf<Int, IExpr<T>>()
                    newTerms.putAll(terms)

                    for ((exp, term) in other.terms) {
                        newTerms[exp] = merge(newTerms[exp], term, op)
                    }

                    CollectedTerms(setIndicator, newTerms)
                }

                MULTIPLICATION -> {
                    // Multiply terms together
                    val newTerms = mutableMapOf<Int, IExpr<T>>()

                    for ((exp1, term1) in terms) {
                        for ((exp2, term2) in other.terms) {
                            // Add exponents and multiply coefficients
                            val newExp = exp1 + exp2
                            val newTerm = ArithmeticExpression(term1, term2, MULTIPLICATION)
                            newTerms[newExp] = merge(newTerms[newExp], newTerm, ADDITION)
                        }
                    }
                    CollectedTerms(setIndicator, newTerms)
                }

                DIVISION -> {
                    val newTerms = mutableMapOf<Int, IExpr<T>>()

                    for ((exp1, term1) in terms) {
                        for ((exp2, term2) in other.terms) {
                            // Subtract exponents and divide coefficients
                            val newExp = exp1 - exp2
                            val newTerm = ArithmeticExpression(term1, term2, DIVISION)
                            newTerms[newExp] = merge(newTerms[newExp], newTerm, ADDITION)
                        }
                    }
                    CollectedTerms(setIndicator, newTerms)
                }
            }
        }
    }

    private fun <T : Number> merge(lhs: IExpr<T>?, rhs: IExpr<T>, op: BinaryNumberOp): IExpr<T> {
        return if (lhs == null) {
            ArithmeticExpression(ConstantExpression.zero(rhs), rhs, op)
        } else {
            ArithmeticExpression(lhs, rhs, op)
        }
    }

    /**
     * It is important this method is confident in its values — these can be inverted down the line,
     * so returning the full set is essentially saying the other branch is never taken.
     *
     * Does not return a given constraint if it could not be ascertained accurately.
     */
    fun getConstraints(
        expr: ComparisonExpression<*>,
    ): Constraints {
        var constraints = Constraints.completelyUnconstrained()

        val variables = expr.getVariables(false)
        for (variable in variables) {
            assert(variable.getSetIndicator() is NumberSetIndicator<*>) {
                "All variables must be number sets. This requires more thought if we've hit this."
            }

            // Safety: We know the variable is a number set.
            @Suppress("UNCHECKED_CAST")
            val castVariable = variable as VariableExpression<out Number>

            val variableConstraints = getVariableConstraints(expr, castVariable) ?: continue
            constraints = constraints.addConstraint(variable.getKey().key, variableConstraints)
        }

        return constraints
    }

    /**
     * Does not return a given constraint if it could not be ascertained accurately.
     */
    private fun <T : Number> getVariableConstraints(
        expr: ComparisonExpression<*>,
        variable: VariableExpression<T>,
    ): Bundle<T>? {
        if (expr.getVariables(false).none { it.getKey() == variable.getKey() }) {
            // The variable wasn't found in the expression.
            return null
        }

        val (lhs, constant) = normalizeComparisonExpression(expr, variable) ?: return null

        if (lhs.terms.isEmpty()) {
            // The variable wasn't found in the expression, but we should have caught this earlier.
            throw IllegalStateException("Variable $variable was not found in the expression $lhs")
        }

        return if (lhs.terms.size == 1) {
            // This is good, we can solve this now.
            val (exponent, coefficient) = lhs.terms.entries.first()

            val coeffLZ = ComparisonExpression(
                coefficient,
                ConstantExpression.zero(variable.getNumberSetIndicator()),
                ComparisonOp.LESS_THAN
            )

            val rhs = ArithmeticExpression(constant, coefficient, DIVISION)
            return if (exponent == 1) {
                val rhsBundle = rhs.evaluate(ConstantExpression.TRUE).collapse().into()
                val shouldFlip = coeffLZ.evaluate(ConstantExpression.TRUE).collapse().into()

                when (shouldFlip) {
                    BooleanSet.TRUE -> rhsBundle.getSetSatisfying(expr.comp.flip())
                    BooleanSet.FALSE -> rhsBundle.getSetSatisfying(expr.comp)
                    BooleanSet.BOTH -> rhsBundle.getSetSatisfying(expr.comp)
                        .union(rhsBundle.getSetSatisfying(expr.comp.flip()))

                    BooleanSet.NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                }
            } else {
                println("Cannot constraint solve yet: $lhs (exponent $exponent)")
                null
            }
        } else {
            // We might be able to solve this in future, for now we'll not bother.
            println("Cannot constraint solve yet as it has zero terms: $lhs")
            null
        }
    }

    /**
     * Normalizes the comparison expression to a form where the left hand side is a linear equation and the right hand
     * side is a constant.
     */
    private fun <T : Number> normalizeComparisonExpression(
        expr: ComparisonExpression<*>,
        variable: VariableExpression<T>,
    ): Pair<CollectedTerms<T>, IExpr<T>>? {
        // Collect terms from both sides
        val leftTerms = expandTerms(expr.lhs, variable)
        val rightTerms = expandTerms(expr.rhs, variable)

        if (leftTerms == null || rightTerms == null) {
            return null
        }

        // Subtract right from left
        val lhs = leftTerms.combine(rightTerms, SUBTRACTION)

        val indicator = variable.getNumberSetIndicator()
        val constant = NegateExpression(lhs.terms.remove(0) ?: ConstantExpression.zero(indicator))

        return lhs to constant
    }

    fun <T : Number> expandTerms(expr: IExpr<*>, variable: VariableExpression<T>): CollectedTerms<T>? {
        val setIndicator = variable.getSetIndicator() as NumberSetIndicator<T>
        val castExpr = TypeCastExpression(expr, setIndicator, true)
        return when (expr) {
            is ArithmeticExpression -> {
                val lhs = expandTerms(expr.lhs, variable)
                val rhs = expandTerms(expr.rhs, variable)

                lhs?.let { rhs?.let { lhs.combine(rhs, expr.op) } }
            }

            is ConstExpr -> CollectedTerms(
                setIndicator,
                terms = mutableMapOf(0 to castExpr)
            )

            is VariableExpression -> {
                if (expr.isResolved()) {
                    return expandTerms(expr.resolvedInto!!, variable)
                }

                if (expr.getKey() == variable.getKey()) {
                    CollectedTerms(setIndicator, terms = mutableMapOf(1 to ConstantExpression.one(setIndicator)))
                } else {
                    CollectedTerms(setIndicator, terms = mutableMapOf(0 to castExpr))
                }
            }
            // I think this is correct?
            is NegateExpression -> expandTerms(expr.expr, variable)?.negate()
            is NumIterationTimesExpression -> TODO("Maybe one day? Unsure if this is possible")
            is TypeCastExpression<*, *> -> {
                fun <Q : Number> extra(expr: IExpr<Q>): CollectedTerms<T>? =
                    expandTerms(expr, variable)?.castTo(setIndicator)

                extra(expr.expr.tryCastToNumbers()!!)
            }
            // This is quite solidly beyond our abilities.
            is IfExpression<*> -> null
            else -> TODO("Not implemented constraints for $expr")
        }
    }
}
