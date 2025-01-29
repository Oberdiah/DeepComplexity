package com.github.oberdiah.deepcomplexity.staticAnalysis

import java.math.BigInteger
import java.math.BigInteger.valueOf

/**
 * A neat little class that represents an integer affine.
 *
 * The way this is if the range represented by center and noiseTerms is outside the numberLimit
 * it's just assumed to be clipped.
 */
class IntegerAffine(
    // This represents twice the constant term. This allows us to represent ranges like [1, 2]
    // where the center is 1.5.
    private val center: BigInteger,
    // This represents twice the noise terms. Again, this allows us to represent ranges like [1, 2]
    private val noiseTerms: Map<Context.Key, BigInteger>,
    /**
     * This is the negative side of the limit (inclusive), the positive side is one lower.
     * e.g. if the limit is 128, the range is [-128, 127].
     */
    private val numberLimit: BigInteger,
) {
    companion object {
        fun fromConstant(constant: Long, numberLimit: Long): IntegerAffine {
            return IntegerAffine(
                valueOf(constant) * BigInteger.TWO,
                emptyMap(),
                valueOf(numberLimit)
            )
        }

        fun fromRange(start: Long, end: Long, numberLimit: Long, key: Context.Key): IntegerAffine {
            val twiceCenter = valueOf(start + end)
            val twiceRadius = valueOf(end - start)
            return IntegerAffine(
                twiceCenter,
                mapOf(key to twiceRadius),
                valueOf(numberLimit)
            )
        }
    }

    fun multiply(other: IntegerAffine): IntegerAffine {
        // Multiply centers (remember they represent twice the value)
        val newCenter = (center * other.center) / BigInteger.TWO

        val newNoiseTerms = mutableMapOf<Context.Key, BigInteger>()

        // Handle center * noise terms
        for ((key, value) in other.noiseTerms) {
            newNoiseTerms[key] = (center * value) / BigInteger.TWO
        }
        for ((key, value) in noiseTerms) {
            val existing = newNoiseTerms.getOrDefault(key, BigInteger.ZERO)
            newNoiseTerms[key] = existing + (other.center * value) / BigInteger.TWO
        }

        // Handle noise terms * noise terms
        var quadraticNoiseSumA = BigInteger.ZERO
        var quadraticNoiseSumB = BigInteger.ZERO

        for (value1 in noiseTerms.values) {
            quadraticNoiseSumA += value1.abs()
        }
        for (value2 in other.noiseTerms.values) {
            quadraticNoiseSumB += value2.abs()
        }

        val quadraticNoiseSum = quadraticNoiseSumA * quadraticNoiseSumB / BigInteger.TWO

        // Add the combined quadratic noise term if non-zero
        if (quadraticNoiseSum != BigInteger.ZERO) {
            val quadraticKey = Context.Key.EphemeralKey("Quadratic")
            val existing = newNoiseTerms.getOrDefault(quadraticKey, BigInteger.ZERO)
            newNoiseTerms[quadraticKey] = existing + quadraticNoiseSum
        }

        return IntegerAffine(newCenter, newNoiseTerms, numberLimit)
    }

    fun toRange(): Pair<Pair<Long, Long>, Pair<Long, Long>?> {
        val (inner, outer) = toRangeInner()
        return Pair(inner.first.toLong(), inner.second.toLong()) to
                outer?.let { Pair(it.first.toLong(), it.second.toLong()) }
    }

    fun negate(): IntegerAffine {
        return IntegerAffine(-center, noiseTerms.mapValues { (_, value) -> -value }, numberLimit)
    }

    fun subtract(other: IntegerAffine): IntegerAffine {
        return add(other.negate())
    }

    fun add(other: IntegerAffine): IntegerAffine {
        val newCenter = center + other.center
        val newNoiseTerms = mutableMapOf<Context.Key, BigInteger>()
        for ((key, value) in noiseTerms) {
            newNoiseTerms[key] = value + other.noiseTerms.getOrDefault(key, BigInteger.ZERO)
        }
        for ((key, value) in other.noiseTerms) {
            if (key !in noiseTerms) {
                newNoiseTerms[key] = value
            }
        }
        return IntegerAffine(newCenter, newNoiseTerms, numberLimit)
    }

    /**
     * Returns at least one pair, possibly two if the range has hit the limit and
     * had to wrap around.
     */
    private fun toRangeInner(): Pair<Pair<BigInteger, BigInteger>, Pair<BigInteger, BigInteger>?> {
        val radius = noiseTerms.values.fold(BigInteger.ZERO) { acc, it -> acc + it }

        val twiceLimit = numberLimit * BigInteger.TWO

        val lower = (center - radius) / BigInteger.TWO
        val upper = (center + radius) / BigInteger.TWO

        // Get lower into the range [-numberLimit, numberLimit)
        val normalizedLower = (lower + numberLimit).mod(twiceLimit) - numberLimit
        val distMoved = lower - normalizedLower
        val shiftedUpper = upper - distMoved

        // Now we have two cases — normalizedUpper is greater than or equal to numberLimit or it's not.
        // If it's not, we're done.
        if (shiftedUpper < numberLimit) {
            return Pair(normalizedLower, shiftedUpper) to null
        } else if (shiftedUpper > twiceLimit) {
            // If it's greater than 2 * numberLimit, we can just return the whole range.
            return Pair(-numberLimit, numberLimit - BigInteger.ONE) to null
        }
        // otherwise, we need to wrap around.
        val pair1 = Pair(normalizedLower, numberLimit - BigInteger.ONE)
        val normalizedUpper = shiftedUpper - twiceLimit

        return pair1 to Pair(-numberLimit, normalizedUpper)
    }
}
