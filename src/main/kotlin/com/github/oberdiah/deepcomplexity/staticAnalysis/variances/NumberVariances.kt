package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.into
import com.github.oberdiah.deepcomplexity.utilities.Functional
import com.jetbrains.rd.util.firstOrNull

/**
 * Think of this as a simple equation like the form: `2x + 3y + 4z`
 * where the keys are the variables (x, y, z) and the values are the multipliers (2, 3, 4).
 *
 * Now, we don't store the constraints here; they come in from outside, so we can't calculate a full
 * range from this alone. So intuitively, when we want to collapse, we take the constraints for each key
 * and multiply them by the multiplier, and then add them all together.
 *
 * The way this works with casting is that if the key is not of the base indicator type, we'll assume
 * casting is the first thing that happens.
 *
 * This means we need to be able to figure out whether an equation *could* have wrapped, and if so,
 * we have no choice but to collapse it. Simple tracking with a constant of 1,
 * such as `Variances<Short>(x(short): 1)`, can be guaranteed not to wrap, so that stuff is straightforward.
 *
 * However, `Variances<Short>(x(short): 2)`, and `Variances<Short>(x(short): 1, y(short): 1)` both may have
 * ended up wrapping, so with a naive implementation they'd need to be collapsed. Whether I actually
 * end up doing that or not remains to be seen.
 */
@ConsistentCopyVisibility
data class NumberVariances<T : Number> private constructor(
    override val ind: NumberSetIndicator<T>,
    private val multipliers: Map<Context.Key, NumberSet<T>> = mapOf()
) : Variances<T> {
    init {
        assert(multipliers.keys.count { it.isEphemeral() } <= 1) {
            "Only one ephemeral key is allowed in the variances map"
        }
    }

    /**
     * They're equal if everything matches except the ephemeral key, which can be different.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NumberVariances<*>) return false
        if (ind != other.ind) return false
        val myNonEphemeralMultipliers = multipliers.filterKeys { !it.isEphemeral() }
        val otherNonEphemeralMultipliers = other.multipliers.filterKeys { !it.isEphemeral() }
        if (myNonEphemeralMultipliers != otherNonEphemeralMultipliers) return false
        val myEphemeralMultiplier = multipliers.filterKeys { it.isEphemeral() }.firstOrNull()?.value
        val otherEphemeralMultiplier = other.multipliers.filterKeys { it.isEphemeral() }.firstOrNull()?.value
        return myEphemeralMultiplier == otherEphemeralMultiplier
    }

    override fun hashCode(): Int {
        var result = ind.hashCode()
        val myNonEphemeralMultipliers = multipliers.filterKeys { !it.isEphemeral() }
        result = 31 * result + myNonEphemeralMultipliers.hashCode()
        val myEphemeralMultiplier = multipliers.filterKeys { it.isEphemeral() }.firstOrNull()?.value
        result = 31 * result + myEphemeralMultiplier.hashCode()
        return result
    }

    override fun toString(): String {
        return multipliers.entries.joinToString(", ") { (key, multiplier) ->
            "$key: ($multiplier)"
        }
    }

    companion object {
        fun <T : Number> newFromConstant(constant: NumberSet<T>): NumberVariances<T> =
            newFromMultiplierMap(constant.ind, mapOf(Context.Key.EphemeralKey.new() to constant))

        fun <T : Number> newFromVariance(ind: NumberSetIndicator<T>, key: Context.Key): NumberVariances<T> {
            return newFromMultiplierMap(ind, mapOf(key to ind.onlyOneSet()))
        }

        private fun <T : Number> newFromMultiplierMap(
            ind: NumberSetIndicator<T>,
            multipliers: Map<Context.Key, NumberSet<T>>
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

            if (ephemeralMultiplier.isZero()) {
                // If the ephemeral multiplier is zero, we should just ignore it.
                return NumberVariances(ind, notEphemeralMap)
            }

            return NumberVariances(
                ind,
                notEphemeralMap + (Context.Key.EphemeralKey.new() to ephemeralMultiplier)
            )
        }
    }

    override fun toDebugString(constraints: Constraints): String {
        return if (varsTracking().isEmpty()) {
            collapse(constraints).toString()
        } else {
            multipliers.entries.filter { (k, m) -> !(m.isZero() && k.isAutogenerated()) }
                .joinToString(" + ") { (key, multiplier) ->
                    val multiplierStr = if (multiplier.isOne() && !key.isEphemeral()) "" else "$multiplier"
                    val keyStr = if (key.isEphemeral()) "" else key.toString()
                    val constraint = constraints.getConstraint(ind, key)
                    val constraintStr = if (constraint.isFull()) "" else "[$constraint]"

                    "$multiplierStr$keyStr$constraintStr"
                }
        }
    }

    override fun varsTracking(): Collection<Context.Key> {
        return multipliers.keys.filter { !it.isEphemeral() }
    }

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<T> {
        return newFromMultiplierMap(
            ind,
            multipliers.entries.associate { (key, v) ->
                if (scope.shouldKeep(key)) {
                    key to v
                } else {
                    Context.Key.EphemeralKey.new() to v.multiply(grabConstraint(constraints, key))
                }
            }
        )
    }

    fun grabConstraint(constraints: Constraints, key: Context.Key): NumberSet<T> {
        return if (key.isEphemeral()) {
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
    override fun collapse(constraints: Constraints): NumberSet<T> =
        multipliers.entries.fold(ind.onlyZeroSet()) { acc, (key, multiplier) ->
            acc.add(multiplier.multiply(grabConstraint(constraints, key)))
        }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }
        if (newInd == ind) {
            // Safety: newInd == ind.
            @Suppress("UNCHECKED_CAST")
            return this as Variances<Q>
        }

        fun <OutT : Number> extra(newInd: NumberSetIndicator<OutT>): NumberVariances<OutT>? {
            // If we're only tracking a single variable with a multiplier of 1, and there's no ephemeral offset,
            // we can safely cast ourselves directly to the new indicator.
            if (multipliers.size == 1) {
                if (multipliers.values.first().isOne() && !multipliers.keys.first().isEphemeral()) {
                    return newFromMultiplierMap(newInd, mapOf(multipliers.keys.first() to newInd.onlyOneSet()))
                }
            }

            val q = collapse(constraints).cast(newInd)?.into() ?: return null
            return newFromConstant(q)
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
                val newMultipliers = mutableMapOf<Context.Key, NumberSet<T>>()

                for ((key, meMultiplier) in multipliers) {
                    for ((otherKey, otherMultiplier) in other.multipliers) {
                        val meCollapsed = meMultiplier.multiply(grabConstraint(constraints, key))
                        val otherCollapsed = otherMultiplier.multiply(grabConstraint(constraints, otherKey))

                        val myImportance = key.importance()
                        val otherImportance = otherKey.importance()

                        val newMultiplier = if (myImportance >= otherImportance) {
                            meMultiplier.multiply(otherCollapsed)
                        } else {
                            otherMultiplier.multiply(meCollapsed)
                        }

                        // In regard to multiplying a key with itself, we have two choices:
                        // 1.  We want to ensure the value is positive. The variable by default
                        //     may be negative, so if we want this, we'll have to create a new, ephemeral key,
                        //     losing tracking.
                        //     Well, that or create a brand-new system where we can track special constraints
                        //     within our keys. (Note, could be fun, not completely out the window.)
                        //     Wait, maybe we investigate that idea further, that sounds cool.
                        // 2.  We treat them as two separate keys and collapse one half.
                        //     This allows us to maintain part of the idea that we're still dependent on the variable.
                        //     So that down the road if we multiply by something else that is also dependent on the
                        //     variable, some of that information is kicking around to infer with.

                        val baseKey = if (myImportance >= otherImportance) key else otherKey

                        newMultipliers.compute(baseKey) { _, set ->
                            set?.add(newMultiplier) ?: newMultiplier
                        }
                    }
                }

                return newFromMultiplierMap(ind, newMultipliers)
            }

            DIVISION -> {
                if (other.isOne(constraints)) {
                    return this
                }

                val newMultipliers = mutableMapOf<Context.Key, NumberSet<T>>()

                for ((key, meMultiplier) in multipliers) {
                    for ((otherKey, otherMultiplier) in other.multipliers) {
                        val meCollapsed = meMultiplier.multiply(grabConstraint(constraints, key))
                        val otherCollapsed = otherMultiplier.multiply(grabConstraint(constraints, otherKey))

                        val newValue = meCollapsed.divide(otherCollapsed)

                        newMultipliers.compute(key) { _, set ->
                            set?.add(newValue) ?: newValue
                        }
                    }
                }

                return newFromMultiplierMap(ind, newMultipliers)
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

    override fun comparisonOperation(
        other: Variances<T>,
        comparisonOp: ComparisonOp,
        constraints: Constraints
    ): BooleanVariances {
        // We could maybe do something smarter here long-term, but this'll do for now.
        return collapse(constraints).comparisonOperation(other.into().collapse(constraints), comparisonOp)
            .toConstVariance().into()
    }

    override fun generateConstraintsFrom(
        other: Variances<T>,
        comparisonOp: ComparisonOp,
        incomingConstraints: Constraints
    ): Constraints {
        val other = other.into()
        val allKeys = (multipliers.keys + other.multipliers.keys).filter { !it.isEphemeral() }

        var constraints = Constraints.completelyUnconstrained()

        for (key in allKeys) {
            val myKeyMultiplier = multipliers[key] ?: ind.onlyZeroSet()
            val otherKeyMultiplier = other.multipliers[key] ?: ind.onlyZeroSet()

            // Move all the keys onto the left
            val coefficient = myKeyMultiplier.subtract(otherKeyMultiplier)

            val lhsConstant =
                newFromMultiplierMap(ind, multipliers.filter { it.key != key })
                    .collapse(incomingConstraints)

            val rhsConstant =
                newFromMultiplierMap(ind, other.multipliers.filter { it.key != key })
                    .collapse(incomingConstraints)

            val constant = rhsConstant.subtract(lhsConstant)

            if (coefficient.isZero()) {
                // The key has cancelled itself out, this is now an all-or-nothing situation:
                // either the constraint is met, or it isn't.
                // The equation at this point looks like `0x blah constant`
                // So we can just check the constant against zero.
                val meetsConstraint = ind.onlyZeroSet().comparisonOperation(constant, comparisonOp)
                constraints = when (meetsConstraint) {
                    BooleanSet.BOTH, BooleanSet.TRUE -> constraints.withConstraint(key, key.ind.newFullSet())
                    BooleanSet.FALSE, BooleanSet.NEITHER -> constraints.withConstraint(key, key.ind.newEmptySet())
                }
            } else {
                val shouldFlip = coefficient.comparisonOperation(ind.onlyZeroSet(), ComparisonOp.LESS_THAN)
                val rhs = constant.divide(coefficient)
                val constraint = when (shouldFlip) {
                    BooleanSet.TRUE -> rhs.getSetSatisfying(comparisonOp.flip())
                    BooleanSet.FALSE -> rhs.getSetSatisfying(comparisonOp)
                    BooleanSet.BOTH -> rhs.getSetSatisfying(comparisonOp)
                        .union(rhs.getSetSatisfying(comparisonOp.flip()))

                    BooleanSet.NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                }

                // I'm slightly unsure about this cast.
                constraints = constraints.withConstraint(key, constraint.clampCast(key.ind)!!)
            }
        }

        return constraints
    }

    fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.CollectedTerms<T>,
        valid: NumberSet<T>
    ): NumberVariances<T> = TODO("Not yet implemented")
}