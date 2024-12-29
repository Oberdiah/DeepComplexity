package com.github.oberdiah.deepcomplexity.staticAnalysis

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
import org.apache.commons.numbers.core.DD
import kotlin.reflect.KClass

class NumberSet(private val clazz: KClass<Any>) : MoldableSet<DD> {
    private val ranges = mutableListOf<MoldableRange>()

    override fun contains(other: DD): Boolean {
        return ranges.any { it.contains(other) }
    }

    override fun getClass(): KClass<Any> {
        return clazz
    }

    fun multiplication(other: NumberSet): NumberSet {
        val newSet = NumberSet(clazz)
        for (range in ranges) {
            for (otherRange in other.ranges) {
                newSet.ranges.addAll(range.multiplication(otherRange, clazz))
            }
        }
        newSet.mergeAndDeduplicate()
        return newSet
    }

    fun addition(other: NumberSet): NumberSet {
        val newSet = NumberSet(clazz)
        for (range in ranges) {
            for (otherRange in other.ranges) {
                newSet.ranges.addAll(range.addition(otherRange, clazz))
            }
        }
        newSet.mergeAndDeduplicate()
        return newSet
    }

    private fun mergeAndDeduplicate() {
        ranges.sortWith { a, b -> a.start.compareTo(b.start) }
        val newRanges = mutableListOf<MoldableRange>()
        var currentRange = ranges[0]
        for (i in 1 until ranges.size) {
            val nextRange = ranges[i]
            if (currentRange.end >= nextRange.start) {
                currentRange = MoldableRange(currentRange.start, nextRange.end)
            } else {
                newRanges.add(currentRange)
                currentRange = nextRange
            }
        }
        newRanges.add(currentRange)
        ranges.clear()
        ranges.addAll(newRanges)
    }

    /**
     * The start may be equal to the end.
     */
    private inner class MoldableRange(val start: DD, val end: DD) {
        fun contains(other: DD): Boolean {
            return start <= other && other <= end
        }

        /**
         * Returns the possible range of values that could be obtained when adding this range to the other.
         */
        fun addition(other: MoldableRange, clazz: KClass<Any>): Iterable<MoldableRange> {
            return resolvePotentialOverflow(
                start.min(other.start),
                end.max(other.end),
                clazz
            )
        }

        fun multiplication(other: MoldableRange, clazz: KClass<Any>): Iterable<MoldableRange> {
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

        private fun resolvePotentialOverflow(min: DD, max: DD, clazz: KClass<Any>): Iterable<MoldableRange> {
            val minValue = clazz.getMinValue()
            val maxValue = clazz.getMaxValue()

            if (clazz == Double::class || clazz == Float::class) {
                val biggest = if (max > maxValue) DD_POSITIVE_INFINITY else max
                val smallest = if (min < minValue) DD_NEGATIVE_INFINITY else min
                return listOf(MoldableRange(smallest, biggest))
            }

            when (Settings.overflowBehaviour) {
                CLAMP -> {
                    return listOf(MoldableRange(min.max(minValue), max.min(maxValue)))
                }

                ALLOW -> {
                    // Check if we're overflowing and if we are, we must split the range.
                    // If we're overflowing in both directions we don't need to split again.
                    
                    if (min < minValue && max > maxValue) {
                        return listOf(MoldableRange(minValue, maxValue))
                    } else if (min < minValue) {
                        val wrappedMin = min.add(clazz.getSetSize())
                        return listOf(
                            MoldableRange(wrappedMin, maxValue),
                            MoldableRange(minValue, max)
                        )
                    } else if (max > maxValue) {
                        val wrappedMax = max.subtract(clazz.getSetSize())
                        return listOf(
                            MoldableRange(minValue, wrappedMax),
                            MoldableRange(min, maxValue)
                        )
                    } else {
                        return listOf(MoldableRange(min, max))
                    }
                }
            }
        }
    }
}

/**
 * This is the set of possible values an expression can take.
 */
interface MoldableSet<T> {
    /**
     * The class of the elements in the set.
     *
     * T may not be equal to the class e.g. in the case of numbers,
     * T is a DD but the class is Int, Double, etc.
     */
    fun getClass(): KClass<Any>

    fun contains(other: T): Boolean
}