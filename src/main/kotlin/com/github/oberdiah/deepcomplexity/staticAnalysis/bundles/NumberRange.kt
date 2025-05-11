package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
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

    fun divide(other: NumberRange<T>): Iterable<NumberRange<T>> {
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
            val a = divide(start, other.start)
            val b = divide(end, other.start)
            val c = divide(start, other.end)
            val d = divide(end, other.end)

            resolvePotentialOverflow(
                a.min(b).min(c).min(d),
                a.max(b).max(c).max(d),
            )
        }
    }

    private fun multiply(a: Number, b: Number): BigInteger {
        return BigInteger.valueOf(a.toLong()).multiply(BigInteger.valueOf(b.toLong()))
    }

    private fun divide(a: Number, b: Number): BigInteger {
        if (b.toLong() == 0L) {
            // Could potentially warn of overflow here one day?
            return if (a > 0) {
                BigInteger.valueOf(ind.getMaxValue().toLong())
            } else {
                BigInteger.valueOf(ind.getMinValue().toLong())
            }
        }

        return BigInteger.valueOf(a.toLong()).divide(BigInteger.valueOf(b.toLong()))
    }

    private fun bigIntToT(v: BigInteger): T {
        return v.longValueExact().castInto(clazz)
    }

    private fun resolvePotentialOverflow(
        min: BigInteger,
        max: BigInteger
    ): Iterable<NumberRange<T>> {
        val minValue = BigInteger.valueOf(ind.getMinValue().toLong())
        val maxValue = BigInteger.valueOf(ind.getMaxValue().toLong())

        // Check if we're overflowing and if we are, we must split the range.
        // If we're overflowing in both directions we can just return the full range.
        if (min < minValue && max > maxValue) {
            return listOf(newRange(bigIntToT(minValue), bigIntToT(maxValue)))
        } else if (min < minValue) {
            val wrappedMin = min.add(clazz.getSetSize())
            if (wrappedMin < minValue) {
                // We're overflowing so much in a single direction
                // that the overflow will cover the full range anyway.
                return listOf(newRange(bigIntToT(minValue), bigIntToT(maxValue)))
            }
            return listOf(
                newRange(bigIntToT(wrappedMin), bigIntToT(maxValue)),
                newRange(bigIntToT(minValue), bigIntToT(max))
            )
        } else if (max > maxValue) {
            val wrappedMax = max.subtract(clazz.getSetSize())
            if (wrappedMax > maxValue) {
                // We're overflowing so much in a single direction
                // that the overflow will cover the full range anyway.
                return listOf(newRange(bigIntToT(minValue), bigIntToT(maxValue)))
            }
            return listOf(
                newRange(bigIntToT(minValue), bigIntToT(wrappedMax)),
                newRange(bigIntToT(min), bigIntToT(maxValue))
            )
        } else {
            return listOf(newRange(bigIntToT(min), bigIntToT(max)))
        }
    }
}