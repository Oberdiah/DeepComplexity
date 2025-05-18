package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.github.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.github.oberdiah.deepcomplexity.utilities.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.utilities.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.utilities.Utilities.isOne
import com.github.oberdiah.deepcomplexity.utilities.Utilities.isZero
import com.github.oberdiah.deepcomplexity.utilities.Utilities.max
import com.github.oberdiah.deepcomplexity.utilities.Utilities.min
import com.github.oberdiah.deepcomplexity.utilities.Utilities.negate
import com.github.oberdiah.deepcomplexity.utilities.Utilities.upOneEpsilon
import kotlin.reflect.KClass

@ConsistentCopyVisibility
data class NumberSet<T : Number> private constructor(
    override val ind: NumberSetIndicator<T>,
    val hasThrownDivideByZero: Boolean,
    // In order, and non-overlapping
    val ranges: List<NumberRange<T>>
) : ISet<T> {
    val clazz: KClass<T> = ind.clazz

    override fun toString(): String {
        val mainStr = ranges.joinToString {
            it.toString()
        }
        val divideByZeroWarning = if (hasThrownDivideByZero) {
            " (divide by zero)"
        } else {
            ""
        }

        return "$mainStr$divideByZeroWarning"
    }

    override fun isEmpty(): Boolean = ranges.isEmpty()

    override fun isFull(): Boolean {
        val range = getRange()
        return range.first == ind.getMinValue() && range.second == ind.getMaxValue()
    }

    private fun makeNew(ranges: List<NumberRange<T>>, thrownDivByZero: Boolean? = null): NumberSet<T> {
        return NumberSet(
            ind,
            thrownDivByZero ?: hasThrownDivideByZero,
            NumberUtilities.mergeAndDeduplicate(ranges)
        )
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

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(newInd: NumberSetIndicator<OutT>): NumberSet<OutT> {
            assert(newInd.isWholeNum() && ind.isWholeNum()) {
                "Attempted to cast to a floating point number."
            }
            return NumberSet(
                newInd,
                hasThrownDivideByZero,
                NumberUtilities.mergeAndDeduplicate(ranges.flatMap { it.castTo(newInd) })
            )
        }

        @Suppress("UNCHECKED_CAST")
        // Safety: Trivially true by checking the signature of extra().
        return extra(newInd) as ISet<Q>
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
            // No point doing the ranges^2 stuff here.
            MODULO -> return doModulo(other)
        }

        var hasThrownDivideByZero = false
        val newList: MutableList<NumberRange<T>> = mutableListOf()
        for (range in ranges) {
            for (otherRange in other.ranges) {
                val ranges = rangeOp(range, otherRange)

                if (ranges.any { it == null }) {
                    hasThrownDivideByZero = true
                }
                newList.addAll(ranges.filterNotNull())
            }
        }

        return makeNew(newList, hasThrownDivideByZero)
    }

    private fun doModulo(other: NumberSet<T>): NumberSet<T> {
        val otherRange = other.getRange()
        val maxOther = otherRange.first.negate().max(otherRange.second)

        val positiveSet = newFromConstant(maxOther).getSetSatisfying(LESS_THAN)
        val negativeSet = newFromConstant(maxOther.negate()).getSetSatisfying(GREATER_THAN)

        return if (comparisonOperation(zero(ind), GREATER_THAN_OR_EQUAL) == BooleanSet.TRUE) {
            positiveSet.intersect(ind.positiveNumbersAndZero())
        } else {
            negativeSet.intersect(positiveSet)
        }
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

            EQUAL -> {
                if (mySmallestPossibleValue == myLargestPossibleValue &&
                    otherSmallestPossibleValue == otherLargestPossibleValue &&
                    mySmallestPossibleValue == otherSmallestPossibleValue
                ) { // We can only sure we are equal if we're both literally a single value, otherwise there's uncertainty there.
                    return BooleanSet.TRUE
                } else if (myLargestPossibleValue < otherSmallestPossibleValue ||
                    mySmallestPossibleValue > otherLargestPossibleValue
                ) {
                    return BooleanSet.FALSE
                }
            }

            NOT_EQUAL -> {
                if (myLargestPossibleValue < otherSmallestPossibleValue ||
                    mySmallestPossibleValue > otherLargestPossibleValue
                ) {
                    return BooleanSet.TRUE
                } else if (mySmallestPossibleValue == myLargestPossibleValue &&
                    otherSmallestPossibleValue == otherLargestPossibleValue &&
                    mySmallestPossibleValue == otherSmallestPossibleValue
                ) {
                    return BooleanSet.FALSE
                }
            }
        }

        return BooleanSet.BOTH
    }

    fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.CollectedTerms<T>,
        valid: NumberSet<T>
    ): NumberSet<T> {
        // This was half-implemented in the old version.
        TODO("Not yet implemented")
    }

    override fun toConstVariance(): Variances<T> = NumberVariances.Companion.newFromConstant(this)

    /**
     * Returns a new set containing everything that could satisfy the comparison operation.
     * I.e. for equality, the entire range is returned as the entire range may return true.
     * For inequality, all values are returned, unless we are a single value.
     * We're the right-hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): NumberSet<T> {
        val range = getRange()
        val smallestValue = range.first.castInto<T>(clazz)
        val biggestValue = range.second.castInto<T>(clazz)

        var newData: List<NumberRange<T>> =
            when (comp) {
                LESS_THAN_OR_EQUAL ->
                    listOf(NumberRange.new(ind.getMinValue(), biggestValue))

                LESS_THAN ->
                    listOf(NumberRange.new(ind.getMinValue(), biggestValue.downOneEpsilon()))

                GREATER_THAN_OR_EQUAL ->
                    listOf(NumberRange.new(smallestValue, ind.getMaxValue()))

                GREATER_THAN ->
                    listOf(NumberRange.new(smallestValue.upOneEpsilon(), ind.getMaxValue()))

                EQUAL -> ranges
                NOT_EQUAL -> {
                    if (smallestValue == biggestValue) {
                        listOf(
                            NumberRange.new(
                                ind.getMinValue(),
                                smallestValue.downOneEpsilon()
                            ),
                            NumberRange.new(
                                biggestValue.upOneEpsilon(),
                                ind.getMaxValue()
                            )
                        )
                    } else {
                        // In this case we can't say anything at all, we have to return the entire range :(
                        listOf(NumberRange.new(ind.getMinValue(), ind.getMaxValue()))
                    }
                }
            }


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

    override fun union(other: ISet<T>): NumberSet<T> {
        assert(ind == other.ind)

        return makeNew(ranges + other.into().ranges)
    }

    override fun intersect(other: ISet<T>): NumberSet<T> {
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

    fun isZero(): Boolean {
        return ranges.size == 1 && ranges[0].start == ranges[0].end && ranges[0].start.isZero()
    }

    companion object {
        fun <T : Number> zero(ind: NumberSetIndicator<T>): NumberSet<T> = NumberSet(
            ind,
            false,
            listOf(NumberRange.fromConstant(ind.getZero())),
        )

        fun <T : Number> one(ind: NumberSetIndicator<T>): NumberSet<T> = NumberSet(
            ind,
            false,
            listOf(NumberRange.fromConstant(ind.getOne())),
        )

        fun <T : Number> newFromConstant(
            constant: T
        ): NumberSet<T> {
            val ind = SetIndicator.fromValue(constant)
            return NumberSet(
                ind,
                false,
                listOf(NumberRange.fromConstant(constant)),
            )
        }

        fun <T : Number> newEmpty(ind: NumberSetIndicator<T>): NumberSet<T> =
            NumberSet(ind, false, emptyList())

        fun <T : Number> newFull(ind: NumberSetIndicator<T>): NumberSet<T> =
            NumberSet(ind, false, listOf(NumberRange.new(ind.getMinValue(), ind.getMaxValue())))
    }
}