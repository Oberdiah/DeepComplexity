package com.oberdiah.deepcomplexity.staticAnalysis.sets

import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import com.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.oberdiah.deepcomplexity.utilities.Utilities.compareTo
import com.oberdiah.deepcomplexity.utilities.Utilities.downOneEpsilon
import com.oberdiah.deepcomplexity.utilities.Utilities.isOne
import com.oberdiah.deepcomplexity.utilities.Utilities.isZero
import com.oberdiah.deepcomplexity.utilities.Utilities.max
import com.oberdiah.deepcomplexity.utilities.Utilities.min
import com.oberdiah.deepcomplexity.utilities.Utilities.negate
import com.oberdiah.deepcomplexity.utilities.Utilities.upOneEpsilon
import kotlin.reflect.KClass

@ConsistentCopyVisibility
data class NumberSet<T : Number> private constructor(
    override val ind: NumberIndicator<T>,
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
        if (ranges.isEmpty()) {
            return false
        }

        val range = getRange()
        return range.first == ind.getMinValue() && range.second == ind.getMaxValue()
    }

    /**
     * Use when called from a binary operation context.
     * `other` is for tracking hasThrownDivideByZero. You can override this if you want with `thrownDivByZero`.
     */
    private fun binaryMakeNew(
        other: NumberSet<T>,
        ranges: List<NumberRange<T>>,
        thrownDivByZero: Boolean? = null
    ): NumberSet<T> {
        return NumberSet(
            ind,
            thrownDivByZero ?: (other.hasThrownDivideByZero || hasThrownDivideByZero),
            NumberUtilities.mergeAndDeduplicate(ranges)
        )
    }

    private fun unaryMakeNew(ranges: List<NumberRange<T>>, thrownDivByZero: Boolean? = null): NumberSet<T> {
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
        return unaryMakeNew(ranges.flatMap { elem -> zero.subtract(elem) })
    }

    /**
     * Like a regular cast, but rather than wrapping it will clamp instead.
     */
    fun <Q : Any> clampCast(newInd: Indicator<Q>): ISet<Q>? {
        // Intersect with the new indicator's full set, removing anything that might
        // result in wrapping, and then cast to the new indicator, guaranteed wrap-free.
        val newIndFullSet = newInd.newFullSet().cast(ind) ?: return null
        return this.intersect(newIndFullSet).cast(newInd)
    }

    override fun <Q : Any> cast(newInd: Indicator<Q>): ISet<Q>? {
        if (newInd !is NumberIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(newInd: NumberIndicator<OutT>): NumberSet<OutT> {
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

        if (ranges.size > 10) {
            throw RuntimeException("NumberSet has more than 10 ranges, this may be slow.")
        }

        var hasThrownDivideByZero = this.hasThrownDivideByZero || other.hasThrownDivideByZero
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

        return binaryMakeNew(other, newList, hasThrownDivideByZero)
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

    override fun comparisonOperation(
        other: ISet<T>,
        operation: ComparisonOp
    ): BooleanSet {
        if (isEmpty() || other.isEmpty()) {
            return BooleanSet.NEITHER
        }
        val other = other.into()

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
                ) {
                    // We can only sure we are equal if we're both literally a single value, otherwise there's uncertainty there.
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
                    // We know for sure we're equal, thus NOT_EQUAL is guaranteed false.
                    return BooleanSet.FALSE
                }
            }
        }

        return BooleanSet.EITHER
    }

    override fun toConstVariance(): Variances<T> = NumberVariances.newFromConstant(this)

    /**
     * Returns a new set containing everything that could satisfy the comparison operation.
     * I.e. for equality, the entire range is returned as the entire range may return true.
     * For inequality, all values are returned, unless we are a single value.
     * We're the right-hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): NumberSet<T> {
        if (ranges.isEmpty()) {
            return this
        }

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

        return unaryMakeNew(newData)
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

        return binaryMakeNew(other.into(), ranges + other.into().ranges)
    }

    override fun intersect(other: ISet<T>): NumberSet<T> {
        assert(ind == other.ind)
        val other = other.into()

        val newList: MutableList<NumberRange<T>> = mutableListOf()
        // For each range in this set, find overlapping ranges in other set
        for (range in ranges) {
            for (otherRange in other.ranges) {
                // Find overlap
                val start = range.start.max(otherRange.start)
                val end = range.end.min(otherRange.end)

                // If there is an overlap, add it
                if (start <= end) {
                    newList.add(NumberRange.new(start, end))
                }
            }
        }

        return binaryMakeNew(other, newList)
    }

    override fun size(): Long? {
        var total: Long = 0
        for (range in ranges) {
            val rangeSize = range.size() ?: return null
            total += rangeSize
            if (total < 0) {
                // Overflow, just return null
                return null
            }
        }
        return total
    }

    override fun invert(): NumberSet<T> {
        val newList: MutableList<NumberRange<T>> = mutableListOf()

        val minValue = ind.getMinValue()
        val maxValue = ind.getMaxValue()

        if (ranges.isEmpty()) {
            return unaryMakeNew(listOf(NumberRange.new(minValue, maxValue)))
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

        return unaryMakeNew(newList)
    }

    fun isOne(): Boolean {
        return ranges.size == 1 && ranges[0].start == ranges[0].end && ranges[0].start.isOne()
    }

    fun isZero(): Boolean {
        return ranges.size == 1 && ranges[0].start == ranges[0].end && ranges[0].start.isZero()
    }

    companion object {
        fun <T : Number> zero(ind: NumberIndicator<T>): NumberSet<T> = NumberSet(
            ind,
            false,
            listOf(NumberRange.fromConstant(ind.getZero())),
        )

        fun <T : Number> one(ind: NumberIndicator<T>): NumberSet<T> = NumberSet(
            ind,
            false,
            listOf(NumberRange.fromConstant(ind.getOne())),
        )

        fun <T : Number> newFromConstant(
            constant: T
        ): NumberSet<T> {
            val ind = NumberIndicator.fromValue(constant)
            return NumberSet(
                ind,
                false,
                listOf(NumberRange.fromConstant(constant)),
            )
        }

        fun <T : Number> newEmpty(ind: NumberIndicator<T>): NumberSet<T> =
            NumberSet(ind, false, emptyList())

        fun <T : Number> newFull(ind: NumberIndicator<T>): NumberSet<T> =
            NumberSet(ind, false, listOf(NumberRange.new(ind.getMinValue(), ind.getMaxValue())))
    }
}