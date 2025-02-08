package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.settings.Settings
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.ALLOW
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.CLAMP
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.intersect
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.isFloatingPoint
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.orElse
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import java.math.BigInteger
import java.math.BigInteger.valueOf
import kotlin.reflect.KClass

typealias RangePair<T, Self> = WrappedPair<NumberRange<T, Self>>
typealias WrappedPair<T> = Pair<T, T?>

class NumberRange<T : Number, NumberSet : NumberSetImpl<T, NumberSet>> private constructor(
    val start: T,
    val end: T,
    private val setIndicator: NumberSetIndicator<T, NumberSet>,
    private val affine: IntegerAffine<T, NumberSet>
) {
    private val clazz: KClass<*> = setIndicator.clazz

    private fun newRange(start: T, end: T): NumberRange<T, NumberSet> {
        return fromRangeIndependentKey(start, end, setIndicator)
    }

    companion object {
        fun <T : Number, Self : NumberSetImpl<T, Self>> fromRange(
            start: T,
            end: T,
            setIndicator: NumberSetIndicator<T, Self>,
            key: Context.Key
        ): NumberRange<T, Self> {
            val affine = IntegerAffine.fromRange(
                start.toLong(),
                end.toLong(),
                setIndicator,
                key
            )
            return NumberRange(start, end, setIndicator, affine)
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> fromConstant(
            constant: T,
            setIndicator: NumberSetIndicator<T, Self>
        ): NumberRange<T, Self> {
            val affine = IntegerAffine.fromConstant(constant.toLong(), setIndicator)
            return NumberRange(constant, constant, setIndicator, affine)
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> fromRangeIndependentKey(
            start: T,
            end: T,
            setIndicator: NumberSetIndicator<T, Self>,
        ): NumberRange<T, Self> {
            val affine = IntegerAffine.fromRangeIndependentKey(start.toLong(), end.toLong(), setIndicator)
            return NumberRange(start, end, setIndicator, affine)
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> resolvePotentialOverflow(
            min: BigInteger,
            max: BigInteger,
            setIndicator: NumberSetIndicator<T, Self>
        ): WrappedPair<Pair<BigInteger, BigInteger>> {
            val minValue = valueOf(setIndicator.getMinValue().toLong())
            val maxValue = valueOf(setIndicator.getMaxValue().toLong())

            when (Settings.overflowBehaviour) {
                CLAMP -> {
                    return Pair(min.max(minValue) to max.min(maxValue), null)
                }

                ALLOW -> {
                    // If the range is within bounds, return it as is
                    if (min >= minValue && max <= maxValue) {
                        return Pair(min to max, null)
                    }

                    val rangeSize = maxValue - minValue + BigInteger.ONE

                    // If the range spans more than rangeSize, it covers all values
                    if (max - min >= rangeSize) {
                        return Pair(minValue to maxValue, null)
                    }

                    // Normalize the values into the valid range using modulo
                    val normalizedMin = ((min - minValue).mod(rangeSize) + minValue)
                    val normalizedMax = ((max - minValue).mod(rangeSize) + minValue)

                    // If normalized min <= normalized max, it's a simple range
                    if (normalizedMin <= normalizedMax) {
                        return Pair(normalizedMin to normalizedMax, null)
                    }

                    // Otherwise, it wraps around, so we split it into two ranges
                    return Pair(normalizedMin to maxValue, Pair(minValue, normalizedMax))
                }
            }
        }
    }

    init {
        assert(start <= end) {
            "Start ($start) must be less than or equal to end ($end)"
        }
    }

    override fun toString(): String {
        if (start == end) {
            return start.toString()
        }

        return "[$start, $end]"
    }

    private fun combineAffinesAndRanges(
        affines: WrappedPair<IntegerAffine<T, NumberSet>>,
        ranges: WrappedPair<Pair<BigInteger, BigInteger>>
    ): RangePair<T, NumberSet> {
        val (affine1, affine2) = affines
        val (range1, range2) = mapBigIntsToLong(ranges)

        val firstNumberRange = combineAffineAndRange(range1, affine1)
        val secondNumberRange = affine2?.let { range2?.let { combineAffineAndRange(range2, affine2) } }

        return Pair(firstNumberRange, secondNumberRange)
    }

    private fun combineAffineAndRange(
        range: Pair<Long, Long>,
        affineRange: IntegerAffine<T, NumberSet>
    ): NumberRange<T, NumberSet> {
        val (lower, upper) = range
        val (lowerAffine, upperAffine) = affineRange.toRange()

        // If the affine bounds are as good or better, we can continue
        // using affine.
        if (lower <= lowerAffine && upper >= upperAffine) {
            return NumberRange(
                lowerAffine.castInto(clazz),
                upperAffine.castInto(clazz),
                setIndicator,
                affineRange
            )
        }

        val (newLower, newUpper) = range.intersect(affineRange.toRange())!!

        return fromRangeIndependentKey(
            newLower.castInto(clazz),
            newUpper.castInto(clazz),
            setIndicator
        )
    }

    fun addition(other: NumberRange<T, NumberSet>): RangePair<T, NumberSet> {
        return if (clazz.isFloatingPoint()) {
            println("WARN: Floating point addition, not affine yet.")
            Pair(newRange(start + other.end, end + other.start), null)
        } else {
            val affines = this.affine.add(other.affine)
            val ranges = resolvePotentialOverflow(
                valueOf(start.toLong()) + valueOf(other.end.toLong()),
                valueOf(end.toLong()) + valueOf(other.start.toLong()),
                setIndicator
            )

            combineAffinesAndRanges(affines, ranges)
        }
    }

    fun subtraction(other: NumberRange<T, NumberSet>): RangePair<T, NumberSet> {
        return if (clazz.isFloatingPoint()) {
            println("WARN: Floating point addition, not affine yet.")
            Pair(newRange(start - other.end, end - other.start), null)
        } else {
            val affines = this.affine.add(other.affine)
            val ranges = resolvePotentialOverflow(
                valueOf(start.toLong()) - valueOf(other.end.toLong()),
                valueOf(end.toLong()) - valueOf(other.start.toLong()),
                setIndicator
            )

            combineAffinesAndRanges(affines, ranges)
        }
    }

    fun multiplication(other: NumberRange<T, NumberSet>): RangePair<T, NumberSet> {
        return if (clazz.isFloatingPoint()) {
            val a = start * other.start
            val b = start * other.end
            val c = end * other.start
            val d = end * other.end
            Pair(
                newRange(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                ),
                null
            )
        } else {
            fun multiply(a: Number, b: Number): BigInteger {
                return valueOf(a.toLong()).multiply(valueOf(b.toLong()))
            }

            val a = multiply(start, other.start)
            val b = multiply(end, other.start)
            val c = multiply(start, other.end)
            val d = multiply(end, other.end)

            val ranges = resolvePotentialOverflow(
                a.min(b).min(c).min(d),
                a.max(b).max(c).max(d),
                setIndicator
            )

            combineAffinesAndRanges(this.affine.multiply(other.affine), ranges)
        }
    }

    fun division(other: NumberRange<T, NumberSet>): RangePair<T, NumberSet> {
        return if (clazz.isFloatingPoint()) {
            val a = start / other.start
            val b = start / other.end
            val c = end / other.start
            val d = end / other.end
            Pair(
                newRange(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                ),
                null
            )
        } else {
            fun divide(a: Number, b: Number): BigInteger {
                if (b.toLong() == 0L) {
                    // Could potentially warn of overflow here one day?
                    return if (a > 0) {
                        valueOf(setIndicator.getMaxValue().toLong())
                    } else {
                        valueOf(setIndicator.getMinValue().toLong())
                    }
                }

                return valueOf(a.toLong()).divide(valueOf(b.toLong()))
            }

            val a = divide(start, other.start)
            val b = divide(end, other.start)
            val c = divide(start, other.end)
            val d = divide(end, other.end)

            mapBigIntsToT(
                resolvePotentialOverflow(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                    setIndicator
                )
            )
        }
    }

    private fun bigIntToT(v: BigInteger): T {
        return v.longValueExact().castInto(clazz)
    }

    private fun mapBigIntsToLong(pairs: WrappedPair<Pair<BigInteger, BigInteger>>): WrappedPair<Pair<Long, Long>> {
        val (pair1, pair2) = pairs
        return Pair(
            Pair(pair1.first.longValueExact(), pair1.second.longValueExact()),
            pair2?.let { Pair(it.first.longValueExact(), it.second.longValueExact()) }
        )
    }

    private fun mapBigIntsToT(pairs: WrappedPair<Pair<BigInteger, BigInteger>>): RangePair<T, NumberSet> {
        val (pair1, pair2) = pairs
        return Pair(
            newRange(bigIntToT(pair1.first), bigIntToT(pair1.second)),
            pair2?.let { newRange(bigIntToT(it.first), bigIntToT(it.second)) })

    }
}
