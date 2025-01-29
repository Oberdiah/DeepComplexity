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
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.Companion.fullPositiveRange
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.isOne
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.negate
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import kotlin.reflect.KClass

sealed class NumberSetImpl<T : Number, Self : NumberSetImpl<T, Self>>(
    private val setIndicator: NumberSetIndicator<T, Self>
) : FullyTypedNumberSet<T, Self> {
    class DoubleSet : NumberSetImpl<Double, DoubleSet>(DoubleSetIndicator), FullyTypedNumberSet.DoubleSet<DoubleSet>
    class FloatSet : NumberSetImpl<Float, FloatSet>(FloatSetIndicator), FullyTypedNumberSet.FloatSet<FloatSet>
    class IntSet : NumberSetImpl<Int, IntSet>(IntSetIndicator), FullyTypedNumberSet.IntSet<IntSet>
    class LongSet : NumberSetImpl<Long, LongSet>(LongSetIndicator), FullyTypedNumberSet.LongSet<LongSet>
    class ShortSet : NumberSetImpl<Short, ShortSet>(ShortSetIndicator), FullyTypedNumberSet.ShortSet<ShortSet>
    class ByteSet : NumberSetImpl<Byte, ByteSet>(ByteSetIndicator), FullyTypedNumberSet.ByteSet<ByteSet>

    private val clazz: KClass<*> = setIndicator.clazz

    private fun newRange(start: T, end: T): NumberRange<T, Self> {
        return NumberRange(start, end, setIndicator)
    }

    override fun getSetIndicator(): SetIndicator<Self> {
        return setIndicator
    }

    fun duplicateMe(): Self {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is ByteSet -> ByteSet()
            is ShortSet -> ShortSet()
            is IntSet -> IntSet()
            is LongSet -> LongSet()
            is FloatSet -> FloatSet()
            is DoubleSet -> DoubleSet()
        } as Self
    }

    fun me(): Self {
        @Suppress("UNCHECKED_CAST")
        return this as Self
    }

    /**
     * These ranges are always sorted and never overlap.
     */
    private val ranges = mutableListOf<NumberRange<T, Self>>()

    override fun contains(element: Any): Boolean {
        if (element::class != clazz) {
            return false
        }

        @Suppress("UNCHECKED_CAST")
        return contains(element as T)
    }

    override fun <T : NumberSet<T>> castToType(clazz: KClass<*>): T {
        if (clazz != this.clazz) {
            throw IllegalArgumentException("Cannot cast to different type — $clazz != ${this.clazz}")
        }
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    override fun addRange(start: T, end: T) {
        ranges.add(NumberRange(start, end, setIndicator))
    }

    override fun getRange(): Pair<Number, Number>? {
        val (start, end) = getRangeTyped() ?: return null
        return start to end
    }

    fun getRangeTyped(): Pair<T, T>? {
        if (ranges.isEmpty()) {
            return null
        }

        return ranges[0].start to ranges[ranges.size - 1].end
    }

    override fun toString(): String {
        if (ranges.isEmpty()) {
            return "∅"
        }

        if (ranges.size == 1 && ranges[0].start == ranges[0].end) {
            return ranges[0].start.toString()
        }

        return ranges.joinToString(", ")
    }

    private fun <Q : IMoldableSet<Q>> castToThisType(other: Q): Self {
        if (other::class != this::class) {
            throw IllegalArgumentException("Cannot perform operation on different types ($other != $this)")
        }
        @Suppress("UNCHECKED_CAST")
        return other as Self
    }

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right hand side of the equation.
     */
    override fun getSetSatisfying(comp: ComparisonOp): Self {
        val (smallestValue, biggestValue) = getRangeTyped() ?: return me()

        val newSet = duplicateMe()

        when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL -> newSet.ranges.add(
                newRange(setIndicator.getMinValue(), smallestValue.downOneEpsilon())
            )

            GREATER_THAN, GREATER_THAN_OR_EQUAL -> newSet.ranges.add(
                newRange(biggestValue.upOneEpsilon(), setIndicator.getMaxValue())
            )
        }

        if (comp == LESS_THAN_OR_EQUAL || comp == GREATER_THAN_OR_EQUAL) {
            newSet.ranges.addAll(ranges)
        }

        newSet.mergeAndDeduplicate()

        return newSet
    }

    override fun union(other: Self): Self {
        val newSet = duplicateMe()
        newSet.ranges.addAll(ranges)
        newSet.ranges.addAll(castToThisType(other).ranges)
        newSet.mergeAndDeduplicate()
        return newSet
    }

    override fun intersect(otherSet: Self): Self {
        val newSet = duplicateMe()

        // If either set is empty, intersection is empty
        if (ranges.isEmpty() || otherSet.ranges.isEmpty()) {
            return newSet
        }

        // For each range in this set, find overlapping ranges in other set
        for (range in ranges) {
            for (otherRange in otherSet.ranges) {
                // Find overlap
                val start = range.start.max(otherRange.start)
                val end = range.end.min(otherRange.end)

                // If there is an overlap, add it
                if (start <= end) {
                    newSet.addRange(start, end)
                }
            }
        }

        newSet.mergeAndDeduplicate()
        return newSet
    }

    override fun invert(): Self {
        val newSet = duplicateMe()

        val minValue = setIndicator.getMinValue()
        val maxValue = setIndicator.getMaxValue()

        if (ranges.isEmpty()) {
            newSet.addRange(minValue, maxValue)
            return newSet
        }

        var currentMin = minValue
        for (range in ranges) {
            if (currentMin < range.start) {
                newSet.addRange(currentMin, range.start.downOneEpsilon())
            }
            currentMin = range.end.upOneEpsilon()
        }

        // Add final range if necessary
        if (currentMin < maxValue) {
            newSet.addRange(currentMin, maxValue)
        }

        return newSet
    }

    override fun negate(): Self {
        val newSet = duplicateMe()
        for (range in ranges.reversed()) {
            newSet.addRange(range.end.negate(), range.start.negate())
        }
        return newSet
    }

    fun contains(value: T): Boolean {
        for (range in ranges) {
            if (value >= range.start && value <= range.end) {
                return true
            }
        }
        return false
    }

    override fun arithmeticOperation(other: Self, operation: BinaryNumberOp): Self {
        val newSet = duplicateMe()
        for (range in ranges) {
            for (otherRange in castToThisType(other).ranges) {
                val values: Iterable<NumberRange<T, Self>> = when (operation) {
                    ADDITION -> range.addition(otherRange)
                    SUBTRACTION -> range.subtraction(otherRange)
                    MULTIPLICATION -> range.multiplication(otherRange)
                    DIVISION -> range.division(otherRange)
                }

                newSet.ranges.addAll(values)
            }
        }
        newSet.mergeAndDeduplicate()
        return newSet
    }

    override fun comparisonOperation(otherSet: Self, operation: ComparisonOp): BooleanSet {
        val mySmallestPossibleValue = ranges[0].start
        val myLargestPossibleValue = ranges[ranges.size - 1].end
        val otherSmallestPossibleValue = otherSet.ranges[0].start
        val otherLargestPossibleValue = otherSet.ranges[otherSet.ranges.size - 1].end

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

    /**
     * Given a set of `terms` to apply to this set on each iteration, evaluate
     * the number of iterations required to exit the `valid` number set.
     *
     * You can think of this set as being the 'initial' state.
     */
    override fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<Self>,
        valid: Self
    ): Self {
        val gaveUp = fullPositiveRange(setIndicator)

        val linearChange = changeTerms.terms[1] ?: return gaveUp
        val constantChange = changeTerms.terms[0] ?: return gaveUp
        if (changeTerms.terms.size > 2) return gaveUp
        if (!linearChange.isOne()) {
            // We can deal with this if the constant term is 0, but we're not bothering
            // with that for now.
            return gaveUp
        }

        val zero = setIndicator.getZero()

        if (constantChange.contains(zero)) {
            // We genuinely can't do a thing, gave up is the best we're ever going to be able to do here.
            return gaveUp
        }

        var minimumNumberOfLoops = setIndicator.getMaxValue()
        var maximumNumberOfLoops = zero

        // This is obviously not a very performant way to do this, but it's a start.

        // This is mostly there, but doesn't consider the possibility of variables overflowing, so I don't
        // want to make it final yet.
//            for (deltaRange in constantChange.ranges) {
//                for (initialRange in ranges) {
//                    for (invalidRange in invalid.ranges) {
//                        // As we know deltaRange cannot contain zero, it must either be entirely positive or entirely negative.
//                        if (deltaRange.start > zero) {
//                            if (invalidRange.start < initialRange.end) {
//                                minimumNumberOfLoops = zero
//                            }
//
//                            // Going up
//                            val minLoopsUp = (invalidRange.start - initialRange.end) / deltaRange.end
//                            minimumNumberOfLoops = minimumNumberOfLoops.min(minLoopsUp)
//                            val maxLoopsUp = (invalidRange.start - initialRange.start) / deltaRange.start
//                            maximumNumberOfLoops = maximumNumberOfLoops.max(maxLoopsUp)
//                        } else {
//
//                        }
//                    }
//                }
//            }

        val newSet = duplicateMe()
        newSet.addRange(minimumNumberOfLoops, maximumNumberOfLoops)
        return newSet
    }

    private fun mergeAndDeduplicate() {
        if (ranges.size <= 1) {
            return
        }

        ranges.sortWith { a, b -> a.start.compareTo(b.start) }
        val newRanges = mutableListOf<NumberRange<T, Self>>()
        var currentRange = ranges[0]
        for (i in 1 until ranges.size) {
            val nextRange = ranges[i]
            if (currentRange.end >= nextRange.start) {
                currentRange = newRange(currentRange.start, nextRange.end.max(currentRange.end))
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

    override fun isOne(): Boolean {
        return ranges.size == 1 && ranges[0].start == ranges[0].end && ranges[0].start.isOne()
    }
}