package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.orElse
import java.math.BigInteger
import java.math.BigInteger.TWO
import java.math.BigInteger.ZERO
import java.math.BigInteger.valueOf

/**
 * A neat little class that represents an integer affine.
 */
@ConsistentCopyVisibility
data class IntegerAffine private constructor(
    // This represents twice the constant term. This allows us to represent ranges like [1, 2]
    // where the center is 1.5. Any variable that starts with d is doubled.
    private val dCenter: BigInteger,
    private val noiseTerms: Map<Context.Key, AffineNoiseTerm>,
) {
    // The way to think of this is the range is the full range the unknown could be.
    // The min is any constraints we've applied to the unknown to make it smaller, vice versa with max.
    // Max & min will always be in the interval [-range, range].
    // It is important to remember that a min/max means that the underlying key can never be less/greater than that
    // value in this situation. It's not a 'this is clamped between these', it's a hard limit.
    // If we try to do x[1, 5] + x[7, 10], that isn't possible and results in the empty set.
    //
    // Remember: this represents twice the noise terms. The range, min, and max are all doubled.
    // This allows us to represent ranges like [1, 2]
    class AffineNoiseTerm(val dRange: BigInteger, val dMin: BigInteger, val dMax: BigInteger) {
        companion object {
            fun findDMin(vs: Collection<AffineNoiseTerm>): BigInteger =
                vs.fold(ZERO) { acc, it -> acc + it.dMin }

            fun findDMax(vs: Collection<AffineNoiseTerm>): BigInteger =
                vs.fold(ZERO) { acc, it -> acc + it.dMax }

            fun findDRange(vs: Collection<AffineNoiseTerm>): BigInteger =
                vs.fold(ZERO) { acc, it -> acc + it.dRange }

            val zero = AffineNoiseTerm(ZERO, ZERO, ZERO)
        }

        override fun toString(): String {
            return "±${formatV(dRange)} [${formatV(dMin)}, ${formatV(dMax)}]"
        }

        fun grab(sign: Int): BigInteger = when (sign) {
            -1 -> dMin
            1 -> dMax
            else -> throw IllegalArgumentException("Sign must be -1 or 1")
        }

        fun addOrSubtract(other: AffineNoiseTerm, shouldAdd: Boolean): AffineNoiseTerm {
            val newDRange = if (shouldAdd) dRange + other.dRange else dRange - other.dRange
            val newDMin = if (shouldAdd) dMin + other.dMin else dMin - other.dMin
            val newDMax = if (shouldAdd) dMax + other.dMax else dMax - other.dMax
            return AffineNoiseTerm(newDRange, newDMin.min(newDMax), newDMin.max(newDMax))
        }

        fun multByDCenter(dCenter: BigInteger): AffineNoiseTerm {
            // Divide by two because 2 * 2 = 4 so we need to compensate back to two.
            return AffineNoiseTerm(dRange * dCenter / TWO, dMin * dCenter / TWO, dMax * dCenter / TWO)
        }

        fun mult(other: AffineNoiseTerm): AffineNoiseTerm {
            val newDRange = dRange * other.dRange / TWO
            val dMinA = dMin * other.dMin / TWO
            val dMinB = dMin * other.dMax / TWO
            val dMaxA = dMax * other.dMin / TWO
            val dMaxB = dMax * other.dMax / TWO
            val newDMin = dMinA.min(dMinB).min(dMaxA).min(dMaxB)
            val newDMax = dMinA.max(dMinB).max(dMaxA).max(dMaxB)

            return AffineNoiseTerm(newDRange, newDMin, newDMax)
        }

        fun constrainDMin(dMin: BigInteger): AffineNoiseTerm =
            if (dMin > this.dMin) AffineNoiseTerm(dRange, dMin, dMax) else this

        fun constrainDMax(dMax: BigInteger): AffineNoiseTerm =
            if (dMax < this.dMax) AffineNoiseTerm(dRange, dMin, dMax) else this
    }

    companion object {
        private fun formatV(value: BigInteger): String =
            (value / TWO).toString() +
                    if (value % TWO == ZERO) "" else ".5"

        fun noiseTerm(doubledRange: BigInteger): AffineNoiseTerm =
            AffineNoiseTerm(doubledRange, -doubledRange, doubledRange)

        fun fromConstant(constant: Long): IntegerAffine =
            IntegerAffine(valueOf(constant.toLong()) * TWO, emptyMap())

        fun fromRangeNoKey(start: Long, end: Long): IntegerAffine {
            val twiceCenter = valueOf(start) + valueOf(end)
            val twiceRadius = valueOf(end) - valueOf(start)
            return IntegerAffine(
                twiceCenter,
                mapOf(Context.Key.EphemeralKey.new() to noiseTerm(twiceRadius))
            )
        }

        fun fromRange(start: Long, end: Long, key: Context.Key): IntegerAffine {
            val twiceCenter = valueOf(start) + valueOf(end)
            val twiceRadius = valueOf(end) - valueOf(start)
            return IntegerAffine(twiceCenter, mapOf(key to noiseTerm(twiceRadius)))
        }
    }

    fun canRangeConstrain(): Boolean = noiseTerms.size == 1
    fun rangeConstrain(start: BigInteger, end: BigInteger): IntegerAffine {
        assert(canRangeConstrain())
        val key = noiseTerms.keys.first()
        val noiseTerm = noiseTerms.values.first()
        val newNoiseTerm = noiseTerm
            .constrainDMin(start * TWO - dCenter)
            .constrainDMax(end * TWO - dCenter)
        return IntegerAffine(dCenter, mapOf(key to newNoiseTerm))
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
        return "$min..$max (${formatV(dCenter)} + ${
            noiseTerms.entries.joinToString(" + ") { "${formatV(it.value.dRange)} * ${it.key}" }
        })"
    }

    fun toRange(): Pair<BigInteger, BigInteger> {
        val negDRadius = AffineNoiseTerm.findDMin(noiseTerms.values)
        val posDRadius = AffineNoiseTerm.findDMax(noiseTerms.values)
        val lower = (dCenter + negDRadius) / TWO
        val upper = (dCenter + posDRadius) / TWO
        return Pair(lower, upper)
    }

    fun isExactly(i: Int): Boolean {
        val (lower, upper) = toRange()
        return lower == valueOf(i.toLong()) && upper == valueOf(i.toLong())
    }

    fun subtract(other: IntegerAffine): IntegerAffine {
        return addOrSubtract(other, shouldAdd = false)
    }

    fun add(other: IntegerAffine): IntegerAffine {
        return addOrSubtract(other, shouldAdd = true)
    }

    private fun addOrSubtract(other: IntegerAffine, shouldAdd: Boolean): IntegerAffine {
        val newCenter = if (shouldAdd) dCenter + other.dCenter else dCenter - other.dCenter
        val newNoiseTerms = mutableMapOf<Context.Key, AffineNoiseTerm>()

        for (key in noiseTerms.keys.union(other.noiseTerms.keys)) {
            val lhs = noiseTerms[key] ?: AffineNoiseTerm.zero
            val rhs = other.noiseTerms[key] ?: AffineNoiseTerm.zero
            newNoiseTerms[key] = lhs.addOrSubtract(rhs, shouldAdd)
        }

        return IntegerAffine(newCenter, newNoiseTerms)
    }

    fun multiply(other: IntegerAffine): IntegerAffine {
        // Multiply centers (remember they represent twice the value)
        val newCenter = (dCenter * other.dCenter) / TWO

        val newNoiseTerms = mutableMapOf<Context.Key, AffineNoiseTerm>()

        // Handle center * noise terms
        for ((key, value) in other.noiseTerms) {
            newNoiseTerms[key] = value.multByDCenter(dCenter)
        }
        for ((key, value) in noiseTerms) {
            val newTerm = value.multByDCenter(other.dCenter)
            newNoiseTerms[key] = newNoiseTerms[key]?.addOrSubtract(newTerm, true) ?: newTerm
        }

        // Don't need to worry about noise * noise if one of them is empty.
        if (!noiseTerms.isEmpty() && !other.noiseTerms.isEmpty()) {
            // Solving even simple affines isn't possible to do easily generally, as seen by
            // `min (x + 5y) * (3x + 6y) subject to -10 <= x <= 10, -10 <= y <= 10` which has two
            // troughs at y=-7/2 and y=7/2.

            // However, what you can do, if you've both got independent noise terms, is to figure out what would need
            // to be what for min and max and then calculate those ahead-of-time for multiplication.
            val hasIndependentKeys = noiseTerms.keys.intersect(other.noiseTerms.keys).isEmpty()
            val quadraticTerm = let {
                val myDMin = AffineNoiseTerm.findDMin(noiseTerms.values)
                val myDMax = AffineNoiseTerm.findDMax(noiseTerms.values)
                val myDRange = AffineNoiseTerm.findDRange(noiseTerms.values)

                val otherDMin = AffineNoiseTerm.findDMin(other.noiseTerms.values)
                val otherDMax = AffineNoiseTerm.findDMax(other.noiseTerms.values)
                val otherDRange = AffineNoiseTerm.findDRange(other.noiseTerms.values)

                val myDMinOtherDMin = myDMin * otherDMin / TWO
                val myDMinOtherDMax = myDMin * otherDMax / TWO
                val myDMaxOtherDMin = myDMax * otherDMin / TWO
                val myDMaxOtherDMax = myDMax * otherDMax / TWO

                val newDMin = myDMinOtherDMin.min(myDMinOtherDMax).min(myDMaxOtherDMin).min(myDMaxOtherDMax)
                val newDMax = myDMinOtherDMin.max(myDMinOtherDMax).max(myDMaxOtherDMin).max(myDMaxOtherDMax)
                val newDRange = myDRange * otherDRange / TWO
                if (!hasIndependentKeys) {
                    AffineNoiseTerm(newDRange, newDMin, newDMax)
                } else {
                    // Step 1: For min and max, figure out which needs to be negative and which needs to be positive.
                    val myMinSign = if (newDMin == myDMinOtherDMin || newDMin == myDMinOtherDMax) -1 else 1
                    val otherMinSign = if (newDMin == myDMinOtherDMin || newDMin == myDMaxOtherDMin) -1 else 1
                    val myMaxSign = if (newDMax == myDMaxOtherDMin || newDMax == myDMaxOtherDMax) 1 else -1
                    val otherMaxSign = if (newDMax == myDMinOtherDMax || newDMax == myDMaxOtherDMax) 1 else -1

                    // Step 2: Now we know what the signs of our terms are, we can multiply them.
                    var newQuadraticDMin = ZERO
                    var newQuadraticDMax = ZERO
                    for (myNoise in noiseTerms.values) {
                        for (otherNoise in other.noiseTerms.values) {
                            newQuadraticDMin += myNoise.grab(myMinSign) * otherNoise.grab(otherMinSign) / TWO
                            newQuadraticDMax += myNoise.grab(myMaxSign) * otherNoise.grab(otherMaxSign) / TWO
                        }
                    }

                    AffineNoiseTerm(newDRange, newQuadraticDMin, newQuadraticDMax)
                }
            }

            // todo: We could re-center the quadratic based on its ranges.

            val key = Context.Key.EphemeralKey("Quadratic")
            newNoiseTerms[key] = newNoiseTerms[key]?.addOrSubtract(quadraticTerm, true) ?: quadraticTerm
        }

        val finalAffine = IntegerAffine(newCenter, newNoiseTerms)

        return finalAffine
    }
}
