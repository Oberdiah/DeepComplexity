package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet.NumberData
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Affine
import java.util.stream.Stream
import kotlin.collections.sortedWith

class Ranges<T : Number> private constructor(
    // These affines can overlap.
    // They are not sorted (Primarily because it's a little hard to do when each affine can represent two ranges)
    private val ranges: List<Affine<T>>,
    private val setIndicator: NumberSetIndicator<T, *>
) : NumberData<T> {
    override fun toString(): String = "SortedRanges($ranges)"

    fun toRangePairs(): List<Pair<T, T>> = NumberUtilities.mergeAndDeduplicate(pairsStream().toList())
    private fun pairsStream(): Stream<Pair<T, T>> = ranges.stream().flatMap { it.toRanges().stream() }
    private fun makeNew(ranges: List<Affine<T>>): Ranges<T> = Ranges(ranges, setIndicator)

    override fun isConfirmedToBe(i: Int): Boolean = ranges.all { it.isExactly(i) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ranges<*>) return false
        if (ranges != other.ranges) return false

        return true
    }

    override fun hashCode(): Int = ranges.hashCode()

    fun add(other: Ranges<T>): Ranges<T> = doOperation(other, Affine<T>::add)
    fun subtract(other: Ranges<T>): Ranges<T> = doOperation(other, Affine<T>::subtract)
    fun multiply(other: Ranges<T>): Ranges<T> = doOperation(other, Affine<T>::multiply)
    fun divide(other: Ranges<T>): Ranges<T> = doOperation(other, Affine<T>::divide)

    fun doOperation(other: Ranges<T>, operation: (Affine<T>, Affine<T>) -> Affine<T>): Ranges<T> {
        val newList: MutableList<Affine<T>> = mutableListOf()

        for (range in ranges) {
            for (otherRange in other.ranges) {
                newList.add(operation(range, otherRange))
            }
        }

        return makeNew(newList)
    }

    fun union(other: Ranges<T>): Ranges<T> {
        // No affine killing here :)
        return makeNew(ranges + other.ranges)
    }

    fun intersection(other: Ranges<T>): Ranges<T> {
        val newList: MutableList<Affine<T>> = mutableListOf()

        // For each range in this set, find overlapping ranges in other set
        // This could be made much more efficient.
        for (range in pairsStream()) {
            for (otherRange in other.pairsStream()) {
                // Find overlap
                val start = range.first.max(otherRange.first)
                val end = range.second.min(otherRange.second)

                // This could be much smarter to prevent some affine death.
                // If one range is completely contained within another, we could add the contained range
                // whole.

                // If there is an overlap, add it
                if (start <= end) {
                    newList.add(Affine.fromRangeNoKey(start, end, setIndicator))
                }
            }
        }

        return makeNew(newList)
    }

    fun invert(): Ranges<T> {
        val newList: MutableList<Affine<T>> = mutableListOf()
        // Affine death is inevitable here.

        val minValue = setIndicator.getMinValue()
        val maxValue = setIndicator.getMaxValue()

        if (ranges.isEmpty()) {
            newList.add(Affine.fromRangeNoKey(minValue, maxValue, setIndicator))
            return makeNew(newList)
        }

        var currentMin = minValue

        val orderedPairs = toRangePairs().sortedWith { a, b -> a.first.compareTo(b.first) }
        for (range in orderedPairs) {
            if (currentMin < range.first) {
                newList.add(Affine.fromRangeNoKey(currentMin, range.first.downOneEpsilon(), setIndicator))
            }
            currentMin = range.second.upOneEpsilon()
        }

        // Add final range if necessary
        if (currentMin < maxValue) {
            newList.add(Affine.fromRangeNoKey(currentMin, maxValue, setIndicator))
        }

        return makeNew(newList)
    }

    companion object {
        fun <T : Number> fromConstant(constant: T, ind: NumberSetIndicator<T, *>): Ranges<T> =
            Ranges(listOf(Affine.fromConstant(constant, ind)), ind)

        fun <T : Number> fromRange(start: T, end: T, key: Context.Key, ind: NumberSetIndicator<T, *>): Ranges<T> =
            Ranges(listOf(Affine.fromRange(start, end, key, ind)), ind)
    }
}