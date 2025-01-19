package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ByteSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.settings.Settings
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.ALLOW
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.CLAMP
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.ByteSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.DoubleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.FloatSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.IntSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.LongSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.ShortSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMaxValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMinValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getOne
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getSetSize
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getZero
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.isFloatingPoint
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.isOne
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.negate
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import java.math.BigInteger
import kotlin.reflect.KClass

sealed interface NumberSet<Self> : IMoldableSet<Self> where Self : IMoldableSet<Self>, Self : NumberSet<Self> {
    fun <T : NumberSet<T>> castToType(clazz: KClass<*>): T
    fun arithmeticOperation(other: Self, operation: BinaryNumberOp): Self
    fun comparisonOperation(other: Self, operation: ComparisonOp): BooleanSet
    fun addRange(start: Number, end: Number)

    /**
     * Returns the range of the set. i.e., the smallest value in the set and the largest.
     *
     * Returns null if the set is empty.
     */
    fun getRange(): Pair<Number, Number>?
    fun negate(): Self
    fun isOne(): Boolean

    /**
     * Given a set of `terms` to apply to this set on each iteration, evaluate
     * the number of iterations required to exit the `valid` number set.
     *
     * You can think of this set as being the 'initial' state.
     */
    fun evaluateLoopingRange(changeTerms: ConstraintSolver.EvaluatedCollectedTerms<Self>, valid: Self): Self

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): Self

    companion object {
        fun <T : NumberSet<T>> newFromIndicator(indicator: SetIndicator<T>): T {
            @Suppress("UNCHECKED_CAST")
            return when (indicator) {
                ByteSetIndicator -> ByteSet()
                ShortSetIndicator -> ShortSet()
                IntSetIndicator -> IntSet()
                LongSetIndicator -> LongSet()
                FloatSetIndicator -> FloatSet()
                DoubleSetIndicator -> DoubleSet()
                BooleanSetIndicator, GenericSetIndicator ->
                    throw IllegalArgumentException("Cannot create number set from boolean or generic indicator")
            } as T
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> fullRange(indicator: NumberSetIndicator<T, Self>): Self {
            val set = newFromIndicator(indicator)
            set.addRange(indicator.clazz.getMinValue(), indicator.clazz.getMaxValue())
            return set
        }

        fun <T : NumberSet<T>> fullPositiveRange(indicator: SetIndicator<T>): T {
            val set = newFromIndicator(indicator)
            set.addRange(indicator.clazz.getZero(), indicator.clazz.getMaxValue())
            return set
        }

        fun <T : NumberSet<T>> fullNegativeRange(indicator: SetIndicator<T>): T {
            val set = newFromIndicator(indicator)
            set.addRange(indicator.clazz.getMinValue(), indicator.clazz.getZero())
            return set
        }

        fun <T : NumberSet<T>> emptyRange(indicator: SetIndicator<T>): T {
            return newFromIndicator(indicator)
        }

        fun <T : NumberSet<T>> zero(indicator: SetIndicator<T>): T {
            val set = newFromIndicator(indicator)
            set.addRange(indicator.clazz.getZero(), indicator.clazz.getZero())
            return set
        }

        fun <T : NumberSet<T>> one(indicator: SetIndicator<T>): T {
            val set = newFromIndicator(indicator)
            set.addRange(indicator.clazz.getOne(), indicator.clazz.getOne())
            return set
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> singleValue(value: T): Self {
            val set: Self = newFromIndicator(SetIndicator.fromValue(value))
            set.addRange(value, value)
            return set
        }
    }

    sealed class NumberSetImpl<T : Number, Self : NumberSetImpl<T, Self>>(
        private val setIndicator: SetIndicator<Self>
    ) : NumberSet<Self> {
        class DoubleSet : NumberSetImpl<Double, DoubleSet>(DoubleSetIndicator)
        class FloatSet : NumberSetImpl<Float, FloatSet>(FloatSetIndicator)
        class LongSet : NumberSetImpl<Long, LongSet>(LongSetIndicator)
        class IntSet : NumberSetImpl<Int, IntSet>(IntSetIndicator)
        class ShortSet : NumberSetImpl<Short, ShortSet>(ShortSetIndicator)
        class ByteSet : NumberSetImpl<Byte, ByteSet>(ByteSetIndicator)

        private val clazz: KClass<*> = setIndicator.clazz

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
        private val ranges = mutableListOf<NumberRange>()

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

        override fun addRange(start: Number, end: Number) {
            addRangeTyped(start.castInto(clazz), end.castInto(clazz))
        }

        fun addRangeTyped(start: T, end: T) {
            ranges.add(NumberRange(start, end))
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
                    NumberRange(
                        clazz.getMinValue().castInto(clazz),
                        smallestValue.downOneEpsilon()
                    )
                )

                GREATER_THAN, GREATER_THAN_OR_EQUAL -> {
                    newSet.ranges.add(
                        NumberRange(
                            biggestValue.upOneEpsilon(),
                            clazz.getMaxValue().castInto(clazz)
                        )
                    )
                }
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
                        newSet.addRangeTyped(start, end)
                    }
                }
            }

            newSet.mergeAndDeduplicate()
            return newSet
        }

        override fun invert(): Self {
            val newSet = duplicateMe()

            val minValue = clazz.getMinValue().castInto<T>(clazz)
            val maxValue = clazz.getMaxValue().castInto<T>(clazz)

            if (ranges.isEmpty()) {
                newSet.addRangeTyped(minValue, maxValue)
                return newSet
            }

            var currentMin = minValue
            for (range in ranges) {
                if (currentMin < range.start) {
                    newSet.addRangeTyped(currentMin, range.start.downOneEpsilon())
                }
                currentMin = range.end.upOneEpsilon()
            }

            // Add final range if necessary
            if (currentMin < maxValue) {
                newSet.addRangeTyped(currentMin, maxValue)
            }

            return newSet
        }

        override fun negate(): Self {
            val newSet = duplicateMe()
            for (range in ranges.reversed()) {
                newSet.addRangeTyped(range.end.negate(), range.start.negate())
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
                    val values: Iterable<NumberRange> = when (operation) {
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

            val zero = clazz.getZero().castInto<T>(clazz)

            if (constantChange.contains(zero)) {
                // We genuinely can't do a thing, gave up is the best we're ever going to be able to do here.
                return gaveUp
            }

            var minimumNumberOfLoops = clazz.getMaxValue().castInto<T>(clazz)
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
            newSet.addRangeTyped(minimumNumberOfLoops, maximumNumberOfLoops)
            return newSet
        }

        private fun mergeAndDeduplicate() {
            if (ranges.size <= 1) {
                return
            }

            ranges.sortWith { a, b -> a.start.compareTo(b.start) }
            val newRanges = mutableListOf<NumberRange>()
            var currentRange = ranges[0]
            for (i in 1 until ranges.size) {
                val nextRange = ranges[i]
                if (currentRange.end >= nextRange.start) {
                    currentRange = NumberRange(currentRange.start, nextRange.end.max(currentRange.end))
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

        /**
         * The start may be equal to the end.
         */
        private inner class NumberRange(val start: T, val end: T) {
            init {
                assert(start <= end) {
                    "Start ($start) must be less than or equal to end ($end)"
                }
            }

            override fun toString(): String {
                if (start == end) {
                    return start.toString()
                }

                return "[$start, $end]"
            }

            fun addition(other: NumberRange): Iterable<NumberRange> {
                return if (clazz.isFloatingPoint()) {
                    listOf(NumberRange(start + other.start, end + other.end))
                } else {
                    resolvePotentialOverflow(
                        BigInteger.valueOf(start.toLong()).add(BigInteger.valueOf(other.start.toLong())),
                        BigInteger.valueOf(end.toLong()).add(BigInteger.valueOf(other.end.toLong())),
                    )
                }
            }

            fun subtraction(other: NumberRange): Iterable<NumberRange> {
                return if (clazz.isFloatingPoint()) {
                    listOf(NumberRange(start - other.end, end - other.start))
                } else {
                    resolvePotentialOverflow(
                        BigInteger.valueOf(start.toLong()).subtract(BigInteger.valueOf(other.end.toLong())),
                        BigInteger.valueOf(end.toLong()).subtract(BigInteger.valueOf(other.start.toLong())),
                    )
                }
            }

            fun multiplication(other: NumberRange): Iterable<NumberRange> {
                return if (clazz.isFloatingPoint()) {
                    val a = start * other.start
                    val b = start * other.end
                    val c = end * other.start
                    val d = end * other.end
                    listOf(
                        NumberRange(
                            a.min(b).min(c).min(d),
                            a.max(b).max(c).max(d),
                        )
                    )
                } else {
                    val a = multiply(start, other.start)
                    val b = multiply(end, other.start)
                    val c = multiply(start, other.end)
                    val d = multiply(end, other.end)

                    resolvePotentialOverflow(
                        a.min(b).min(c).min(d),
                        a.max(b).max(c).max(d),
                    )
                }
            }

            fun division(other: NumberRange): Iterable<NumberRange> {
                return if (clazz.isFloatingPoint()) {
                    val a = start / other.start
                    val b = start / other.end
                    val c = end / other.start
                    val d = end / other.end
                    listOf(
                        NumberRange(
                            a.min(b).min(c).min(d),
                            a.max(b).max(c).max(d),
                        )
                    )
                } else {
                    val a = divide(start, other.start)
                    val b = divide(end, other.start)
                    val c = divide(start, other.end)
                    val d = divide(end, other.end)

                    resolvePotentialOverflow(
                        a.min(b).min(c).min(d),
                        a.max(b).max(c).max(d),
                    )
                }
            }

            private fun multiply(a: Number, b: Number): BigInteger {
                return BigInteger.valueOf(a.toLong()).multiply(BigInteger.valueOf(b.toLong()))
            }

            private fun divide(a: Number, b: Number): BigInteger {
                if (b.toLong() == 0L) {
                    // Could potentially warn of overflow here one day?
                    return if (a > 0) {
                        BigInteger.valueOf(clazz.getMaxValue().toLong())
                    } else {
                        BigInteger.valueOf(clazz.getMinValue().toLong())
                    }
                }

                return BigInteger.valueOf(a.toLong()).divide(BigInteger.valueOf(b.toLong()))
            }

            private fun bigIntToT(v: BigInteger): T {
                return v.longValueExact().castInto(clazz)
            }

            private fun resolvePotentialOverflow(min: BigInteger, max: BigInteger): Iterable<NumberRange> {
                val minValue = BigInteger.valueOf(clazz.getMinValue().toLong())
                val maxValue = BigInteger.valueOf(clazz.getMaxValue().toLong())

                when (Settings.overflowBehaviour) {
                    CLAMP -> {
                        return listOf(
                            NumberRange(
                                bigIntToT(min.max(minValue)),
                                bigIntToT(max.min(maxValue))
                            )
                        )
                    }

                    ALLOW -> {
                        // Check if we're overflowing and if we are, we must split the range.
                        // If we're overflowing in both directions we can just return the full range.

                        if (min < minValue && max > maxValue) {
                            return listOf(NumberRange(bigIntToT(minValue), bigIntToT(maxValue)))
                        } else if (min < minValue) {
                            val wrappedMin = min.add(clazz.getSetSize())
                            if (wrappedMin < minValue) {
                                // We're overflowing so much in a single direction
                                // that the overflow will cover the full range anyway.
                                return listOf(NumberRange(bigIntToT(minValue), bigIntToT(maxValue)))
                            }
                            return listOf(
                                NumberRange(bigIntToT(wrappedMin), bigIntToT(maxValue)),
                                NumberRange(bigIntToT(minValue), bigIntToT(max))
                            )
                        } else if (max > maxValue) {
                            val wrappedMax = max.subtract(clazz.getSetSize())
                            if (wrappedMax > maxValue) {
                                // We're overflowing so much in a single direction
                                // that the overflow will cover the full range anyway.
                                return listOf(NumberRange(bigIntToT(minValue), bigIntToT(maxValue)))
                            }
                            return listOf(
                                NumberRange(bigIntToT(minValue), bigIntToT(wrappedMax)),
                                NumberRange(bigIntToT(min), bigIntToT(maxValue))
                            )
                        } else {
                            return listOf(NumberRange(bigIntToT(min), bigIntToT(max)))
                        }
                    }
                }
            }
        }
    }
}
