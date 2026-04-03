package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.BigIntegerIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberRange
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.oberdiah.deepcomplexity.utilities.Functional
import com.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.oberdiah.deepcomplexity.utilities.Utilities.ceilDiv
import com.oberdiah.deepcomplexity.utilities.Utilities.floorDiv
import com.oberdiah.deepcomplexity.utilities.Utilities.getSetSize
import com.oberdiah.deepcomplexity.utilities.Utilities.toBigInteger
import com.oberdiah.deepcomplexity.utilities.into
import java.math.BigInteger

/**
 * Think of this as a linear equation like the form: `2x + 3y + 4z + c`
 * where the keys are the variables (x, y, z) and the values are the multipliers (2, 3, 4).
 * The constant (c) is represented by the Constant key.
 *
 * Now, we don't store the constraints here; they come in from outside, so we can't calculate a full
 * range from this alone. So intuitively, when we want to collapse, we take the constraints for each key
 * and multiply them by the multiplier, and then add them all together.
 *
 * When operations are performed, both variances must have the same indicator. That's just how operations
 * work in Java. Any variance that is legal before the operation should be legal after without too much fuss.
 *
 * We're considering NumberVariances a whole-number-only zone for now, so we first cast everything into BigInteger.
 *
 * A NumberVariances instance collapses by first turning everything into big integers, then multiplying and adding over those
 * big integers, and then finally squishing down into the type.
 *
 * What this means for casting (here x is an integer):
 * `(short) x`        -> A simple type swap is fine, NumberVariances<Short>(1*int) is ok.
 *                       The int comes in and becomes a `short` during collapse, no problems there.
 * `(short) 2x`       -> Fine as well. NumberVariances<Short>(2*int) also contains all the information required
 *                       for a full reconstruction.
 * `(long) x`         -> Fine too, for a similar reason. This is starting to get risky, though, as we can only do this
 *                       if the underlying equation couldn't get beyond its old bounds (int). If it could,
 *                       we could no longer do this. For example,
 * `(long) 2x`        -> Must be collapsed. Converting this naively to NumberVariances<Long>(2*int) would change its
 *                       meaning.
 * `(int) ((byte) x)` -> Big problem, must be collapsed. Can't possibly be represented.
 *
 * In summary, if the underlying equation could get beyond the bounds we previously had it under, and we're
 * growing in size, we must collapse.
 */
class NumberVariances<T : Number> private constructor(
    override val ind: NumberIndicator<T>,
    multipliers: Map<EvaluationKey<*>, NumberSet<BigInteger>> = mapOf()
) : Variances<T> {
    private val multipliers: Map<EvaluationKey<*>, NumberSet<BigInteger>> =
        mapOf(EvaluationKey.ConstantKey to BigIntegerIndicator.onlyZeroSet()) +
                multipliers.filter { !it.value.isZero() }

    init {
        require(this.multipliers.size < 10) {
            "Too many multipliers in NumberVariances: ${multipliers.size}. " +
                    "This isn't a hard limit, but it's questionable from a performance perspective."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NumberVariances<*>) return false
        if (ind != other.ind) return false
        if (multipliers != other.multipliers) return false
        return true
    }

    override fun hashCode(): Int = 31 * ind.hashCode() + multipliers.hashCode()

    override fun toString(): String {
        return multipliers.entries.joinToString(", ") { (key, multiplier) ->
            "$key: ($multiplier)"
        }
    }

    companion object {
        fun <T : Number> newFromConstant(constant: NumberSet<T>): NumberVariances<T> =
            NumberVariances(
                constant.ind,
                mapOf(EvaluationKey.ConstantKey to constant.castToNumber(BigIntegerIndicator))
            )

        fun <T : Number> newFromVariance(ind: NumberIndicator<T>, key: EvaluationKey<*>): NumberVariances<T> =
            NumberVariances(ind, mapOf(key to BigIntegerIndicator.onlyOneSet()))
    }

    override fun toDebugString(constraints: Constraints): String {
        return if (varsTracking().isEmpty()) {
            multipliers[EvaluationKey.ConstantKey]?.toString() ?: "0L"
        } else {
            multipliers.entries
                .filter { (k, m) -> !(m.isZero() && k.isAutogenerated()) }
                .joinToString(" + ") { (key, multiplier) ->
                    if (key.isConstant()) {
                        "${multiplier}L"
                    } else {
                        val multiplierStr = if (multiplier.isOne()) "" else "$multiplier"
                        val constraint = grabConstraint(constraints, key)
                        val constraintStr = if (constraint.isFull()) "" else "[$constraint]"

                        "$multiplierStr${key}$constraintStr"
                    }
                }
        }
    }

    override fun varsTracking(): Collection<EvaluationKey<*>> {
        return multipliers.keys.filter { !it.isConstant() }
    }

    fun grabConstraint(constraints: Constraints, key: EvaluationKey<*>): NumberSet<BigInteger> {
        return if (key.isConstant()) {
            // Constants don't have any variance or constraints,
            // so the 'variable', if you can call it that, is always constrained to exactly 1.
            BigIntegerIndicator.onlyOneSet()
        } else {
            constraints.getConstraint(key).castTo(BigIntegerIndicator).into()
        }
    }

    /**
     * Collapse the multipliers into a single set of values.
     * Requires constraints because a NumberVariance on its own doesn't contain
     * all the information it needs alone.
     */
    override fun collapse(constraints: Constraints): NumberSet<T> =
        collapseWithoutLimits(constraints).castToNumber(ind)

    private fun collapseWithoutLimits(constraints: Constraints): NumberSet<BigInteger> =
        multipliers.entries.fold(BigIntegerIndicator.onlyZeroSet()) { acc, (key, multiplier) ->
            acc.add(multiplier.multiply(grabConstraint(constraints, key)))
        }

    override fun <Q : Any> attemptHardCastTo(
        newInd: Indicator<Q>,
        constraints: Constraints
    ): Variances<Q>? {
        if (newInd !is NumberIndicator<*>) {
            return null
        }
        if (newInd == ind) {
            // Safety: newInd == ind.
            @Suppress("UNCHECKED_CAST")
            return this as Variances<Q>
        }

        fun <OutT : Number> extra(newInd: NumberIndicator<OutT>): NumberVariances<OutT>? {
            if (ind.canSafelyContain(newInd)) {
                // If we've shrunken down, we don't need to do anything, and we can continue as-is.
                return NumberVariances(newInd, multipliers)
            } else {
                // Ok, so we're being asked to grow (e.g. from a short to an int)

                // Is it possible that our pre-cast setup could have overflowed its old bounds?
                if (isSafelyWithinBounds(constraints)) {
                    // No risk of overflow, we can just do a type swap and be done with it.
                    return NumberVariances(newInd, multipliers)
                } else {
                    // Unfortunately, we have to collapse :(
                    val collapsedSet = collapse(constraints).tryCastTo(newInd)?.into() ?: return null
                    return newFromConstant(collapsedSet)
                }
            }
        }

        @Suppress("UNCHECKED_CAST") // Safety: Trivially true by checking the signature of extra().
        return extra(newInd) as Variances<Q>?
    }

    /**
     * Returns true if this variance has no chance of overflowing its indicator's bounds and can be
     * safely cast to a larger indicator.
     */
    fun isSafelyWithinBounds(constraints: Constraints): Boolean {
        return collapseWithoutLimits(constraints).getRange().canFitWithin(ind.getTotalRange())
    }

    fun isOne(constraints: Constraints): Boolean = collapse(constraints).isOne()

    fun negate(): NumberVariances<T> {
        return NumberVariances(ind, multipliers.mapValues { (_, multiplier) ->
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
                return NumberVariances(
                    ind, Functional.mergeMapsWithBlank(
                        multipliers,
                        other.multipliers,
                        BigIntegerIndicator.onlyZeroSet()
                    ) { mySet, otherSet ->
                        if (operation == ADDITION) {
                            mySet.add(otherSet)
                        } else {
                            mySet.subtract(otherSet)
                        }
                    })
            }

            MULTIPLICATION -> {
                val newMultipliers = mutableMapOf<EvaluationKey<*>, NumberSet<BigInteger>>()

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
                        //     may be negative, so if we want this, we'll have to shove it into a constant,
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

                return NumberVariances(ind, newMultipliers)
            }

            DIVISION -> {
                if (other.isOne(constraints)) {
                    return this
                }

                val meCollapsed = this.collapse(constraints)
                val otherCollapsed = other.collapse(constraints)

                // I don't think we can do any better, at least for whole numbers.
                // If you imagine we have 1x, and we're dividing by 2, there's really nothing we can do in that
                // situation other than collapse.

                return newFromConstant(
                    meCollapsed.divide(otherCollapsed)
                )
            }

            MODULO, MINIMUM, MAXIMUM -> {
                // In theory, there may be some edge cases where we can retain variable tracking through
                // a minimum or maximum, but I'm not going to consider them unless they come up.
                // In the general case it's not doable as we don't have max or min features built into the variance.
                val meCollapsed = this.collapse(constraints)
                val otherCollapsed = other.collapse(constraints)

                return newFromConstant(
                    meCollapsed.arithmeticOperation(otherCollapsed, operation)
                )
            }
        }
    }

    /**
     * Given a comparison e.g. 3x + 2y + 5 < 2x + 7,
     * generate constraints on x and y that would allow this comparison to be satisfied.
     * It does this by rearranging the equation to the form `ax op c` for every key found, where `a` is the multiplier
     * of one of the keys, and `c` is a constant representing the rest. All variables in this equation are sets.
     * Once we have that form, we divide both sides by `a` to get `x op c/a`, and then we can generate a
     * constraint for x based on the result of that division.
     */
    override fun generateConstraintsFrom(
        other: Variances<T>,
        comparisonOp: ComparisonOp,
        constraints: Constraints
    ): Constraints {
        require(ind == other.ind) {
            "Cannot generate constraints from a variance of a different type. ($ind vs ${other.ind})"
        }

        val other = other.into()
        val allKeys = (multipliers.keys + other.multipliers.keys).filter { !it.isConstant() }

        if (allKeys.isEmpty()) {
            return super.generateConstraintsFrom(other, comparisonOp, constraints)
        }

        return allKeys.fold(constraints) { constraints, key ->
            constraints.withConstraint(
                key, generateConstraintFor(
                    key.coerceToNumbers(),
                    other,
                    comparisonOp,
                    constraints
                )
            )
        }
    }

    private fun <Q : Number> generateConstraintFor(
        key: EvaluationKey<Q>,
        other: NumberVariances<T>,
        op: ComparisonOp,
        constraints: Constraints
    ): NumberSet<Q> {
        val keyInd = key.ind.into()
        val zeroSet = BigIntegerIndicator.onlyZeroSet()

        val leftKeyMultiplier = multipliers[key] ?: zeroSet
        val rightKeyMultiplier = other.multipliers[key] ?: zeroSet

        val leftConstant =
            NumberVariances(ind, multipliers.filter { it.key != key }).collapseWithoutLimits(constraints)
        val rightConstant =
            NumberVariances(ind, other.multipliers.filter { it.key != key }).collapseWithoutLimits(constraints)

        val isSimpleSolve = isSafelyWithinBounds(constraints) && other.isSafelyWithinBounds(constraints)

        if (isSimpleSolve) {
            // Get into the form `a*x op c`. It is only OK to rearrange like this when there is definitely no wrapping

            // Move all of our keys to the left
            val coefficient = leftKeyMultiplier.subtract(rightKeyMultiplier)
            // Move all of our non-key 'constants' to the right.
            val constant = rightConstant.subtract(leftConstant)

            if (coefficient.isZero()) {
                // This is now an all-or-nothing situation; either the constraint is met, or it isn't.
                // The equation looks like `0x op c`, so we can just check the constant against zero.
                return when (zeroSet.comparisonOperation(constant, op)) {
                    BooleanSet.EITHER, BooleanSet.TRUE -> keyInd.newFullSet()
                    BooleanSet.FALSE, BooleanSet.NEITHER -> keyInd.newEmptySet()
                }
            } else {
                val shouldFlip = coefficient.comparisonOperation(zeroSet, ComparisonOp.LESS_THAN)
                
                val rhs = constant.divide(coefficient).castTo(ind).into()
                val constraint = when (shouldFlip) {
                    BooleanSet.TRUE -> rhs.getSetSatisfying(op.flip())
                    BooleanSet.FALSE -> rhs.getSetSatisfying(op)
                    BooleanSet.EITHER -> rhs.getSetSatisfying(op).union(rhs.getSetSatisfying(op.flip()))
                    BooleanSet.NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                }

                return constraint.clampCast(keyInd)!!.into()
            }
        }

        val currentConstraint = constraints.getConstraint(key).coerceTo(keyInd).into()
        if (currentConstraint.ranges.size != 1) {
            return keyInd.newFullSet()
        }

        val (coefficient, constant, target) = when {
            leftKeyMultiplier.isSingleValue() && rightKeyMultiplier.isZero() -> Triple(
                leftKeyMultiplier.getSingleValue()!!,
                NumberVariances(ind, multipliers.filter { it.key != key }).collapseWithoutLimits(constraints),
                NumberVariances(ind, other.multipliers.filter { it.key != key }).collapse(constraints)
                    .getSetSatisfying(op)
            )

            leftKeyMultiplier.isZero() && rightKeyMultiplier.isSingleValue() -> Triple(
                rightKeyMultiplier.getSingleValue()!!,
                NumberVariances(ind, other.multipliers.filter { it.key != key }).collapseWithoutLimits(constraints),
                NumberVariances(ind, multipliers.filter { it.key != key }).collapse(constraints)
                    .getSetSatisfying(op.flip())
            )

            else -> return keyInd.newFullSet()
        }

        if (target.isFull() || coefficient == BigInteger.ZERO || constant.ranges.size > 2 || target.ranges.size > 2) {
            return currentConstraint
        }

        val xRange = currentConstraint.ranges.single()
        val xMin = xRange.start.toBigInteger()
        val xMax = xRange.end.toBigInteger()
        val axMin = minOf(coefficient * xMin, coefficient * xMax)
        val axMax = maxOf(coefficient * xMin, coefficient * xMax)
        val modulus = ind.clazz.getSetSize()
        var out = NumberSet.newEmpty(keyInd)

        for (constantRange in constant.ranges) for (targetRange in target.castToNumber(BigIntegerIndicator).ranges) {
            val low = targetRange.start - constantRange.end
            val high = targetRange.end - constantRange.start
            val kStart = (axMin - high).ceilDiv(modulus)
            val kEnd = (axMax - low).floorDiv(modulus)

            var k = kStart
            while (k <= kEnd) {
                val shiftedLow = low + k * modulus
                val shiftedHigh = high + k * modulus
                val start =
                    if (coefficient > BigInteger.ZERO)
                        shiftedLow.ceilDiv(coefficient)
                    else
                        (-shiftedHigh).ceilDiv(-coefficient)

                val end =
                    if (coefficient > BigInteger.ZERO)
                        shiftedHigh.floorDiv(coefficient)
                    else
                        (-shiftedLow).floorDiv(-coefficient)

                if (start <= end) {
                    val clampedStart = maxOf(start, xMin)
                    val clampedEnd = minOf(end, xMax)
                    if (clampedStart <= clampedEnd) {
                        if (out.ranges.size >= NumberSet.MAX_RANGES) {
                            return currentConstraint
                        }
                        out = out.union(
                            NumberSet.newFromRange(
                                NumberRange.new(
                                    clampedStart.castInto(keyInd.clazz),
                                    clampedEnd.castInto(keyInd.clazz)
                                )
                            )
                        )
                    }
                }
                k += BigInteger.ONE
            }
        }

        return out
    }
}
