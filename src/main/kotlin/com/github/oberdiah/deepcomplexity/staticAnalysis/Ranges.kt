package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet.NumberData
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import kotlin.collections.map
import kotlin.collections.sortedWith

class Ranges<T : Number> private constructor(
    // These affines can overlap and must be sorted by their min value.
    // Their minimum value can be assumed to be within bounds, though their max may not be.
    private val ranges: List<Affine<T>>,
    private val setIndicator: NumberSetIndicator<T, *>
) : NumberData<T> {
    override fun toString(): String = "SortedRanges($ranges)"

    fun toRangePairs(): List<Pair<T, T>> {
        return NumberUtilities.mergeAndDeduplicate(
            ranges.map { it.toRange() }
        )
    }

    override fun isConfirmedToBe(i: Int): Boolean {
        return ranges.all { it.isExactly(i) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ranges<*>) return false
        if (ranges != other.ranges) return false

        return true
    }

    override fun hashCode(): Int {
        return ranges.hashCode()
    }

    fun makeNew(ranges: List<Affine<T>>): Ranges<T> {
        return Ranges(
            ranges.sortedWith { a, b -> a.start().compareTo(b.start()) },
            setIndicator
        )
    }

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
        for (range in ranges) {
            for (otherRange in other.ranges) {
                // Find overlap
                val start = range.start().max(otherRange.start())
                val end = range.end().min(otherRange.end())

                // This could be much smarter to prevent some affine death.
                // If one range is completely contained within another, we could add the contained range
                // whole.

                // If there is an overlap, add it
                if (start <= end) {
                    newList.add(Affine.fromRangeNoKey(start, end))
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
            newList.add(Affine.fromRangeNoKey(minValue, maxValue))
            return makeNew(newList)
        }

        var currentMin = minValue
        for (range in ranges) {
            if (currentMin < range.start()) {
                newList.add(Affine.fromRangeNoKey(currentMin, range.start().downOneEpsilon()))
            }
            currentMin = range.end().upOneEpsilon()
        }

        // Add final range if necessary
        if (currentMin < maxValue) {
            newList.add(Affine.fromRangeNoKey(currentMin, maxValue))
        }

        return makeNew(newList)
    }

    companion object {
        fun <T : Number> fromConstant(constant: T, setIndicator: NumberSetIndicator<T, *>): Ranges<T> {
            return Ranges(listOf(Affine.fromConstant(constant)), setIndicator)
        }

        fun <T : Number> fromRange(start: T, end: T, key: Context.Key, ind: NumberSetIndicator<T, *>): Ranges<T> {
            return Ranges(listOf(Affine.fromRange(start, end, key)), ind)
        }
    }

    data class Affine<T : Number>(private val innerAffine: Int) {
        fun start(): T = TODO()
        fun end(): T = TODO()

        override fun toString(): String = "Affine($innerAffine)"

        fun isExactly(i: Int): Boolean = TODO()
        fun toRange(): Pair<T, T> = TODO()

        fun add(other: Affine<T>): Affine<T> = TODO()
        fun subtract(other: Affine<T>): Affine<T> = TODO()
        fun multiply(other: Affine<T>): Affine<T> = TODO()
        fun divide(other: Affine<T>): Affine<T> = TODO()

        companion object {
            fun <T : Number> fromConstant(constant: T): Affine<T> {
                return Affine(0)
            }

            fun <T : Number> fromRangeNoKey(start: T, end: T): Affine<T> {
                return Affine(0)
            }

            fun <T : Number> fromRange(start: T, end: T, key: Context.Key): Affine<T> {
                return Affine(0)
            }
        }
    }
}