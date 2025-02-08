package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import java.math.BigInteger
import java.math.BigInteger.valueOf

/**
 * A neat little class that represents an integer affine.
 */
class IntegerAffine<N : Number, NumberSet : FullyTypedNumberSet<N, NumberSet>>(
    // This represents twice the constant term. This allows us to represent ranges like [1, 2]
    // where the center is 1.5.
    private val center: BigInteger,
    // This represents twice the noise terms. Again, this allows us to represent ranges like [1, 2]
    private val noiseTerms: Map<Context.Key, BigInteger>,
    /**
     * This is the negative side of the limit (inclusive), the positive side is one lower.
     * e.g. if the limit is 128, the range is [-128, 127].
     */
    private val setIndicator: NumberSetIndicator<N, NumberSet>,
) {
    override fun toString(): String {
        val (lower, upper) = toRange()
        return "[$lower, $upper] @ ${noiseTerms.keys}"
    }

    companion object {
        fun <N : Number, NumberSet : FullyTypedNumberSet<N, NumberSet>> fromConstant(
            constant: Long,
            setIndicator: NumberSetIndicator<N, NumberSet>
        ): IntegerAffine<N, NumberSet> {
            return IntegerAffine(
                valueOf(constant) * BigInteger.TWO,
                emptyMap(),
                setIndicator
            )
        }

        fun <N : Number, Self : FullyTypedNumberSet<N, Self>> fromRangeIndependentKey(
            start: Long,
            end: Long,
            setIndicator: NumberSetIndicator<N, Self>
        ): IntegerAffine<N, Self> {
            val twiceCenter = valueOf(start + end)
            val twiceRadius = valueOf(end - start)
            return IntegerAffine(
                twiceCenter,
                mapOf(Context.Key.EphemeralKey.new() to twiceRadius),
                setIndicator
            )
        }

        fun <N : Number, NumberSet : FullyTypedNumberSet<N, NumberSet>> fromRange(
            start: Long,
            end: Long,
            setIndicator: NumberSetIndicator<N, NumberSet>,
            key: Context.Key
        ): IntegerAffine<N, NumberSet> {
            val twiceCenter = valueOf(start + end)
            val twiceRadius = valueOf(end - start)
            return IntegerAffine(
                twiceCenter,
                mapOf(key to twiceRadius),
                setIndicator
            )
        }

        private fun <N : Number, NumberSet : FullyTypedNumberSet<N, NumberSet>> rangeFix(affine: IntegerAffine<N, NumberSet>):
                Pair<IntegerAffine<N, NumberSet>, IntegerAffine<N, NumberSet>?> {
            val (lower, upper) = affine.toRangeInner()
            val numberLimit = affine.setIndicator.getMaxValue().toLong()
            if (lower < valueOf(-numberLimit) || upper >= valueOf(numberLimit)) {
                val (range1, range2) = NumberRange.resolvePotentialOverflow(lower, upper, affine.setIndicator)

                // This is sad news; we've had to leave affine territory and go back to ranges.
                // I don't think there's a nice way to avoid this in general (although in theory there's something
                // we could do with addition/subtraction since they're commutative over %), but if this is happening
                // a lot it might be possible that we can spot expression duplication on the tree somewhere
                // and lean on that instead of relying entirely on affine.
                return fromRangeIndependentKey(
                    range1.first.toLong(),
                    range1.second.toLong(),
                    affine.setIndicator
                ) to range2?.let {
                    fromRangeIndependentKey(
                        it.first.toLong(),
                        it.second.toLong(),
                        affine.setIndicator
                    )
                }
            }

            return affine to null
        }
    }

    fun multiply(other: IntegerAffine<N, NumberSet>): Pair<IntegerAffine<N, NumberSet>, IntegerAffine<N, NumberSet>?> {
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

        return rangeFix(IntegerAffine(newCenter, newNoiseTerms, setIndicator))
    }

    fun toRange(): Pair<Long, Long> {
        val (lower, upper) = toRangeInner()
        return Pair(lower.toLong(), upper.toLong())
    }

    fun toRangeInner(): Pair<BigInteger, BigInteger> {
        val radius = noiseTerms.values.fold(BigInteger.ZERO) { acc, it -> acc + it }
        val lower = (center - radius) / BigInteger.TWO
        val upper = (center + radius) / BigInteger.TWO
        return Pair(lower, upper)
    }

    fun subtract(other: IntegerAffine<N, NumberSet>): Pair<IntegerAffine<N, NumberSet>, IntegerAffine<N, NumberSet>?> {
        return addOrSubtract(other, shouldAdd = false)
    }

    fun add(other: IntegerAffine<N, NumberSet>): Pair<IntegerAffine<N, NumberSet>, IntegerAffine<N, NumberSet>?> {
        return addOrSubtract(other, shouldAdd = true)
    }

    private fun addOrSubtract(
        other: IntegerAffine<N, NumberSet>,
        shouldAdd: Boolean
    ): Pair<IntegerAffine<N, NumberSet>, IntegerAffine<N, NumberSet>?> {
        val newCenter = if (shouldAdd) center + other.center else center - other.center
        val newNoiseTerms = mutableMapOf<Context.Key, BigInteger>()
        for ((key, value) in noiseTerms) {
            if (shouldAdd) {
                newNoiseTerms[key] = value + other.noiseTerms.getOrDefault(key, BigInteger.ZERO)
            } else {
                newNoiseTerms[key] = value - other.noiseTerms.getOrDefault(key, BigInteger.ZERO)
            }
        }
        for ((key, value) in other.noiseTerms) {
            if (key !in noiseTerms) {
                newNoiseTerms[key] = value
            }
        }

        return rangeFix(IntegerAffine(newCenter, newNoiseTerms, setIndicator))
    }
}
