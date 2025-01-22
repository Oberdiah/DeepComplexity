package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.settings.Settings
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.ALLOW
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.CLAMP
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getSetSize
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.isFloatingPoint
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import java.math.BigInteger
import kotlin.reflect.KClass

class NumberRange<T : Number, Self : NumberSetRangeImpl<T, Self>>(
    val start: T,
    val end: T,
    private val setIndicator: NumberSetIndicator<T, Self>
) {
    private val clazz: KClass<*> = setIndicator.clazz

    private fun newRange(start: T, end: T): NumberRange<T, Self> {
        return NumberRange(start, end, setIndicator)
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

    fun addition(other: NumberRange<T, Self>): Iterable<NumberRange<T, Self>> {
        return if (clazz.isFloatingPoint()) {
            listOf(newRange(start + other.start, end + other.end))
        } else {
            resolvePotentialOverflow(
                BigInteger.valueOf(start.toLong()).add(BigInteger.valueOf(other.start.toLong())),
                BigInteger.valueOf(end.toLong()).add(BigInteger.valueOf(other.end.toLong())),
            )
        }
    }

    fun subtraction(other: NumberRange<T, Self>): Iterable<NumberRange<T, Self>> {
        return if (clazz.isFloatingPoint()) {
            listOf(newRange(start - other.end, end - other.start))
        } else {
            resolvePotentialOverflow(
                BigInteger.valueOf(start.toLong()).subtract(BigInteger.valueOf(other.end.toLong())),
                BigInteger.valueOf(end.toLong()).subtract(BigInteger.valueOf(other.start.toLong())),
            )
        }
    }

    fun multiplication(other: NumberRange<T, Self>): Iterable<NumberRange<T, Self>> {
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

    fun division(other: NumberRange<T, Self>): Iterable<NumberRange<T, Self>> {
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
                BigInteger.valueOf(setIndicator.getMaxValue().toLong())
            } else {
                BigInteger.valueOf(setIndicator.getMinValue().toLong())
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
    ): Iterable<NumberRange<T, Self>> {
        val minValue = BigInteger.valueOf(setIndicator.getMinValue().toLong())
        val maxValue = BigInteger.valueOf(setIndicator.getMaxValue().toLong())

        when (Settings.overflowBehaviour) {
            CLAMP -> {
                return listOf(
                    newRange(
                        bigIntToT(min.max(minValue)),
                        bigIntToT(max.min(maxValue))
                    )
                )
            }

            ALLOW -> {
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
    }
}
