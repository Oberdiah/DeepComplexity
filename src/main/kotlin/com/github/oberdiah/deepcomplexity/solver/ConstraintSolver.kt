package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.VariableExpression.VariableKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConstraintSolver {
    private data class CollectedTerms(
        /**
         * Map from exponent to coefficient. 0th is constant, 1st is linear, 2nd is quadratic, etc.
         */
        val terms: MutableMap<Int, IExprRetNum> = mutableMapOf(),
    ) {
        fun negate(): CollectedTerms {
            val newTerms = mutableMapOf<Int, IExprRetNum>()

            for ((exp, term) in terms) {
                newTerms[exp] = NegateExpression(term)
            }

            return CollectedTerms(newTerms)
        }

        fun combine(other: CollectedTerms, op: BinaryNumberOp): CollectedTerms {
            return when (op) {
                ADDITION, SUBTRACTION -> {
                    // Combine terms
                    val newTerms = mutableMapOf<Int, IExprRetNum>()
                    newTerms.putAll(terms)

                    for ((exp, term) in other.terms) {
                        newTerms[exp] = merge(newTerms[exp], term, op)
                    }

                    CollectedTerms(newTerms)
                }

                MULTIPLICATION -> {
                    // Multiply terms together
                    val newTerms = mutableMapOf<Int, IExprRetNum>()

                    for ((exp1, term1) in terms) {
                        for ((exp2, term2) in other.terms) {
                            // Add exponents and multiply coefficients
                            val newExp = exp1 + exp2
                            val newTerm = ArithmeticExpression(term1, term2, MULTIPLICATION)
                            newTerms[newExp] = merge(newTerms[newExp], newTerm, ADDITION)
                        }
                    }
                    CollectedTerms(newTerms)
                }

                DIVISION -> {
                    val newTerms = mutableMapOf<Int, IExprRetNum>()

                    for ((exp1, term1) in terms) {
                        for ((exp2, term2) in other.terms) {
                            // Subtract exponents and divide coefficients
                            val newExp = exp1 - exp2
                            val newTerm = ArithmeticExpression(term1, term2, DIVISION)
                            newTerms[newExp] = merge(newTerms[newExp], newTerm, ADDITION)
                        }
                    }
                    CollectedTerms(newTerms)
                }
            }
        }
    }

    private fun merge(lhs: IExprRetNum?, rhs: IExprRetNum, op: BinaryNumberOp): IExprRetNum {
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
    fun getVariableConstraints(
        expr: ComparisonExpression,
        varKey: VariableKey,
    ): NumberSet? {
        val numClazz = expr.lhs.getBaseClass()

        // Collect terms from both sides
        val leftTerms = expandTerms(expr.lhs, varKey)
        val rightTerms = expandTerms(expr.rhs, varKey)

        // Subtract right from left
        val lhs = leftTerms.combine(rightTerms, SUBTRACTION)

        val constant = NegateExpression(lhs.terms.remove(0) ?: ConstExprNum(NumberSet.zero(numClazz)))

        if (lhs.terms.isEmpty()) {
            // The variable is not present in the expression, so there is no constraint.
            return null
        }

        return if (lhs.terms.size == 1) {
            // This is good, we can solve this now.
            val (exponent, coefficient) = lhs.terms.entries.first()
            val coefficientValue = coefficient.evaluate(ConstantExpression.TRUE)
            val constantValue = constant.evaluate(ConstantExpression.TRUE)

            val coeffGEZ = coefficientValue.comparisonOperation(
                NumberSet.zero(numClazz),
                ComparisonOp.GREATER_THAN_OR_EQUAL
            )

            val rhs = constantValue.arithmeticOperation(coefficientValue, DIVISION)

            val resultingSet = when (coeffGEZ) {
                TRUE -> rhs.getSetSatisfying(expr.comp)
                FALSE -> rhs.getSetSatisfying(expr.comp.flip())
                BOTH -> rhs.getSetSatisfying(expr.comp)
                    .union(rhs.getSetSatisfying(expr.comp.flip())) as NumberSet

                NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
            }

            return if (exponent == 1) {
                resultingSet
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

    private fun expandTerms(expr: IExprRetNum, varKey: VariableKey): CollectedTerms {
        return when (expr) {
            is ArithmeticExpression -> {
                val lhs = expandTerms(expr.lhs, varKey)
                val rhs = expandTerms(expr.rhs, varKey)

                lhs.combine(rhs, expr.op)
            }

            is ConstExprNum -> CollectedTerms(terms = mutableMapOf(0 to expr))
            is VariableExpression.VariableNumber -> {
                if (expr.getKey() == varKey) {
                    CollectedTerms(terms = mutableMapOf(1 to ConstExprNum(NumberSet.singleValue(1))))
                } else {
                    CollectedTerms(terms = mutableMapOf(0 to expr))
                }
            }

            // I think this is correct?
            is NegateExpression -> expandTerms(expr.expr, varKey).negate()
        }
    }
}
