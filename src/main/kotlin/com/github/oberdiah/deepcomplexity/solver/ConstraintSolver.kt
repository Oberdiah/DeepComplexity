package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.into

object ConstraintSolver {
    data class CollectedTerms<T : Number>(
        val ind: SetIndicator<T>,
        val terms: MutableMap<Int, NumberSet<T>>,
    ) {
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
                // I'm suspicious of this. It feels like the most obvious thing to do,
                // but I'm not 100% sure it's actually fine.
                terms.mapValues { (_, term) -> term.cast(setIndicator)!!.into() }.toMutableMap()
            )

        fun negate(): CollectedTerms<T> {
            return CollectedTerms(ind, terms.mapValues { (_, term) -> term.negate() }.toMutableMap())
        }

        fun combine(other: CollectedTerms<T>, op: BinaryNumberOp): CollectedTerms<T> {
            return when (op) {
                ADDITION, SUBTRACTION -> {
                    // Combine terms
                    val newTerms = mutableMapOf<Int, NumberSet<T>>()
                    newTerms.putAll(terms)

                    for ((exp, term) in other.terms) {
                        newTerms[exp] = merge(newTerms[exp], term, op)
                    }

                    CollectedTerms(ind, newTerms)
                }

                MULTIPLICATION -> {
                    // Multiply terms together
                    val newTerms = mutableMapOf<Int, NumberSet<T>>()

                    for ((exp1, term1) in terms) {
                        for ((exp2, term2) in other.terms) {
                            // Add exponents and multiply coefficients
                            val newExp = exp1 + exp2
                            val newTerm = term1.arithmeticOperation(term2, MULTIPLICATION)
                            newTerms[newExp] = merge(newTerms[newExp], newTerm, ADDITION)
                        }
                    }
                    CollectedTerms(ind, newTerms)
                }

                DIVISION -> {
                    val newTerms = mutableMapOf<Int, NumberSet<T>>()

                    for ((exp1, term1) in terms) {
                        for ((exp2, term2) in other.terms) {
                            // Subtract exponents and divide coefficients
                            val newExp = exp1 - exp2
                            val newTerm = term1.arithmeticOperation(term2, DIVISION)
                            newTerms[newExp] = merge(newTerms[newExp], newTerm, ADDITION)
                        }
                    }
                    CollectedTerms(ind, newTerms)
                }

                MODULO -> {
                    TODO()
                }
            }
        }
    }

    private fun <T : Number> merge(lhs: NumberSet<T>?, rhs: NumberSet<T>, op: BinaryNumberOp): NumberSet<T> {
        return (lhs ?: NumberSet.zero(rhs.ind))
            .arithmeticOperation(rhs, op)
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

        val variables = expr.getVariables()
        for (variable in variables) {
            assert(variable.ind is NumberSetIndicator<*>) {
                "All variables must be number sets. This requires more thought if we've hit this."
            }

            // Safety: We know the variable is a number set.
            @Suppress("UNCHECKED_CAST")
            val castVariable = variable as VariableExpression<out Number>

            val variableConstraints = getVariableConstraints(expr, castVariable, ConstantExpression.TRUE) ?: continue
            constraints = constraints.withConstraint(variable.key, variableConstraints)
        }

        return constraints
    }

    /**
     * Does not return a given constraint if it could not be ascertained accurately.
     * Constraints are always the set of values that may result in this equation being true.
     * I.e. they try to be as broad as possible.
     */
    private fun <T : Number> getVariableConstraints(
        expr: ComparisonExpression<*>,
        variable: VariableExpression<T>,
        condition: Expr<Boolean>,
    ): ISet<T>? {
        if (expr.getVariables().none { it.key == variable.key }) {
            // The variable wasn't found in the expression.
            return null
        }

        val (lhs, constant) = normalizeComparisonExpression(expr, variable, condition) ?: return null

        if (lhs.terms.isEmpty()) {
            // The variable wasn't found in the expression, but we should have caught this earlier.
            throw IllegalStateException("Variable $variable was not found in the expression $lhs")
        }

        return if (lhs.terms.size == 1) {
            // This is good, we can solve this now.
            val (exponent, coefficient) = lhs.terms.entries.first()

            val shouldFlip = coefficient.comparisonOperation(
                variable.getNumberSetIndicator().onlyZeroSet(),
                ComparisonOp.LESS_THAN
            )

            val rhs = constant.divide(coefficient)
            return if (exponent == 1) {
                when (shouldFlip) {
                    BooleanSet.TRUE -> rhs.getSetSatisfying(expr.comp.flip())
                    BooleanSet.FALSE -> rhs.getSetSatisfying(expr.comp)
                    BooleanSet.BOTH -> rhs.getSetSatisfying(expr.comp)
                        .union(rhs.getSetSatisfying(expr.comp.flip()))

                    BooleanSet.NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                }
            } else {
                println("Cannot constraint solve yet: $lhs (exponent $exponent)")
                null
            }
        } else {
            // We might be able to solve this in the future, for now we'll not bother.
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
        condition: Expr<Boolean>,
    ): Pair<CollectedTerms<T>, NumberSet<T>>? {
        // Collect terms from both sides
        val leftTerms = expandTerms(expr.lhs, variable, condition)
        val rightTerms = expandTerms(expr.rhs, variable, condition)

        if (leftTerms == null || rightTerms == null) {
            return null
        }

        // Subtract right from left
        val lhs = leftTerms.combine(rightTerms, SUBTRACTION)

        val ind = variable.getNumberSetIndicator()
        val constant = (lhs.terms.remove(0) ?: ind.onlyZeroSet()).negate()

        return lhs to constant
    }

    fun <T : Number> expandTerms(
        expr: Expr<*>,
        variable: VariableExpression<T>,
        condition: Expr<Boolean>
    ): CollectedTerms<T>? {
        val ind = variable.getNumberSetIndicator()

        return when (expr) {
            is ArithmeticExpression -> {
                val lhs = expandTerms(expr.lhs, variable, condition)
                val rhs = expandTerms(expr.rhs, variable, condition)

                lhs?.let { rhs?.let { lhs.combine(rhs, expr.op) } }
            }

            // I think this is correct?
            is NegateExpression -> expandTerms(expr.expr, variable, condition)?.negate()
            is NumIterationTimesExpression -> TODO("Maybe one day? Unsure if this is possible")
            is TypeCastExpression<*, *> -> {
                fun <Q : Number> extra(expr: Expr<Q>): CollectedTerms<T>? =
                    expandTerms(expr, variable, condition)?.castTo(ind)

                extra(expr.expr.castToNumbers())
            }
            // This is quite solidly beyond our abilities.
            is IfExpression<*> -> null

            is ConstExpr, is VariableExpression -> {
                if (expr is VariableExpression && expr.key == variable.key) {
                    CollectedTerms(ind, terms = mutableMapOf(1 to ind.onlyOneSet()))
                } else {
//                    val bundle = expr.evaluate(ExprEvaluate.Scope(condition = condition)).collapse()
//                    val castExpr = bundle.cast(ind)!!.into()
//                    CollectedTerms(ind, terms = mutableMapOf(0 to castExpr))
                    TODO()
                }
            }

            else -> TODO("Not implemented constraints for $expr")
        }
    }
}
