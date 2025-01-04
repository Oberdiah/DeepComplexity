package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression.BinaryNumberOperation
import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression.BinaryNumberOperation.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonExpression.ComparisonOperation
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonExpression.ComparisonOperation.*
import com.github.oberdiah.deepcomplexity.settings.Settings
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.DD_NEGATIVE_INFINITY
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.DD_POSITIVE_INFINITY
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMaxValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMinValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getSetSize
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.numberToDD
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.toStr
import org.apache.commons.numbers.core.DD
import kotlin.reflect.KClass

class NumberSet(private val clazz: KClass<*>) : MoldableSet {
    /**
     * These ranges are always sorted and never overlap.
     * They must also always be non-empty.
     */
    private val ranges = mutableListOf<NumberRange>()

    companion object {
        inline fun <reified T : Number> singleValue(v: T): NumberSet {
            return fromRange(numberToDD(v), numberToDD(v), T::class)
        }

        fun singleValue(v: DD, clazz: KClass<*>): NumberSet {
            return fromRange(v, v, clazz)
        }

        inline fun <reified T : Number> fromRange(start: T, end: T): NumberSet {
            return fromRange(numberToDD(start), numberToDD(end), T::class)
        }

        fun fromRange(start: DD, end: DD, clazz: KClass<*>): NumberSet {
            return NumberSet(clazz).apply {
                ranges.add(NumberRange(start, end))
            }
        }

        fun gaveUp(): NumberSet {
            return fromRange(DD_NEGATIVE_INFINITY, DD_POSITIVE_INFINITY, Double::class)
        }
    }

    override fun toString(): String {
        if (ranges.size == 1 && ranges[0].start == ranges[0].end) {
            return ranges[0].start.toStr()
        }

        return ranges.joinToString(", ") {
            "[${it.start.toStr()}, ${it.end.toStr()}]"
        }
    }

    fun contains(other: DD): Boolean {
        return ranges.any { it.contains(other) }
    }

    override fun getClass(): KClass<*> {
        return clazz
    }

    override fun union(other: MoldableSet): MoldableSet {
        TODO("Not yet implemented")
    }

    fun arithmeticOperation(other: NumberSet, operation: BinaryNumberOperation): NumberSet {
        val newSet = NumberSet(clazz)
        for (range in ranges) {
            for (otherRange in other.ranges) {
                val values = when (operation) {
                    ADDITION -> range.addition(otherRange, clazz)
                    SUBTRACTION -> range.subtraction(otherRange, clazz)
                    MULTIPLICATION -> range.multiplication(otherRange, clazz)
                    DIVISION -> range.division(otherRange, clazz)

                }
                newSet.ranges.addAll(values)
            }
        }
        newSet.mergeAndDeduplicate()
        return newSet
    }

    fun comparisonOperation(other: NumberSet, operation: ComparisonOperation): BooleanSet {
        val mySmallestPossibleValue = ranges[0].start
        val myLargestPossibleValue = ranges[ranges.size - 1].end
        val otherSmallestPossibleValue = other.ranges[0].start
        val otherLargestPossibleValue = other.ranges[other.ranges.size - 1].end

        when (operation) {
            LESS_THAN -> {
                if (myLargestPossibleValue < otherSmallestPossibleValue) {
                    return BooleanSet.TRUE
                } else if (mySmallestPossibleValue >= otherLargestPossibleValue) {
                    return BooleanSet.FALSE
                }
            }

            LESS_THAN_OR_EQUAL -> {
                if (myLargestPossibleValue <= otherSmallestPossibleValue) {
                    return BooleanSet.TRUE
                } else if (mySmallestPossibleValue > otherLargestPossibleValue) {
                    return BooleanSet.FALSE
                }
            }

            GREATER_THAN -> {
                if (mySmallestPossibleValue > otherLargestPossibleValue) {
                    return BooleanSet.TRUE
                } else if (myLargestPossibleValue <= otherSmallestPossibleValue) {
                    return BooleanSet.FALSE
                }
            }

            GREATER_THAN_OR_EQUAL -> {
                if (mySmallestPossibleValue >= otherLargestPossibleValue) {
                    return BooleanSet.TRUE
                } else if (myLargestPossibleValue < otherSmallestPossibleValue) {
                    return BooleanSet.FALSE
                }
            }
        }

        return BooleanSet.BOTH
    }

    private fun mergeAndDeduplicate() {
        ranges.sortWith { a, b -> a.start.compareTo(b.start) }
        val newRanges = mutableListOf<NumberRange>()
        var currentRange = ranges[0]
        for (i in 1 until ranges.size) {
            val nextRange = ranges[i]
            if (currentRange.end >= nextRange.start) {
                currentRange = NumberRange(currentRange.start, nextRange.end)
            } else {
                newRanges.add(currentRange)
                currentRange = nextRange
            }
        }
        newRanges.add(currentRange)

        assert(newRanges.size >= 1)

        ranges.clear()
        ranges.addAll(newRanges)
    }

    /**
     * The start may be equal to the end.
     */
    private inner class NumberRange(val start: DD, val end: DD) {
        fun contains(other: DD): Boolean {
            return start <= other && other <= end
        }

        fun addition(other: NumberRange, clazz: KClass<*>): Iterable<NumberRange> {
            return resolvePotentialOverflow(
                start.add(other.start),
                end.add(other.end),
                clazz
            )
        }

        fun subtraction(other: NumberRange, clazz: KClass<*>): Iterable<NumberRange> {
            return resolvePotentialOverflow(
                start.subtract(other.start),
                end.subtract(other.end),
                clazz
            )
        }

        fun multiplication(other: NumberRange, clazz: KClass<*>): Iterable<NumberRange> {
            val a = start.multiply(other.start)
            val b = start.multiply(other.end)
            val c = end.multiply(other.start)
            val d = end.multiply(other.end)
            return resolvePotentialOverflow(
                a.min(b).min(c).min(d),
                a.max(b).max(c).max(d),
                clazz
            )
        }

        fun division(other: NumberRange, clazz: KClass<*>): Iterable<NumberRange> {
            val a = start.divide(other.start)
            val b = start.divide(other.end)
            val c = end.divide(other.start)
            val d = end.divide(other.end)
            return resolvePotentialOverflow(
                a.min(b).min(c).min(d),
                a.max(b).max(c).max(d),
                clazz
            )
        }

        private fun resolvePotentialOverflow(min: DD, max: DD, clazz: KClass<*>): Iterable<NumberRange> {
            val minValue = clazz.getMinValue()
            val maxValue = clazz.getMaxValue()

            if (clazz == Double::class || clazz == Float::class) {
                val biggest = if (max > maxValue) DD_POSITIVE_INFINITY else max
                val smallest = if (min < minValue) DD_NEGATIVE_INFINITY else min
                return listOf(NumberRange(smallest, biggest))
            }

            when (Settings.overflowBehaviour) {
                CLAMP -> {
                    return listOf(NumberRange(min.max(minValue), max.min(maxValue)))
                }

                ALLOW -> {
                    // Check if we're overflowing and if we are, we must split the range.
                    // If we're overflowing in both directions we can just return the full range.

                    if (min < minValue && max > maxValue) {
                        return listOf(NumberRange(minValue, maxValue))
                    } else if (min < minValue) {
                        val wrappedMin = min.add(clazz.getSetSize())
                        if (wrappedMin < minValue) {
                            // We're overflowing so much in a single direction
                            // that the overflow will cover the full range anyway.
                            return listOf(NumberRange(minValue, maxValue))
                        }
                        return listOf(
                            NumberRange(wrappedMin, maxValue),
                            NumberRange(minValue, max)
                        )
                    } else if (max > maxValue) {
                        val wrappedMax = max.subtract(clazz.getSetSize())
                        if (wrappedMax > maxValue) {
                            // We're overflowing so much in a single direction
                            // that the overflow will cover the full range anyway.
                            return listOf(NumberRange(minValue, maxValue))
                        }
                        return listOf(
                            NumberRange(minValue, wrappedMax),
                            NumberRange(min, maxValue)
                        )
                    } else {
                        return listOf(NumberRange(min, max))
                    }
                }
            }
        }
    }
}