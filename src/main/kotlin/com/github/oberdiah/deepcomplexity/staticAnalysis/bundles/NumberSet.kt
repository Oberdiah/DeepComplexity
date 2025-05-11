package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.github.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.github.oberdiah.deepcomplexity.utilities.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.utilities.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.utilities.Utilities.isOne
import com.github.oberdiah.deepcomplexity.utilities.Utilities.max
import com.github.oberdiah.deepcomplexity.utilities.Utilities.min
import com.github.oberdiah.deepcomplexity.utilities.Utilities.upOneEpsilon
import kotlin.reflect.KClass

typealias Ranges<T> = List<NumberRange<T>>

class NumberSet<T : Number>(
    override val ind: NumberSetIndicator<T>,
    // In order, and non-overlapping
    val ranges: Ranges<T>
) : Bundle<T> {
    val clazz: KClass<T> = ind.clazz

    override fun toString(): String = ranges.joinToString {
        it.toString()
    }

    override fun isEmpty(): Boolean = ranges.isEmpty()

    private fun makeNew(ranges: Ranges<T>): NumberSet<T> {
        return newFromDataAndInd(ind, NumberUtilities.mergeAndDeduplicate(ranges))
    }

    /**
     * Returns the full range of this number set (Smallest possible value to largest)
     */
    fun getRange(): Pair<T, T> {
        val smallest = ranges.first().start
        val largest = ranges.last().end
        return Pair(smallest, largest)
    }

    fun negate(): NumberSet<T> {
        val zero = NumberRange.fromConstant(ind.getZero())
        return makeNew(ranges.flatMap { elem -> zero.subtract(elem) })
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(newInd: NumberSetIndicator<OutT>): NumberSet<OutT> {
            assert(newInd.isWholeNum() && ind.isWholeNum()) // We can't handle/haven't thought about floating point casting yet.
            return newFromDataAndInd(newInd, ranges.flatMap { it.castTo(newInd) })
        }

        @Suppress("UNCHECKED_CAST")
        // Safety: Trivially true by checking the signature of extra().
        return extra(newInd) as Bundle<Q>
    }

    fun add(other: NumberSet<T>): NumberSet<T> = arithmeticOperation(other, ADDITION)
    fun subtract(other: NumberSet<T>): NumberSet<T> = arithmeticOperation(other, SUBTRACTION)
    fun multiply(other: NumberSet<T>): NumberSet<T> = arithmeticOperation(other, MULTIPLICATION)
    fun divide(other: NumberSet<T>): NumberSet<T> = arithmeticOperation(other, DIVISION)

    fun arithmeticOperation(
        other: NumberSet<T>,
        operation: BinaryNumberOp
    ): NumberSet<T> {
        assert(ind == other.ind)

        val rangeOp = when (operation) {
            ADDITION -> NumberRange<T>::add
            SUBTRACTION -> NumberRange<T>::subtract
            MULTIPLICATION -> NumberRange<T>::multiply
            DIVISION -> NumberRange<T>::divide
        }

        val newList: MutableList<NumberRange<T>> = mutableListOf()
        for (range in ranges) {
            for (otherRange in other.ranges) {
                newList.addAll(rangeOp(range, otherRange))
            }
        }

        return makeNew(newList)
    }

    fun comparisonOperation(
        other: NumberSet<T>,
        operation: ComparisonOp
    ): BooleanSet {
        assert(ind == other.ind)
        val (mySmallestPossibleValue, myLargestPossibleValue) = getRange()
        val (otherSmallestPossibleValue, otherLargestPossibleValue) = other.getRange()

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

    fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<T>,
        valid: NumberSet<T>
    ): NumberSet<T> {
        // This was half-implemented in the old version.
        TODO("Not yet implemented")
    }

    override fun withVariance(key: Context.Key): NumberVariances<T> =
        NumberVariances.Companion.newFromVariance(ind, key)

    override fun toConstVariance(): Variances<T> = NumberVariances.Companion.newFromConstant(this)

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right-hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): NumberSet<T> {
        val range = getRange()
        val smallestValue = range.first.castInto<T>(clazz)
        val biggestValue = range.second.castInto<T>(clazz)

        var newData: List<NumberRange<T>> = listOf(
            when (comp) {
                LESS_THAN, LESS_THAN_OR_EQUAL ->
                    NumberRange.new(
                        ind.getMinValue(),
                        smallestValue.downOneEpsilon()
                    )

                GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                    NumberRange.new(
                        biggestValue.upOneEpsilon(),
                        ind.getMaxValue()
                    )
            }
        )

        if (comp == LESS_THAN_OR_EQUAL) {
            newData = newData + ranges
        } else if (comp == GREATER_THAN_OR_EQUAL) {
            newData = ranges + newData
        }

        return makeNew(newData)
    }

    override fun contains(element: T): Boolean {
        val value = element
        for (range in ranges) {
            if (value >= range.start && value <= range.end) {
                return true
            }
        }
        return false
    }

    override fun union(other: Bundle<T>): NumberSet<T> {
        assert(ind == other.ind)

        return makeNew(ranges + other.into().ranges)
    }

    override fun intersect(other: Bundle<T>): NumberSet<T> {
        assert(ind == other.ind)

        val newList: MutableList<NumberRange<T>> = mutableListOf()
        // For each range in this set, find overlapping ranges in other set
        for (range in ranges) {
            for (otherRange in other.into().ranges) {
                // Find overlap
                val start = range.start.max(otherRange.start)
                val end = range.end.min(otherRange.end)

                // If there is an overlap, add it
                if (start <= end) {
                    newList.add(NumberRange.new(start, end))
                }
            }
        }

        return makeNew(newList)
    }

    override fun invert(): NumberSet<T> {
        val newList: MutableList<NumberRange<T>> = mutableListOf()

        // Affine death is inevitable here.
        val minValue = ind.getMinValue()
        val maxValue = ind.getMaxValue()

        if (ranges.isEmpty()) {
            return makeNew(listOf(NumberRange.new(minValue, maxValue)))
        }

        var currentMin = minValue

        for (range in ranges) {
            if (currentMin < range.start) {
                newList.add(NumberRange.new(currentMin, range.start.downOneEpsilon()))
            }
            currentMin = range.end.upOneEpsilon()
        }

        // Add final range if necessary
        if (currentMin < maxValue) {
            newList.add(NumberRange.new(currentMin, maxValue))
        }

        return makeNew(newList)
    }

    fun isOne(): Boolean {
        return ranges.size == 1 && ranges[0].start == ranges[0].end && ranges[0].start.isOne()
    }

    companion object {
        fun <T : Number> zero(ind: NumberSetIndicator<T>): NumberSet<T> = newFromDataAndInd(
            ind,
            listOf(NumberRange.fromConstant(ind.getZero())),
        )

        fun <T : Number> one(ind: NumberSetIndicator<T>): NumberSet<T> = newFromDataAndInd(
            ind,
            listOf(NumberRange.fromConstant(ind.getOne())),
        )

        fun <T : Number> newFromConstant(
            constant: T
        ): NumberSet<T> {
            val ind = SetIndicator.fromValue(constant)
            return newFromDataAndInd(
                ind,
                listOf(NumberRange.fromConstant(constant)),
            )
        }

        fun <T : Number> newFull(ind: NumberSetIndicator<T>): NumberSet<T> =
            newFromDataAndInd(ind, listOf(NumberRange.new(ind.getMinValue(), ind.getMaxValue())))

        @Suppress("UNCHECKED_CAST")
        private fun <T : Number> newFromDataAndInd(
            ind: NumberSetIndicator<T>,
            ranges: Ranges<T>,
        ): NumberSet<T> {
            return NumberSet(ind, ranges)
        }
    }
}