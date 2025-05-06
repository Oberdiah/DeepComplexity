package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.half
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import com.github.oberdiah.deepcomplexity.utilities.BigFraction
import com.github.oberdiah.deepcomplexity.utilities.BigFraction.ZERO
import com.github.oberdiah.deepcomplexity.utilities.BigFraction.of
import java.math.BigInteger
import java.math.RoundingMode

/**
 * A neat little class that represents an integer affine.
 */
@ConsistentCopyVisibility
data class IntegerAffine private constructor(
    private val center: BigFraction,
    private val noiseTerms: Map<Context.Key, AffineNoiseTerm>,
) {
    // The minimum and the maximum are pre-multiplication. (e.g. they're always between -1 and 1)
    // This is conceptually far less confusing
    class AffineNoiseTerm private constructor(
        val multi: BigFraction,
        val normalizedMin: BigFraction,
        val normalizedMax: BigFraction
    ) {
        // The min and max are post-multiplication.
        val min
            get() = if (multi > ZERO) multi * normalizedMin else multi * normalizedMax

        val max
            get() = if (multi > ZERO) multi * normalizedMax else multi * normalizedMin

        companion object {
            fun updateMinMaxForLikeTerms(
                lhs: Map<Context.Key, AffineNoiseTerm>,
                rhs: Map<Context.Key, AffineNoiseTerm>
            ): Pair<Map<Context.Key, AffineNoiseTerm>, Map<Context.Key, AffineNoiseTerm>>? {
                var lhsOutMap = mutableMapOf<Context.Key, AffineNoiseTerm>()
                var rhsOutMap = mutableMapOf<Context.Key, AffineNoiseTerm>()
                for (key in lhs.keys + rhs.keys) {
                    if (key in lhs && key in rhs) {
                        val (newLhs, newRhs) =
                            updateMinMaxFromLikeTerms(lhs[key]!!, rhs[key]!!) ?: return null
                        lhsOutMap[key] = newLhs
                        rhsOutMap[key] = newRhs
                    } else if (key in lhs) {
                        lhsOutMap[key] = lhs[key]!!
                    } else if (key in rhs) {
                        rhsOutMap[key] = rhs[key]!!
                    }
                }

                return Pair(lhsOutMap, rhsOutMap)
            }

            /**
             * Takes two Affine Noise Terms and returns two new ones with their min and maxes updated
             * to their intersection.
             *
             * This assumes the key for both affines was the same.
             */
            fun updateMinMaxFromLikeTerms(
                lhs: AffineNoiseTerm,
                rhs: AffineNoiseTerm
            ): Pair<AffineNoiseTerm, AffineNoiseTerm>? {
                val newNormalizedMin = lhs.normalizedMin.max(rhs.normalizedMin)
                val newNormalizedMax = lhs.normalizedMax.min(rhs.normalizedMax)

                if (newNormalizedMin > newNormalizedMax) {
                    return null
                }

                return Pair(
                    AffineNoiseTerm(lhs.multi, newNormalizedMin, newNormalizedMax),
                    AffineNoiseTerm(rhs.multi, newNormalizedMin, newNormalizedMax)
                )
            }

            fun fromRadius(radius: BigFraction): AffineNoiseTerm {
                return AffineNoiseTerm(radius, BigFraction.ONE.negate(), BigFraction.ONE)
            }

            fun constrainedRadius(radius: BigFraction, min: BigFraction, max: BigFraction): AffineNoiseTerm {
                return AffineNoiseTerm(radius, min / radius, max / radius)
            }
        }

        init {
            require(min <= max) { "Min ($min) must be less than or equal to max ($max)" }
            require(normalizedMin >= BigFraction.ONE.negate()) {
                "Unscaled min ($normalizedMin) must be greater than or equal to -1"
            }
            require(normalizedMax <= BigFraction.ONE) {
                "Unscaled max ($normalizedMax) must be less than or equal to 1"
            }
        }

        override fun toString(): String {
            return "±$multi [$min, $max]"
        }

        fun grab(sign: Int): BigFraction = when (sign) {
            -1 -> min
            1 -> max
            else -> throw IllegalArgumentException("Sign must be -1 or 1")
        }

        /**
         * This implicitly assumes that both this and other come from the same key.
         *
         * Returns null if this addition could not have occurred.
         */
        fun addOrSubtractLikeTerms(other: AffineNoiseTerm, shouldAdd: Boolean): AffineNoiseTerm? {
            val (newMe, newOther) = updateMinMaxFromLikeTerms(this, other) ?: return null

            val newRange = if (shouldAdd) multi + other.multi else multi - other.multi

            return AffineNoiseTerm(newRange, newMe.normalizedMin, newMe.normalizedMax)
        }

        fun multBy(center: BigFraction): AffineNoiseTerm {
            return AffineNoiseTerm(multi * center, normalizedMin, normalizedMax)
        }
    }

    companion object {
        fun fromConstant(constant: Long): IntegerAffine =
            IntegerAffine(of(constant), emptyMap())

        fun fromRange(start: Long, end: Long, key: Context.Key): IntegerAffine {
            val center = (of(start) + of(end)).half()
            val radius = (of(end) - of(start)).half()
            val affine = IntegerAffine(
                center,
                mapOf(key to AffineNoiseTerm.fromRadius(radius)),
            )

            return affine
        }
    }

    fun getKeys(): List<Context.Key> = noiseTerms.keys.toList()

    fun stringOverview(): String {
        val (min, max) = toRange()

        if (min == max) {
            return "$min"
        }

        return "$min..$max (${noiseTerms.keys})"
    }

    override fun toString(): String {
        val (min, max) = toRange()
        return "$min..$max ($center + ${
            noiseTerms.entries.joinToString(" + ") { "${it.value.multi} * ${it.key}" }
        })"
    }

    fun toRange(): Pair<BigInteger, BigInteger> {
        val negRadius = noiseTerms.values.fold(ZERO) { acc, it -> acc + it.min }
        val posRadius = noiseTerms.values.fold(ZERO) { acc, it -> acc + it.max }
        val lower = (center + negRadius)
        val upper = (center + posRadius)

        val lowerRounded = lower.bigDecimalValue().setScale(0, RoundingMode.FLOOR).toBigIntegerExact()
        val upperRounded = upper.bigDecimalValue().setScale(0, RoundingMode.CEILING).toBigIntegerExact()

        return Pair(lowerRounded, upperRounded)
    }

    fun isExactly(i: Int): Boolean {
        val (lower, upper) = toRange()
        val v = BigInteger.valueOf(i.toLong())
        return lower == v && upper == v
    }

    /**
     * Returns null if the addition could not have occurred based on the constraints
     * we have.
     */
    fun subtract(other: IntegerAffine): IntegerAffine {
        return addOrSubtract(other, shouldAdd = false)
    }

    /**
     * Returns null if the addition could not have occurred based on the constraints
     * we have.
     */
    fun add(other: IntegerAffine): IntegerAffine {
        return addOrSubtract(other, shouldAdd = true)
    }

    private fun addOrSubtract(other: IntegerAffine, shouldAdd: Boolean): IntegerAffine {
        val newCenter = if (shouldAdd) center + other.center else center - other.center
        val newNoiseTerms = mutableMapOf<Context.Key, AffineNoiseTerm>()

        for (key in noiseTerms.keys.union(other.noiseTerms.keys)) {
            val lhs = noiseTerms[key]
            val rhs = other.noiseTerms[key]
            newNoiseTerms[key] = let {
                if (lhs == null) {
                    if (shouldAdd) rhs!! else rhs!!.multBy(of(-1))
                } else if (rhs == null) {
                    lhs
                } else {
                    val additionResult = lhs.addOrSubtractLikeTerms(rhs, shouldAdd)
                    if (additionResult == null) TODO("Hopefully this won't happen until we remove this feature")
                    additionResult
                }
            }
        }

        return IntegerAffine(newCenter, newNoiseTerms)
    }

    fun multiply(other: IntegerAffine): IntegerAffine {
        // Multiply centers
        val newCenter = center * other.center

        val newNoiseTerms = mutableMapOf<Context.Key, AffineNoiseTerm>()

        // Handle center * noise terms
        for ((key, value) in other.noiseTerms) {
            newNoiseTerms[key] = value.multBy(center)
        }
        for ((key, value) in noiseTerms) {
            val newTerm = value.multBy(other.center)
            newNoiseTerms[key] = newNoiseTerms[key]?.addOrSubtractLikeTerms(newTerm, true) ?: newTerm
        }

        // Don't need to worry about noise * noise if one of them is empty.
        if (!noiseTerms.isEmpty() && !other.noiseTerms.isEmpty()) {
            val quadraticTerm = let {
                val (myNoise, otherNoise) =
                    AffineNoiseTerm.updateMinMaxForLikeTerms(noiseTerms, other.noiseTerms)
                        ?: TODO("Hopefully this won't happen until we remove this feature")

                val myRange = noiseTerms.values.fold(ZERO) { acc, it -> acc + it.multi }
                val myMin = myNoise.values.fold(ZERO) { acc, it -> acc + it.min }
                val myMax = myNoise.values.fold(ZERO) { acc, it -> acc + it.max }

                val otherRange = other.noiseTerms.values.fold(ZERO) { acc, it -> acc + it.multi }
                val otherMin = otherNoise.values.fold(ZERO) { acc, it -> acc + it.min }
                val otherMax = otherNoise.values.fold(ZERO) { acc, it -> acc + it.max }

                val myMinOtherMin = myMin * otherMin
                val myMinOtherMax = myMin * otherMax
                val myMaxOtherMin = myMax * otherMin
                val myMaxOtherMax = myMax * otherMax

                val newMin = myMinOtherMin.min(myMinOtherMax).min(myMaxOtherMin).min(myMaxOtherMax)
                val newMax = myMinOtherMin.max(myMinOtherMax).max(myMaxOtherMin).max(myMaxOtherMax)
                val newRange = myRange * otherRange

                // Solving even simple affines isn't possible to do easily generally, as seen by
                // `min (x + 5y) * (3x + 6y) subject to -10 <= x <= 10, -10 <= y <= 10` which has two
                // troughs at y=-7/2 and y=7/2.

                // However, what you can do is assume you've got independent noise terms
                // (If you don't you're going to just make your bound better, never worse)
                // and figure out what would need to be what for min and max and then
                // calculate those ahead-of-time for multiplication.

                // Step 1: For min and max, figure out which needs to be negative and which needs to be positive.
                val myMinSign = if (newMin == myMinOtherMin || newMin == myMinOtherMax) -1 else 1
                val otherMinSign = if (newMin == myMinOtherMin || newMin == myMaxOtherMin) -1 else 1
                val myMaxSign = if (newMax == myMaxOtherMin || newMax == myMaxOtherMax) 1 else -1
                val otherMaxSign = if (newMax == myMinOtherMax || newMax == myMaxOtherMax) 1 else -1

                // Step 2: Now we know what the signs of our terms are, we can multiply them.
                var newQuadraticMin = ZERO
                var newQuadraticMax = ZERO
                for (myNoise in myNoise.values) {
                    for (otherNoise in otherNoise.values) {
                        newQuadraticMin += myNoise.grab(myMinSign) * otherNoise.grab(otherMinSign)
                        newQuadraticMax += myNoise.grab(myMaxSign) * otherNoise.grab(otherMaxSign)
                    }
                }

                AffineNoiseTerm.constrainedRadius(newRange, newQuadraticMin, newQuadraticMax)
            }

            val key = Context.Key.EphemeralKey("Quadratic")
            newNoiseTerms[key] = newNoiseTerms[key]?.addOrSubtractLikeTerms(quadraticTerm, true) ?: quadraticTerm
        }

        val finalAffine = IntegerAffine(newCenter, newNoiseTerms)

        return finalAffine
    }
}
