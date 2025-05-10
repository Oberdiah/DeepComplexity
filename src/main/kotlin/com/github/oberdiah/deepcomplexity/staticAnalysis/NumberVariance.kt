package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.utilities.BigFraction
import com.github.oberdiah.deepcomplexity.utilities.Functional

/**
 * The way this works is as follows:
 * The full set of possible values of this class is equal to the constant added to all the variances when multiplied
 * by the multiplier and divided by the divider.
 *
 * Is it sufficient to treat each of the sets independently?
 * Say we wrap at 10, and we have {8..10, 2*(5..6)x, 3*(7..8)y}.
 * What happens when you add {3*(-2..0)x} to it?
 * The answer is you do the algebra, so you only care about the x values.
 *
 * And I guess the result is 5*(-2..0, 5..6)x?
 * Nope! The answer is that it can't/shouldn't happen.
 * If they do overlap, however, the answer would be the intersection.
 * I think.
 * I'm not confident in that, but we'll just have to find out.
 */
class NumberVariance<T : Number> private constructor(
    override val ind: NumberSetIndicator<T>,
    private val variances: Map<Context.Key, Variance<T>> = mapOf()
) : VarianceBundle<T> {
    init {
        assert(variances.keys.count { it is Context.Key.EphemeralKey } <= 1) {
            "Only one ephemeral key is allowed in the variances map"
        }
    }

    private data class Variance<T : Number>(
        val multiplier: BigFraction,
        val set: NumberSet<T>
    ) {
        fun collapse(): NumberSet<T> {
            return set
                .arithmeticOperation(
                    NumberSet.newFromConstant(set.ind.castToMe(multiplier.numeratorAsLong)),
                    MULTIPLICATION
                )
                .arithmeticOperation(
                    NumberSet.newFromConstant(set.ind.castToMe(multiplier.denominatorAsLong)),
                    DIVISION
                )
        }
    }

    companion object {
        fun <T : Number> newFromConstant(constant: NumberSet<T>): NumberVariance<T> {
            return newFromVariance(constant, Context.Key.EphemeralKey.new())
        }

        fun <T : Number> newFromVariance(constant: NumberSet<T>, key: Context.Key): NumberVariance<T> {
            assert(constant.ind.isWholeNum()) { "Indicator must be whole number for now" }
            return NumberVariance(constant.ind, mapOf(key to Variance(BigFraction.ONE, constant)))
        }

        private fun <T : Number> newFromVarianceMap(
            ind: NumberSetIndicator<T>,
            variances: Map<Context.Key, Variance<T>>
        ): NumberVariance<T> {
            // Ensure only one ephemeral key is present.
            // If more than one is present, we need to merge them through addition.

            val ephemeralEntries = variances.filterKeys { it is Context.Key.EphemeralKey }

            // I think division with this is going to be really hard.
            val mergedVariances =
                Variance(
                    BigFraction.ONE,
                    ephemeralEntries.values.map { it.collapse() }.fold(ind.zeroNumberSet()) { acc, variance ->
                        acc.arithmeticOperation(variance, ADDITION)
                    })

            val nonEphemeralEntries = variances.filterKeys { it !is Context.Key.EphemeralKey }
            val mergedVariancesMap = nonEphemeralEntries + (Context.Key.EphemeralKey.new() to mergedVariances)

            return NumberVariance(ind, mergedVariancesMap)
        }
    }

    override fun toString(): String = collapse().toString()

    override fun collapse(): NumberSet<T> = variances.values.fold(ind.zeroNumberSet()) { acc, variance ->
        acc.arithmeticOperation(variance.collapse(), ADDITION)
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): VarianceBundle<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(newInd: NumberSetIndicator<OutT>): NumberVariance<OutT>? {
            return newFromVarianceMap(newInd, variances.mapValues { (_, variance) ->
                Variance(variance.multiplier, variance.set.cast(newInd)?.into() ?: return null)
            })
        }

        @Suppress("UNCHECKED_CAST") // Safety: Trivially true by checking the signature of extra().
        return extra(newInd) as VarianceBundle<Q>?
    }

    fun isOne(): Boolean = collapse().isOne()

    fun negate(): NumberVariance<T> {
        return newFromVarianceMap(ind, variances.mapValues { (_, variance) ->
            Variance(variance.multiplier.negate(), variance.set)
        })
    }

    fun arithmeticOperation(
        other: NumberVariance<T>,
        operation: BinaryNumberOp
    ): NumberVariance<T> {
        when (operation) {
            ADDITION, SUBTRACTION -> {
                return newFromVarianceMap(
                    ind, Functional.mergeMapsWithBlank(
                        variances,
                        other.variances,
                        Variance(BigFraction.ZERO, ind.newFullBundle())
                    ) { me, other ->
                        val newSet = me.set.intersect(other.set)
                        val newMultiplier =
                            if (operation == ADDITION) {
                                me.multiplier.add(other.multiplier)
                            } else {
                                me.multiplier.subtract(other.multiplier)
                            }
                        Variance(newMultiplier, newSet)
                    })
            }

            MULTIPLICATION -> {
                val newVariances = mutableMapOf<Context.Key, Variance<T>>()

                for ((key, variance) in variances) {
                    for ((otherKey, otherVariance) in other.variances) {
                        val newKey = Context.Key.EphemeralKey.new()
                        var newSet = variance.collapse()
                            .arithmeticOperation(otherVariance.collapse(), MULTIPLICATION)

                        if (key == otherKey) {
                            // If the keys are the same, we're squaring, so we can only have a positive output.
                            newSet = newSet.intersect(ind.allPositiveNumbers())
                        }

                        // No new multiplier is needed since we've collapsed the variances.
                        newVariances[newKey] = Variance(BigFraction.ONE, newSet)
                    }
                }

                return newFromVarianceMap(ind, newVariances)
            }

            DIVISION -> {
                if (other.isOne()) {
                    return this
                }

                // Division is going to be really hard to get right due to order of operations.
                TODO()
            }
        }
    }

    fun comparisonOperation(
        other: NumberVariance<T>,
        operation: ComparisonOp
    ): BooleanSet.BooleanVariance =
        // We could maybe do something smarter here long-term, but this'll do for now.
        collapse().comparisonOperation(other.collapse(), operation).withEphemeralVariance().into()

    fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<T>,
        valid: NumberSet<T>
    ): NumberVariance<T> = TODO("Not yet implemented")
}