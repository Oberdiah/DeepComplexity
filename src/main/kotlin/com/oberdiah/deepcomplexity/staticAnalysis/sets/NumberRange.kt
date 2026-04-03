package com.oberdiah.deepcomplexity.staticAnalysis.sets

import com.oberdiah.deepcomplexity.staticAnalysis.BigIntegerIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.HasIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import com.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.oberdiah.deepcomplexity.utilities.Utilities.compareTo
import com.oberdiah.deepcomplexity.utilities.Utilities.div
import com.oberdiah.deepcomplexity.utilities.Utilities.downOneEpsilon
import com.oberdiah.deepcomplexity.utilities.Utilities.getSetSize
import com.oberdiah.deepcomplexity.utilities.Utilities.isFloatingPoint
import com.oberdiah.deepcomplexity.utilities.Utilities.max
import com.oberdiah.deepcomplexity.utilities.Utilities.min
import com.oberdiah.deepcomplexity.utilities.Utilities.minus
import com.oberdiah.deepcomplexity.utilities.Utilities.plus
import com.oberdiah.deepcomplexity.utilities.Utilities.times
import com.oberdiah.deepcomplexity.utilities.Utilities.toBigInteger
import com.oberdiah.deepcomplexity.utilities.Utilities.upOneEpsilon
import java.math.BigInteger
import kotlin.reflect.KClass

@ConsistentCopyVisibility
data class NumberRange<T : Number> private constructor(
    override val ind: NumberIndicator<T>,
    val start: T,
    val end: T
) : HasIndicator<T> {
    private val clazz: KClass<*> = ind.clazz

    init {
        require(start <= end) {
            "Start ($start) must be less than or equal to end ($end)"
        }
    }

    companion object {
        fun <T : Number> fromConstant(constant: T): NumberRange<T> {
            return NumberRange(NumberIndicator.fromValue(constant), constant, constant)
        }

        fun <T : Number> new(lower: T, upper: T): NumberRange<T> {
            return NumberRange(NumberIndicator.fromValue(lower), lower, upper)
        }

        fun <T : Number> fullRange(ind: NumberIndicator<T>): NumberRange<T> {
            return NumberRange(ind, ind.getMinValue(), ind.getMaxValue())
        }
    }

    /**
     * Returns true if this range can fit entirely within [other].
     */
    fun <T : Number> canFitWithin(other: NumberRange<T>): Boolean = other.contains(start) && other.contains(end)

    fun contains(value: Number): Boolean = value >= start && value <= end

    private fun newRange(start: T, end: T): NumberRange<T> {
        return NumberRange(ind, start, end)
    }

    /**
     * Returns the size of this range, in terms of how many discrete values it contains.
     */
    fun size(): BigInteger {
        if (clazz.isFloatingPoint()) {
            if (start == end) {
                return BigInteger.ONE
            }
            TODO("Not implemented full FP size yet, not sure if we'll ever need it")
        } else {
            return end.toBigInteger() - start.toBigInteger() + BigInteger.ONE
        }
    }

    fun <Q : Number> castTo(newInd: NumberIndicator<Q>): Iterable<NumberRange<Q>> {
        if (newInd.clazz.isFloatingPoint() || ind.clazz.isFloatingPoint()) {
            TODO("Not implemented FP casting yet.")
        }

        val baseline = fromConstant(newInd.getZero())
        return baseline.resolvePotentialOverflow(start.toBigInteger(), end.toBigInteger())
    }

    override fun toString(): String {
        if (start == end) {
            return "$start"
        }

        return "${ind.rangeStringify(start)}..${ind.rangeStringify(end)}"
    }

    fun add(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return if (clazz.isFloatingPoint()) {
            listOf(newRange(start + other.start, end + other.end))
        } else {
            resolvePotentialOverflow(
                start.toBigInteger().add(other.start.toBigInteger()),
                end.toBigInteger().add(other.end.toBigInteger()),
            )
        }
    }

    fun subtract(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return if (clazz.isFloatingPoint()) {
            listOf(newRange(start - other.end, end - other.start))
        } else {
            resolvePotentialOverflow(
                start.toBigInteger().subtract(other.end.toBigInteger()),
                end.toBigInteger().subtract(other.start.toBigInteger()),
            )
        }
    }

    fun multiply(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return if (clazz.isFloatingPoint()) {
            val a = start * other.start
            val b = start * other.end
            val c = end * other.start
            val d = end * other.end
            listOf(
                newRange(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                )
            )
        } else {
            val a = multiply(start, other.start)
            val b = multiply(end, other.start)
            val c = multiply(start, other.end)
            val d = multiply(end, other.end)

            resolvePotentialOverflow(
                a.min(b).min(c).min(d),
                a.max(b).max(c).max(d),
            )
        }
    }

    fun divide(other: NumberRange<T>): Iterable<NumberRange<T>?> {
        return if (clazz.isFloatingPoint()) {
            val a = start / other.start
            val b = start / other.end
            val c = end / other.start
            val d = end / other.end
            listOf(
                newRange(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                )
            )
        } else {
            fun doDividing(other2: NumberRange<T>): Iterable<NumberRange<T>?> {
                val a = divide(start, other2.start)
                val b = divide(end, other2.start)
                val c = divide(start, other2.end)
                val d = divide(end, other2.end)

                val min = min(a, b, c, d)
                val max = max(a, b, c, d)

                if (min == null || max == null) {
                    return listOf(null)
                }

                return resolvePotentialOverflow(min, max)
            }
            if (other.start < 0 && other.end > 0) {
                val ranges = doDividing(NumberRange(ind, other.start, ind.getOne())) +
                        doDividing(NumberRange(ind, ind.getOne(), other.end))
                NumberUtilities.mergeAndDeduplicate(ranges.filterNotNull()) + null
            } else {
                doDividing(other)
            }
        }
    }

    fun min(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return listOf(newRange(start.min(other.start), end.min(other.end)))
    }

    fun max(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return listOf(newRange(start.max(other.start), end.max(other.end)))
    }

    fun overlaps(other: NumberRange<T>): Boolean {
        return start <= other.end && end >= other.start
    }

    @Suppress("unused")
    fun intersection(other: NumberRange<T>): NumberRange<T>? {
        if (!overlaps(other)) {
            return null
        }
        return newRange(start.max(other.start), end.min(other.end))
    }

    /**
     * Chops out the given range from this range. May return 0, 1, or 2 ranges.
     */
    @Suppress("unused")
    fun chopOut(other: NumberRange<T>): Iterable<NumberRange<T>> {
        if (!overlaps(other)) {
            return listOf(this)
        }

        val results = mutableListOf<NumberRange<T>>()

        // If our start is less than the other's start, there's a prefix left over.
        if (this.start < other.start) {
            results.add(newRange(this.start, other.start.downOneEpsilon()))
        }

        // If our end is greater than the other's end, there's a suffix left over.
        if (this.end > other.end) {
            results.add(newRange(other.end.upOneEpsilon(), this.end))
        }

        return results
    }

    private fun multiply(a: Number, b: Number): BigInteger {
        return a.toBigInteger().multiply(b.toBigInteger())
    }

    private fun divide(a: Number, b: Number): BigInteger? {
        if (b.toLong() == 0L) {
            return null
        }

        return a.toBigInteger().divide(b.toBigInteger())
    }

    private fun bigIntToT(v: BigInteger): T {
        require(v >= ind.getMinValue().toBigInteger()) {
            "Value $v is below minimum value ${ind.getMinValue()}"
        }
        require(v <= ind.getMaxValue().toBigInteger()) {
            "Value $v is above maximum value ${ind.getMaxValue()}"
        }
        return v.castInto(clazz)
    }

    private fun resolvePotentialOverflow(
        initialLower: BigInteger,
        initialUpper: BigInteger
    ): Iterable<NumberRange<T>> {
        if (ind == BigIntegerIndicator) {
            // Safety: We've verified that our indicator, and therefore T, is already BigInteger.
            @Suppress("UNCHECKED_CAST")
            return listOf(newRange(initialLower as T, initialUpper as T))
        }

        require(initialLower <= initialUpper) {
            "Lower bound $initialLower is greater than upper bound $initialUpper"
        }

        val indMin = ind.getMinValue().toBigInteger()
        val indMax = ind.getMaxValue().toBigInteger()
        val setSize = ind.clazz.getSetSize()

        val distanceToShunt = setSize * if (initialLower < indMin) {
            (indMin - initialLower - BigInteger.ONE) / setSize + BigInteger.ONE
        } else {
            (indMin - initialLower) / setSize
        }

        // The first step is to shift the range so that the lower value is definitely between min and max.
        // Makes things a lot easier to reason about.
        val (lower, upper) = initialLower + distanceToShunt to initialUpper + distanceToShunt

        require(lower >= indMin) {
            "Lower bound $lower is below minimum value $indMin"
        }
        require(lower <= indMax) {
            "Lower bound $lower is above maximum value $indMax"
        }

        if (upper - lower >= setSize) {
            // In this case we're covering the whole range no matter what.
            return listOf(newRange(bigIntToT(indMin), bigIntToT(indMax)))
        }

        if (upper <= indMax) {
            // Easy-peasy, we fit, all is good.
            return listOf(newRange(bigIntToT(lower), bigIntToT(upper)))
        }

        return listOf(
            newRange(bigIntToT(lower), bigIntToT(indMax)),
            newRange(bigIntToT(indMin), bigIntToT(upper - setSize))
        )
    }
}