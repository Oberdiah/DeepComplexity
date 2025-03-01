package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.ADDITION
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.DIVISION
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.MULTIPLICATION
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.SUBTRACTION
import com.github.oberdiah.deepcomplexity.evaluation.ByteSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.GREATER_THAN
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.GREATER_THAN_OR_EQUAL
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.LESS_THAN
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.LESS_THAN_OR_EQUAL
import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import kotlin.reflect.KClass

sealed class FullyTypedNumberSet<T : Number, Self : FullyTypedNumberSet<T, Self>>(
    private val setIndicator: NumberSetIndicator<T, Self>,
    private val ranges: Ranges<T>
) : NumberSet<Self> {
    private val clazz: KClass<*> = setIndicator.clazz

    override fun toString(): String = getAsRanges().joinToString(", ") { (start, end) ->
        if (start == end) {
            start.toString()
        } else {
            "$start..$end"
        }
    }

    class DoubleSet(ranges: Ranges<Double> = Ranges.empty(DoubleSetIndicator)) :
        FullyTypedNumberSet<Double, DoubleSet>(DoubleSetIndicator, ranges)

    class FloatSet(ranges: Ranges<Float> = Ranges.empty(FloatSetIndicator)) :
        FullyTypedNumberSet<Float, FloatSet>(FloatSetIndicator, ranges)

    class IntSet(ranges: Ranges<Int> = Ranges.empty(IntSetIndicator)) :
        FullyTypedNumberSet<Int, IntSet>(IntSetIndicator, ranges)

    class LongSet(ranges: Ranges<Long> = Ranges.empty(LongSetIndicator)) :
        FullyTypedNumberSet<Long, LongSet>(LongSetIndicator, ranges)

    class ShortSet(ranges: Ranges<Short> = Ranges.empty(ShortSetIndicator)) :
        FullyTypedNumberSet<Short, ShortSet>(ShortSetIndicator, ranges)

    class ByteSet(ranges: Ranges<Byte> = Ranges.empty(ByteSetIndicator)) :
        FullyTypedNumberSet<Byte, ByteSet>(ByteSetIndicator, ranges)

    val lazyRanges: List<Pair<T, T>> by lazy {
        ranges.toRangePairs()
    }

    override fun getRange(): Pair<Number, Number> = getRangeTyped()
    override fun getAsRanges(): List<Pair<Number, Number>> = lazyRanges

    fun getAsRangesTyped(): List<Pair<T, T>> = lazyRanges
    fun getRangeTyped(): Pair<T, T> {
        val ranges = getAsRangesTyped()
        return Pair(ranges.first().first, ranges.last().second)
    }

    override fun negate(): Self {
        return newFromRanges(Ranges.fromConstant(setIndicator.getZero(), setIndicator).subtract(ranges))
    }

    override fun union(other: Self): Self {
        assert(setIndicator == other.setIndicator)
        return newFromRanges(ranges.union(other.ranges))
    }

    override fun intersect(other: Self): Self {
        assert(setIndicator == other.setIndicator)
        return newFromRanges(ranges.intersection(other.ranges))
    }

    override fun invert(): Self {
        return newFromRanges(ranges.invert())
    }

    override fun <Q : IMoldableSet<Q>> cast(outsideInd: SetIndicator<Q>): Q {
        if (outsideInd !is NumberSetIndicator<*, *>) {
            throw IllegalArgumentException("Cannot cast a number to '$outsideInd'.")
        }

        fun <OutT : Number, OutSelf : FullyTypedNumberSet<OutT, OutSelf>> extra(outsideInd: NumberSetIndicator<OutT, OutSelf>): OutSelf {
            return newFromDataAndInd(ranges.castTo(outsideInd), outsideInd)
        }

        @Suppress("UNCHECKED_CAST")
        return extra(outsideInd) as Q
    }


    override fun arithmeticOperation(
        other: Self,
        operation: BinaryNumberOp
    ): Self {
        assert(setIndicator == other.setIndicator)
        return newFromRanges(
            when (operation) {
                ADDITION -> ranges.add(other.ranges)
                SUBTRACTION -> ranges.subtract(other.ranges)
                MULTIPLICATION -> ranges.multiply(other.ranges)
                DIVISION -> ranges.divide(other.ranges)
            }
        )
    }

    override fun comparisonOperation(
        other: Self,
        operation: ComparisonOp
    ): BooleanSet {
        assert(setIndicator == other.setIndicator)
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

    override fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<Self>,
        valid: Self
    ): Self {
        // This was half implemented in the old version.
        TODO("Not yet implemented")
    }

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right hand side of the equation.
     */
    override fun getSetSatisfying(comp: ComparisonOp): Self {
        val range = getRange()
        val smallestValue = range.first.castInto<T>(clazz)
        val biggestValue = range.second.castInto<T>(clazz)

        var newData: Ranges<T> = when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL ->
                Ranges.fromRangeNoKey(
                    setIndicator.getMinValue(),
                    smallestValue.downOneEpsilon(),
                    setIndicator
                )

            GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                Ranges.fromRangeNoKey(
                    biggestValue.upOneEpsilon(),
                    setIndicator.getMaxValue(),
                    setIndicator
                )
        }

        if (comp == LESS_THAN_OR_EQUAL) {
            newData = newData.union(ranges)
        } else if (comp == GREATER_THAN_OR_EQUAL) {
            newData = ranges.union(newData)
        }

        return newFromRanges(newData)
    }

    override fun getSetIndicator(): SetIndicator<Self> {
        return setIndicator
    }

    @Suppress("UNCHECKED_CAST")
    override fun contains(element: Any): Boolean {
        if (element::class != clazz) {
            return false
        }

        val value = element as T
        for (range in getAsRanges()) {
            if (value >= range.first && value <= range.second) {
                return true
            }
        }
        return false
    }

    private fun newFromRanges(ranges: Ranges<T>): Self {
        return newFromDataAndInd(ranges, setIndicator)
    }

    companion object {
        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromConstant(
            ind: NumberSetIndicator<T, Self>,
            value: T
        ): Self {
            return newFromDataAndInd(Ranges.fromConstant(value, ind), ind)
        }

        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromConstraints(
            ind: NumberSetIndicator<T, Self>,
            pair: Pair<T, T>,
            key: Context.Key
        ): Self {
            return newFromDataAndInd(Ranges.fromConstraints(pair.first, pair.second, key, ind), ind)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromDataAndInd(
            ranges: Ranges<T>,
            ind: NumberSetIndicator<T, Self>
        ): Self {
            return when (ind) {
                is ByteSetIndicator -> ByteSet(ranges as Ranges<Byte>)
                is ShortSetIndicator -> ShortSet(ranges as Ranges<Short>)
                is IntSetIndicator -> IntSet(ranges as Ranges<Int>)
                is LongSetIndicator -> LongSet(ranges as Ranges<Long>)
                is FloatSetIndicator -> FloatSet(ranges as Ranges<Float>)
                is DoubleSetIndicator -> DoubleSet(ranges as Ranges<Double>)
            } as Self
        }
    }
}