package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.NumberBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.into
import com.github.oberdiah.deepcomplexity.utilities.Functional

/**
 *
 */
class NumberVariances<T : Number> private constructor(
    override val ind: NumberSetIndicator<T>,
    private val multipliers: Map<Context.Key, NumberBundle<T>> = mapOf()
) : Variances<T> {
    init {
        assert(multipliers.keys.count { it.isEphemeral() } <= 1) {
            "Only one ephemeral key is allowed in the variances map"
        }
    }

    override fun toString(): String {
        return multipliers.entries.joinToString(", ") { (key, multiplier) ->
            "$key: ($multiplier)"
        }
    }

    companion object {
        fun <T : Number> newFromConstant(constant: NumberBundle<T>): NumberVariances<T> =
            NumberVariances(constant.ind, mapOf(Context.Key.EphemeralKey.new() to constant))

        fun <T : Number> newFromVariance(ind: NumberSetIndicator<T>, key: Context.Key): NumberVariances<T> {
            return NumberVariances(ind, mapOf(key to ind.onlyOneSet()))
        }

        private fun <T : Number> newFromMultiplierMap(
            ind: NumberSetIndicator<T>,
            multipliers: Map<Context.Key, NumberBundle<T>>
        ): NumberVariances<T> {
            val ephemeralMap = multipliers.filterKeys { it.isEphemeral() }
            val notEphemeralMap = multipliers.filterKeys { !it.isEphemeral() }

            // Ensure only one ephemeral key is present.
            // If more than one is present, we need to merge them through addition.
            // I think proving division with this might be really hard.
            val ephemeralMultiplier =
                ephemeralMap.values.fold(ind.onlyZeroSet()) { acc, multiplier ->
                    acc.add(multiplier)
                }

            return NumberVariances(
                ind,
                notEphemeralMap + (Context.Key.EphemeralKey.new() to ephemeralMultiplier)
            )
        }
    }

    override fun toDebugString(constraints: Constraints): String {
        return if (multipliers.keys.all { it.isEphemeral() }) {
            collapse(constraints).toString()
        } else {
            multipliers.entries.filter { (k, m) -> !(m.isZero() && k.isEphemeral()) }
                .joinToString(" + ") { (key, multiplier) ->
                    val multiplierStr = if (multiplier.isOne() && !key.isEphemeral()) "" else "$multiplier"
                    val keyStr = if (key.isEphemeral()) "" else key.toString()
                    val constraint = constraints.getConstraint(ind, key)
                    val constraintStr = if (constraint.isFull()) "" else "[$constraint]"

                    "$multiplierStr$keyStr$constraintStr"
                }
        }
    }

    override fun isTrackingVar(key: Context.Key): Boolean {
        return multipliers.keys.any { it == key }
    }

    fun grabConstraint(constraints: Constraints, key: Context.Key): NumberBundle<T> {
        return if (key is Context.Key.EphemeralKey) {
            // Constants/ephemeral keys don't have any variance or constraints,
            // so the 'variable', if you can call it that, is always constrained to exactly 1.
            ind.onlyOneSet()
        } else {
            constraints.getConstraint(ind, key).into()
        }
    }

    /**
     * Collapse the multipliers into a single set of values.
     * Requires constraints because a NumberVariance on its own doesn't contain
     * all the information it needs alone.
     */
    override fun collapse(constraints: Constraints): NumberBundle<T> =
        multipliers.entries.fold(ind.onlyZeroSet()) { acc, (key, multiplier) ->
            acc.add(multiplier.multiply(grabConstraint(constraints, key)))
        }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Variances<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(newInd: NumberSetIndicator<OutT>): NumberVariances<OutT>? {
            return newFromMultiplierMap(newInd, multipliers.mapValues { (_, multiplier) ->
                // I have no idea if doing this actually works with wrapping. Probably doesn't.
                // Worth writing a test for it at some point.
                multiplier.cast(newInd)?.into() ?: return null
            })
        }

        @Suppress("UNCHECKED_CAST") // Safety: Trivially true by checking the signature of extra().
        return extra(newInd) as Variances<Q>?
    }

    fun isOne(constraints: Constraints): Boolean = collapse(constraints).isOne()

    fun negate(): NumberVariances<T> {
        return newFromMultiplierMap(ind, multipliers.mapValues { (_, multiplier) ->
            multiplier.negate()
        })
    }

    fun arithmeticOperation(
        other: NumberVariances<T>,
        operation: BinaryNumberOp,
        constraints: Constraints
    ): NumberVariances<T> {
        when (operation) {
            ADDITION, SUBTRACTION -> {
                return newFromMultiplierMap(
                    ind, Functional.mergeMapsWithBlank(
                        multipliers,
                        other.multipliers,
                        ind.onlyZeroSet()
                    ) { me, other ->
                        if (operation == ADDITION) {
                            me.add(other)
                        } else {
                            me.subtract(other)
                        }
                    })
            }

            MULTIPLICATION -> {
                val newMultipliers = mutableMapOf<Context.Key, NumberBundle<T>>()

                for ((key, meMultiplier) in multipliers) {
                    for ((otherKey, otherMultiplier) in other.multipliers) {
                        // If both are ephemeral, we can just collapse & multiply, no problem.
                        // If one is ephemeral, it should get multiplied into the multiplier of the other.
                        // If neither are ephemeral, we should pick one to become the ephemeral 'mergee'
                        val meIsEphemeral = key.isEphemeral()
                        val otherIsEphemeral = otherKey.isEphemeral()

                        val meCollapsed = meMultiplier.multiply(grabConstraint(constraints, key))
                        val otherCollapsed = otherMultiplier.multiply(grabConstraint(constraints, otherKey))

                        val newKey = Context.Key.EphemeralKey.new()

                        if (meIsEphemeral && otherIsEphemeral) {
                            newMultipliers[newKey] = meCollapsed.multiply(otherCollapsed)
                        } else if (otherIsEphemeral) {
                            // Other is ephemeral, so we stay relatively untouched and multiply it into our multiplier.
                            newMultipliers[key] = meMultiplier.multiply(otherCollapsed)
                        } else if (meIsEphemeral) {
                            // We are ephemeral, so other stays relatively untouched and multiplies us into its multiplier.
                            newMultipliers[otherKey] = otherMultiplier.multiply(meCollapsed)
                        } else {
                            // Neither are ephemeral, so we need to pick one to stick around.
                            // This would definitely help performance in some cases, worth tweaking at some point.

                            // At the moment we're picking neither and letting them both die. :)
                            var newMultiplier = meCollapsed.multiply(otherCollapsed)

                            if (key == otherKey) {
                                // If the keys are the same, we're squaring, so we can only have a positive output.
                                newMultiplier = newMultiplier.intersect(ind.positiveNumbersAndZero())
                            }

                            // No new multiplier is needed since we've collapsed the variances.
                            newMultipliers[newKey] = newMultiplier
                        }

                    }
                }

                return newFromMultiplierMap(ind, newMultipliers)
            }

            DIVISION -> {
                if (other.isOne(constraints)) {
                    return this
                }

                // Division is going to be really hard to get right due to order-of-operations.
                TODO("DIVISION :(")
            }

            MODULO -> {
                val meCollapsed = this.collapse(constraints)
                val otherCollapsed = other.collapse(constraints)

                return newFromConstant(
                    meCollapsed.arithmeticOperation(otherCollapsed, MODULO)
                )
            }
        }
    }

    fun comparisonOperation(
        other: NumberVariances<T>,
        operation: ComparisonOp,
        constraints: Constraints
    ): BooleanVariances =
        // We could maybe do something smarter here long-term, but this'll do for now.
        collapse(constraints).comparisonOperation(other.collapse(constraints), operation).toConstVariance().into()

    fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<T>,
        valid: NumberBundle<T>
    ): NumberVariances<T> = TODO("Not yet implemented")
}