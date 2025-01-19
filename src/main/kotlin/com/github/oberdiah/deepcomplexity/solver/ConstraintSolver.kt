package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.VariableExpression.VariableKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConstraintSolver {
    data class EvaluatedCollectedTerms<T : NumberSet<T>>(
        val terms: Map<Int, T>,
    )

    data class CollectedTerms<T : NumberSet<T>>(
        val setIndicator: SetIndicator<T>,
        /**
         * Map from exponent to coefficient. 0th is constant, 1st is linear, 2nd is quadratic, etc.
         */
        val terms: MutableMap<Int, IExpr<T>> = mutableMapOf(),
    ) {
        fun evaluate(condition: IExpr<BooleanSet>): EvaluatedCollectedTerms<T> {
            return EvaluatedCollectedTerms(
                terms.mapValues { (_, term) -> term.evaluate(condition) }
            )
        }

        override fun toString(): String {
            return terms.entries.joinToString(" + ") { (exp, term) ->
                when (exp) {
                    0 -> term.toString()
                    1 -> "${term}x"
                    else -> "${term}x^$exp"
                }
            }
        }

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

    private fun <T : NumberSet<T>> merge(lhs: IExpr<T>?, rhs: IExpr<T>, op: BinaryNumberOp): IExpr<T> {
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
     * Returns null if accurate constraints could not be ascertained.
     */
    fun <T : NumberSet<T>> getVariableConstraints(
        expr: ComparisonExpression<T>,
        varKey: VariableKey,
    ): IExpr<T>? {
        val (lhs, constant) = normalizeComparisonExpression(expr, varKey)

        return if (lhs.terms.size == 1) {
            // This is good, we can solve this now.
            val (exponent, coefficient) = lhs.terms.entries.first()

            val coeffLZ = ComparisonExpression(
                coefficient,
                ConstantExpression.zero(expr.lhs),
                ComparisonOp.LESS_THAN
            )

            val rhs = ArithmeticExpression(constant, coefficient, DIVISION)

            return if (exponent == 1) {
                NumberLimitsExpression(rhs, coeffLZ, expr.comp)
            } else {
                println("Cannot constraint solve yet: $lhs")
                null
            }
        } else {
            // We might be able to solve this in future, for now we'll not bother.
            println("Cannot constraint solve yet: $lhs")
            null
        }
    }

    /**
     * Normalizes the comparison expression to a form where the left hand side is a linear equation and the right hand
     * side is a constant.
     */
    private fun <T : NumberSet<T>> normalizeComparisonExpression(
        expr: ComparisonExpression<T>,
        varKey: VariableKey,
    ): Pair<CollectedTerms<T>, IExpr<T>> {
        // Collect terms from both sides
        val leftTerms = expandTerms(expr.lhs, varKey)
        val rightTerms = expandTerms(expr.rhs, varKey)

        // Subtract right from left
        val lhs = leftTerms.combine(rightTerms, SUBTRACTION)

        val indicator = expr.lhs.getSetIndicator()
        val constant = NegateExpression(lhs.terms.remove(0) ?: ConstExpr(NumberSet.zero(indicator)))

        return lhs to constant
    }

    fun <T : NumberSet<T>> expandTerms(expr: IExpr<T>, varKey: VariableKey): CollectedTerms<T> {
        val setIndicator = expr.getSetIndicator()
        return when (expr) {
            is ArithmeticExpression -> {
                val lhs = expandTerms(expr.lhs, varKey)
                val rhs = expandTerms(expr.rhs, varKey)

                lhs.combine(rhs, expr.op)
            }

            is ConstExpr -> CollectedTerms(setIndicator, terms = mutableMapOf(0 to expr))
            is VariableExpression -> {
                if (expr.getKey() == varKey) {
                    CollectedTerms(setIndicator, terms = mutableMapOf(1 to ConstExpr(NumberSet.one(expr.setInd))))
                } else {
                    CollectedTerms(setIndicator, terms = mutableMapOf(0 to expr))
                }
            }

            // I think this is correct?
            is NegateExpression -> expandTerms(expr.expr, varKey).negate()
            is NumberLimitsExpression -> throw IllegalStateException("Uuh ... this can maybe happen?")
            is NumIterationTimesExpression -> TODO("Maybe one day? Unsure if this is possible")
            else -> TODO("Not implemented constraints for $expr")
        }
    }
}
