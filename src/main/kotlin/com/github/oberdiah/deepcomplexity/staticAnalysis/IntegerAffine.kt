package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.half
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.utilities.BigFraction
import com.github.oberdiah.deepcomplexity.utilities.BigFraction.of
import com.github.oberdiah.deepcomplexity.utilities.BigFraction.ZERO
import java.math.BigInteger

/**
 * A neat little class that represents an integer affine.
 */
@ConsistentCopyVisibility
data class IntegerAffine private constructor(
    private val center: BigFraction,
    private val noiseTerms: Map<Context.Key, AffineNoiseTerm>,
) {
    // The minimum and the maximum are *post*-multiplication. Obviously. The underlying var only goes between 0 and 1.
    // You can imagine this as the original variable only going between -1 to 1, and then multi takes that up to the
    // full range of the variable.
    // That is, they're immediately comparable with other terms of the same key.
    class AffineNoiseTerm(val multi: BigFraction, val min: BigFraction, val max: BigFraction) {
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
                val (myScaledMin, myScaledMax) = lhs.min / lhs.multi to lhs.max / lhs.multi
                val (otherScaledMin, otherScaledMax) = rhs.min / rhs.multi to rhs.max / rhs.multi

                val newScaledMin = myScaledMin.max(otherScaledMin)
                val newScaledMax = myScaledMax.min(otherScaledMax)

                val newLhsMin = newScaledMin * lhs.multi
                val newLhsMax = newScaledMax * lhs.multi
                val newRhsMin = newScaledMin * rhs.multi
                val newRhsMax = newScaledMax * rhs.multi

                return Pair(
                    AffineNoiseTerm(lhs.multi, newLhsMin, newLhsMax),
                    AffineNoiseTerm(rhs.multi, newRhsMin, newRhsMax)
                )
            }
        }

        init {
            require(multi >= ZERO) { "Multi ($multi) must be non-negative" }
            require(min <= max) { "Min ($min) must be less than or equal to max ($max)" }
            require(min >= multi.negate()) {
                "Min ($min) must be greater than or equal to negative multi ($multi) ${multi.negate()} ${min >= multi.negate()}"
            }
            require(max <= multi) { "Max($max) must be less than or equal to multi ($multi)" }
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
            val newMin = if (shouldAdd) newMe.min + newOther.min else newMe.min - newOther.min
            val newMax = if (shouldAdd) newMe.max + newOther.max else newMe.max - newOther.max

            return AffineNoiseTerm(newRange, newMin, newMax)
        }

        fun multBy(center: BigFraction): AffineNoiseTerm {
            return AffineNoiseTerm(multi * center, min * center, max * center)
        }
    }

    companion object {
        fun fromConstant(constant: Long): IntegerAffine =
            IntegerAffine(of(constant), emptyMap())

        fun fromRangeNoKey(start: Long, end: Long): IntegerAffine {
            val key = Context.Key.EphemeralKey.new()

            val center = (of(start) + of(end)).half()
            val radius = (of(end) - of(start)).half()
            val affine = IntegerAffine(
                center,
                mapOf(key to AffineNoiseTerm(radius, radius.negate(), radius)),
            )

            return affine
        }

        /**
         * When calling this it's obviously assumed that the start and end values constrain the variable
         * directly, rather than after any transformations.
         */
        fun fromConstraints(ind: NumberSetIndicator<*, *>, start: Long, end: Long, key: Context.Key): IntegerAffine {
            val maxInd = ind.getMaxValue().toLong()
            val minInd = ind.getMinValue().toLong()
            val center: BigFraction = (of(start) + of(end)).half()

            val multi: BigFraction = (of(maxInd) - of(minInd)).half()
            // might be + instead
            val noiseMin = of(start) - center
            val noiseMax = of(end) - center

            return IntegerAffine(
                center,
                mapOf(key to AffineNoiseTerm(multi, noiseMin, noiseMax))
            )
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

        require(lower.denominator.toLong() == 1L)
        require(upper.denominator.toLong() == 1L)

        return Pair(lower.numerator, upper.numerator)
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
    fun subtract(other: IntegerAffine): IntegerAffine? {
        return addOrSubtract(other, shouldAdd = false)
    }

    /**
     * Returns null if the addition could not have occurred based on the constraints
     * we have.
     */
    fun add(other: IntegerAffine): IntegerAffine? {
        return addOrSubtract(other, shouldAdd = true)
    }

    private fun addOrSubtract(other: IntegerAffine, shouldAdd: Boolean): IntegerAffine? {
        val newCenter = if (shouldAdd) center + other.center else center - other.center
        val newNoiseTerms = mutableMapOf<Context.Key, AffineNoiseTerm>()

        for (key in noiseTerms.keys.union(other.noiseTerms.keys)) {
            val lhs = noiseTerms[key]
            val rhs = other.noiseTerms[key]
            newNoiseTerms[key] = let {
                if (lhs == null) {
                    rhs!!
                } else if (rhs == null) {
                    lhs
                } else {
                    val additionResult = lhs.addOrSubtractLikeTerms(rhs, shouldAdd)
                    if (additionResult == null) return null
                    additionResult
                }
            }
        }

        return IntegerAffine(newCenter, newNoiseTerms)
    }

    fun multiply(other: IntegerAffine): IntegerAffine? {
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
                    AffineNoiseTerm.updateMinMaxForLikeTerms(noiseTerms, other.noiseTerms) ?: return null

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

                AffineNoiseTerm(newRange, newQuadraticMin, newQuadraticMax)
            }

            val key = Context.Key.EphemeralKey("Quadratic")
            newNoiseTerms[key] = newNoiseTerms[key]?.addOrSubtractLikeTerms(quadraticTerm, true) ?: quadraticTerm
        }

        val finalAffine = IntegerAffine(newCenter, newNoiseTerms)

        return finalAffine
    }
}
