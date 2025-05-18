package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.github.oberdiah.deepcomplexity.utilities.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.utilities.Utilities.div
import com.github.oberdiah.deepcomplexity.utilities.Utilities.getSetSize
import com.github.oberdiah.deepcomplexity.utilities.Utilities.isFloatingPoint
import com.github.oberdiah.deepcomplexity.utilities.Utilities.max
import com.github.oberdiah.deepcomplexity.utilities.Utilities.min
import com.github.oberdiah.deepcomplexity.utilities.Utilities.minus
import com.github.oberdiah.deepcomplexity.utilities.Utilities.plus
import com.github.oberdiah.deepcomplexity.utilities.Utilities.times
import java.math.BigInteger
import kotlin.reflect.KClass

class NumberRange<T : Number> private constructor(
    val ind: NumberSetIndicator<T>,
    val start: T,
    val end: T
) {
    private val clazz: KClass<*> = ind.clazz

    init {
        assert(start <= end) {
            "Start ($start) must be less than or equal to end ($end)"
        }
    }

    companion object {
        fun <T : Number> fromConstant(constant: T): NumberRange<T> {
            return NumberRange(SetIndicator.fromValue(constant), constant, constant)
        }

        fun <T : Number> new(lower: T, upper: T): NumberRange<T> {
            return NumberRange(SetIndicator.fromValue(lower), lower, upper)
        }
    }

    private fun newRange(start: T, end: T): NumberRange<T> {
        return NumberRange(ind, start, end)
    }

    fun <Q : Number> castTo(newInd: NumberSetIndicator<Q>): Iterable<NumberRange<Q>> {
        if (newInd.clazz.isFloatingPoint() || ind.clazz.isFloatingPoint()) {
            TODO("Not implemented FP casting yet.")
        }

        val baseline = fromConstant(newInd.getZero())
        return baseline.resolvePotentialOverflow(
            BigInteger.valueOf(start.toLong()),
            BigInteger.valueOf(end.toLong())
        )
    }

    override fun toString(): String {
        if (start == end) {
            return "$start"
        }

        return "${ind.stringify(start)}..${ind.stringify(end)}"
    }

    fun add(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return if (clazz.isFloatingPoint()) {
            listOf(newRange(start + other.start, end + other.end))
        } else {
            resolvePotentialOverflow(
                BigInteger.valueOf(start.toLong()).add(BigInteger.valueOf(other.start.toLong())),
                BigInteger.valueOf(end.toLong()).add(BigInteger.valueOf(other.end.toLong())),
            )
        }
    }

    fun subtract(other: NumberRange<T>): Iterable<NumberRange<T>> {
        return if (clazz.isFloatingPoint()) {
            listOf(newRange(start - other.end, end - other.start))
        } else {
            resolvePotentialOverflow(
                BigInteger.valueOf(start.toLong()).subtract(BigInteger.valueOf(other.end.toLong())),
                BigInteger.valueOf(end.toLong()).subtract(BigInteger.valueOf(other.start.toLong())),
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

    private fun multiply(a: Number, b: Number): BigInteger {
        return BigInteger.valueOf(a.toLong()).multiply(BigInteger.valueOf(b.toLong()))
    }

    private fun divide(a: Number, b: Number): BigInteger? {
        if (b.toLong() == 0L) {
            return null
        }

        return BigInteger.valueOf(a.toLong()).divide(BigInteger.valueOf(b.toLong()))
    }

    private fun bigIntToT(v: BigInteger): T {
        assert(v >= BigInteger.valueOf(ind.getMinValue().toLong())) {
            "Value $v is below minimum value ${ind.getMinValue()}"
        }
        assert(v <= BigInteger.valueOf(ind.getMaxValue().toLong())) {
            "Value $v is above maximum value ${ind.getMaxValue()}"
        }
        return v.longValueExact().castInto(clazz)
    }

    private fun resolvePotentialOverflow(
        initialLower: BigInteger,
        initialUpper: BigInteger
    ): Iterable<NumberRange<T>> {
        assert(initialLower <= initialUpper) {
            "Lower bound $initialLower is greater than upper bound $initialUpper"
        }

        val indMin = BigInteger.valueOf(ind.getMinValue().toLong())
        val indMax = BigInteger.valueOf(ind.getMaxValue().toLong())
        val setSize = BigInteger.valueOf(ind.clazz.getSetSize().toLong())

        val distanceToShunt = setSize * if (initialLower < indMin) {
            (indMin - initialLower - BigInteger.ONE) / setSize + BigInteger.ONE
        } else {
            (indMin - initialLower) / setSize
        }

        // The first step is to shift the range so that the lower value is definitely between min and max.
        // Makes things a lot easier to reason about.
        val (lower, upper) = initialLower + distanceToShunt to initialUpper + distanceToShunt

        assert(lower >= indMin) {
            "Lower bound $lower is below minimum value $indMin"
        }
        assert(lower <= indMax) {
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