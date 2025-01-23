package com.github.oberdiah.deepcomplexity.staticAnalysis

import java.math.BigInteger

/**
 * A neat little class that represents an integer affine.
 */
class IntegerAffine(
    // This represents twice the constant term. This allows us to represent ranges like [1, 2]
    // where the center is 1.5.
    val twiceCenter: BigInteger,
    val twiceNoiseTerms: Map<Context.Key, BigInteger>
) {
    companion object {
        val ZERO = IntegerAffine(BigInteger.ZERO, emptyMap())
        val ONE = IntegerAffine(BigInteger.TWO, emptyMap())

        fun fromConstant(constant: BigInteger): IntegerAffine {
            return IntegerAffine(constant * BigInteger.TWO, emptyMap())
        }

        fun fromRange(start: Long, end: Long, key: Context.Key): IntegerAffine {
            val twiceCenter = BigInteger.valueOf(start + end)
            val twiceRadius = BigInteger.valueOf(end - start)
            return IntegerAffine(twiceCenter, mapOf(key to twiceRadius))
        }
    }

    fun toRange(): Pair<BigInteger, BigInteger> {
        val radius = twiceNoiseTerms.values.fold(BigInteger.ZERO) { acc, it -> acc + it }

        return Pair((twiceCenter - radius) / BigInteger.TWO, (twiceCenter + radius) / BigInteger.TWO)
    }

    fun add(other: IntegerAffine): IntegerAffine {
        val newTwiceCenter = twiceCenter + other.twiceCenter
        val newTwiceNoiseTerms = mutableMapOf<Context.Key, BigInteger>()
        for ((key, value) in twiceNoiseTerms) {
            newTwiceNoiseTerms[key] = value + other.twiceNoiseTerms.getOrDefault(key, BigInteger.ZERO)
        }
        for ((key, value) in other.twiceNoiseTerms) {
            if (key !in twiceNoiseTerms) {
                newTwiceNoiseTerms[key] = value
            }
        }
        return IntegerAffine(newTwiceCenter, newTwiceNoiseTerms)
    }
}