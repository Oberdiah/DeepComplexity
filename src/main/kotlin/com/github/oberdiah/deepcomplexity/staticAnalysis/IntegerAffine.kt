package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getSetSize
import com.jetbrains.rd.util.threading.coroutines.RdCoroutineScope.Companion.override
import java.math.BigInteger
import java.math.BigInteger.valueOf

/**
 * A neat little class that represents an integer affine.
 */
@ConsistentCopyVisibility
data class IntegerAffine private constructor(
    // This represents twice the constant term. This allows us to represent ranges like [1, 2]
    // where the center is 1.5.
    private val center: BigInteger,
    // This represents twice the noise terms. Again, this allows us to represent ranges like [1, 2]
    private val noiseTerms: Map<Context.Key, BigInteger>,
    private val setIndicator: NumberSetIndicator<*, *>
) {
    companion object {
        fun fromConstant(constant: Long, ind: NumberSetIndicator<*, *>): IntegerAffine =
            IntegerAffine(valueOf(constant.toLong()) * BigInteger.TWO, emptyMap(), ind)

        fun fromRangeNoKey(start: Long, end: Long, ind: NumberSetIndicator<*, *>): IntegerAffine {
            val twiceCenter = valueOf(start) + valueOf(end)
            val twiceRadius = valueOf(end) - valueOf(start)
            return IntegerAffine(twiceCenter, mapOf(Context.Key.EphemeralKey.new() to twiceRadius), ind)
        }

        fun fromRange(start: Long, end: Long, key: Context.Key, ind: NumberSetIndicator<*, *>): IntegerAffine {
            val twiceCenter = valueOf(start) + valueOf(end)
            val twiceRadius = valueOf(end) - valueOf(start)
            return IntegerAffine(twiceCenter, mapOf(key to twiceRadius), ind)
        }
    }

    fun getKeys(): List<Context.Key> = noiseTerms.keys.toList()

    // Still to do — Need to store the idealized range alongside the affine one and
    // calculate with that as well.

    private fun formatV(value: BigInteger): String =
        (value / BigInteger.TWO).toString() +
                if (value % BigInteger.TWO == BigInteger.ZERO) "" else ".5"

    fun stringOverview(): String {
        val (min, max) = toRange()

        if (min == max) {
            return "$min"
        }

        return "$min..$max (${noiseTerms.keys})"
    }

    override fun toString(): String {
        val (min, max) = toRange()
        return "$min..$max (${formatV(center)} + ${
            noiseTerms.entries.joinToString(" + ") { "${formatV(it.value)} * ${it.key}" }
        })"
    }

    fun toRange(): Pair<BigInteger, BigInteger> {
        val radius = noiseTerms.values.fold(BigInteger.ZERO) { acc, it -> acc + it }
        val initialStart = (center - radius) / BigInteger.TWO

        val min = valueOf(setIndicator.getMinValue().toLong())
        val setSize = valueOf(setIndicator.clazz.getSetSize().toLong())

        val incrementsToShunt = if (initialStart < min) {
            (min - initialStart) / setSize + BigInteger.ONE
        } else {
            (min - initialStart) / setSize
        }
        val newCenter = center + incrementsToShunt * setSize * BigInteger.TWO

        val lower = (newCenter - radius) / BigInteger.TWO
        val upper = (newCenter + radius) / BigInteger.TWO
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

        return IntegerAffine(newCenter, newNoiseTerms, setIndicator)
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

        return IntegerAffine(newCenter, newNoiseTerms, setIndicator)
    }
}
