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
    fun getKeys(): List<Context.Key> = ranges.flatMap { it.getKeys() }

    override fun toString(): String = ranges.joinToString(", ") { it.stringOverview() }

    fun <NewT : Number> castTo(newInd: NumberSetIndicator<NewT, *>): Ranges<NewT> {
        assert(newInd.isWholeNum()) // We can't handle/haven't thought about floating point yet.

        if (newInd.getMaxValue() > setIndicator.getMaxValue()) {
            // We're enlarging the possibility space. This means our optimization of keeping unlimited-sized affines
            // around until the final range-pairs step is no longer valid.
            // For example, (short) ((byte) x) + 5 has a range of [-123, 132]
            val newRanges = mutableListOf<Affine<NewT>>()
            for (affine in ranges) {
                val newAffine = affine.castTo(newInd)
                val affineRanges = newAffine.toRanges()

                if (affineRanges.size == 1) {
                    // We don't wrap under the new casting, we can keep the affine alive :)
                    newRanges.add(newAffine)
                } else {
                    // We need to split the affine into multiple affines.
                    for (range in affineRanges) {
                        newRanges.add(Affine.fromRangeNoKey(range.first, range.second, newInd))
                    }
                }
            }

            return Ranges(newRanges, newInd)
        } else {
            // When we're shrinking things, I think (?) we're fine to stay as-is and just label with the new indicator.
            // I could be wrong here, but this feels intuitively correct to me.
            return Ranges(ranges.map { it.castTo(newInd) }, newInd)
        }
    }

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
        for (affine in ranges) {
            for (otherAffine in other.ranges) {
                val ranges = affine.toRanges()
                val otherRanges = otherAffine.toRanges()
                for (range in ranges) {
                    for (otherRange in otherRanges) {
                        // Find overlap
                        val start = range.first.max(otherRange.first)
                        val end = range.second.min(otherRange.second)

                        // If there is an overlap, add it
                        if (start <= end) {
                            val avoidedWrapping = ranges.size == 1 && otherRanges.size == 1
                            val firstRangeEntirelyEnclosed = range.first == start && range.second == end
                            val secondRangeEntirelyEnclosed = otherRange.first == start && otherRange.second == end

                            // This prioritizes affine 1 over affine 2. We may want to change this.
                            if (avoidedWrapping && firstRangeEntirelyEnclosed) {
                                newList.add(affine)
                            } else if (avoidedWrapping && secondRangeEntirelyEnclosed) {
                                newList.add(otherAffine)
                            } else {
                                newList.add(Affine.fromRangeNoKey(start, end, setIndicator))
                            }
                        }
                    }
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