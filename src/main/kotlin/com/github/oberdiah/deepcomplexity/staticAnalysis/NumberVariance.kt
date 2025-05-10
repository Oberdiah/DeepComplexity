package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.utilities.Functional

/**
 * The way this works is as follows:
 * The full set of values of this class is a sum of the variances.
 *
 * The variances have at most one ephemeral key (the variance we've given up on)
 * and then a bunch of known variance we're tracking.
 *
 * The 'constraint' part of the variance is the possible set of values the underlying variable could be,
 * and the multiplier gets applied on top of that.
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
        /**
         *  Usually a constant — and will be when adding or subtracting, however, when multiplying
         *  it can become a range, think of the case of {0..5} * {2..3x} — to preserve the variance,
         *  which we'd like to do if we can, we need the multiplier to be a set.
         */
        val multiplier: NumberSet<T>,
        val constraint: NumberSet<T>
    ) {
        fun collapse(): NumberSet<T> = constraint.multiply(multiplier)
    }

    companion object {
        fun <T : Number> newFromConstant(constant: NumberSet<T>): NumberVariance<T> =
            newFromVariance(constant, Context.Key.EphemeralKey.new())

        fun <T : Number> newFromVariance(constant: NumberSet<T>, key: Context.Key): NumberVariance<T> {
            assert(constant.ind.isWholeNum()) { "Indicator must be whole number for now" }
            return NumberVariance(
                constant.ind,
                mapOf(key to Variance(constant.ind.onlyOneSet(), constant))
            )
        }

        private fun <T : Number> newFromVarianceMap(
            ind: NumberSetIndicator<T>,
            variances: Map<Context.Key, Variance<T>>
        ): NumberVariance<T> {
            // Ensure only one ephemeral key is present.
            // If more than one is present, we need to merge them through addition.
            val ephemeralEntries = variances.filterKeys { it is Context.Key.EphemeralKey }

            // I think division with this might be really hard.
            val mergedVariances =
                Variance(
                    ind.onlyOneSet(),
                    ephemeralEntries.values.map { it.collapse() }.fold(ind.onlyZeroSet()) { acc, variance ->
                        acc.add(variance)
                    })

            val nonEphemeralEntries = variances.filterKeys { it !is Context.Key.EphemeralKey }
            val mergedVariancesMap = nonEphemeralEntries + (Context.Key.EphemeralKey.new() to mergedVariances)

            return NumberVariance(ind, mergedVariancesMap)
        }
    }

    override fun toString(): String = collapse().toString()

    override fun collapse(): NumberSet<T> = variances.values.fold(ind.onlyZeroSet()) { acc, variance ->
        acc.add(variance.collapse())
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): VarianceBundle<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(newInd: NumberSetIndicator<OutT>): NumberVariance<OutT>? {
            return newFromVarianceMap(newInd, variances.mapValues { (_, variance) ->
                Variance(
                    // I have no idea if doing it like this actually works with wrapping. Probably doesn't.
                    // Worth writing a test for it at some point.
                    variance.multiplier.cast(newInd)?.into() ?: return null,
                    variance.constraint.cast(newInd)?.into() ?: return null
                )
            })
        }

        @Suppress("UNCHECKED_CAST") // Safety: Trivially true by checking the signature of extra().
        return extra(newInd) as VarianceBundle<Q>?
    }

    fun isOne(): Boolean = collapse().isOne()

    fun negate(): NumberVariance<T> {
        return newFromVarianceMap(ind, variances.mapValues { (_, variance) ->
            Variance(variance.multiplier.negate(), variance.constraint)
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
                        Variance(ind.onlyZeroSet(), ind.newFullBundle())
                    ) { me, other ->
                        val newSet = me.constraint.intersect(other.constraint)
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

                for ((key, meVariance) in variances) {
                    for ((otherKey, otherVariance) in other.variances) {
                        // If both are ephemeral, we can just collapse & multiply, no problem.
                        // If one is ephemeral, it should get multiplied into the multiplier of the other.
                        // If neither are ephemeral, we should pick one to become the ephemeral 'mergee'
                        val meIsEphemeral = key is Context.Key.EphemeralKey
                        val otherIsEphemeral = otherKey is Context.Key.EphemeralKey

                        val meCollapsed = meVariance.collapse()
                        val otherCollapsed = otherVariance.collapse()

                        val newKey = Context.Key.EphemeralKey.new()

                        if (meIsEphemeral && otherIsEphemeral) {
                            newVariances[newKey] = Variance(ind.onlyOneSet(), meCollapsed.multiply(otherCollapsed))
                        } else if (otherIsEphemeral) {
                            // Other is ephemeral, so we stay relatively untouched and multiply it into our multiplier.
                            newVariances[key] = Variance(
                                meVariance.multiplier.multiply(otherCollapsed),
                                meVariance.constraint
                            )
                        } else if (meIsEphemeral) {
                            // We are ephemeral, so other stays relatively untouched and multiplies us into its multiplier.
                            newVariances[otherKey] = Variance(
                                otherVariance.multiplier.multiply(meCollapsed),
                                otherVariance.constraint
                            )
                        } else {
                            // Neither are ephemeral, so we need to pick one to stick around.
                            // This would definitely help performance in some cases, worth tweaking at some point.

                            // At the moment we're picking neither, and letting them both die. :)
                            var newSet = meCollapsed.multiply(otherCollapsed)

                            if (key == otherKey) {
                                // If the keys are the same, we're squaring, so we can only have a positive output.
                                newSet = newSet.intersect(ind.allPositiveNumbers())
                            }

                            // No new multiplier is needed since we've collapsed the variances.
                            newVariances[newKey] = Variance(ind.onlyOneSet(), newSet)
                        }

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