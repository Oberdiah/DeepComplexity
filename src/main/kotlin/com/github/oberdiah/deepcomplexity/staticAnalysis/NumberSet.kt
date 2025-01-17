package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.github.oberdiah.deepcomplexity.settings.Settings
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.ALLOW
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.CLAMP
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
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
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.negate
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import java.math.BigInteger
import kotlin.reflect.KClass

sealed interface NumberSet : IMoldableSet<NumberSet> {
    override fun getSetClass(): KClass<*> {
        return NumberSet::class
    }

    fun arithmeticOperation(other: NumberSet, operation: BinaryNumberOp): NumberSet
    fun comparisonOperation(other: NumberSet, operation: ComparisonOp): BooleanSet
    fun addRange(start: Number, end: Number)
    fun <T : Number> castToType(clazz: KClass<*>): NumberSetImpl<T>

    /**
     * Returns the range of the set. i.e., the smallest value in the set and the largest.
     *
     * Returns null if the set is empty.
     */
    fun getRange(): Pair<Number, Number>?
    fun negate(): NumberSet
    fun isOne(): Boolean

    /**
     * Given a set of `terms` to apply to this set on each iteration, evaluate
     * the number of iterations required to exit the `valid` number set.
     *
     * You can think of this set as being the 'initial' state.
     */
    fun evaluateLoopingRange(changeTerms: ConstraintSolver.EvaluatedCollectedTerms, valid: NumberSet): NumberSet

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): NumberSet

    fun toImpl(): NumberSetImpl<*> {
        return this as NumberSetImpl<*>
    }

    companion object {
        fun newFromClass(clazz: KClass<*>): NumberSet {
            return when (clazz) {
                Double::class -> NumberSetImpl<Double>(clazz)
                Float::class -> NumberSetImpl<Float>(clazz)
                Long::class -> NumberSetImpl<Long>(clazz)
                Int::class -> NumberSetImpl<Int>(clazz)
                Short::class -> NumberSetImpl<Short>(clazz)
                Byte::class -> NumberSetImpl<Byte>(clazz)
                else -> throw IllegalArgumentException("Unknown number class")
            }
        }

        fun <T : Number> newFromClassTyped(clazz: KClass<*>): NumberSetImpl<T> {
            return when (clazz) {
                Double::class -> NumberSetImpl(clazz)
                Float::class -> NumberSetImpl(clazz)
                Long::class -> NumberSetImpl(clazz)
                Int::class -> NumberSetImpl(clazz)
                Short::class -> NumberSetImpl(clazz)
                Byte::class -> NumberSetImpl(clazz)
                else -> throw IllegalArgumentException("Unknown number class")
            }
        }

        fun empty(clazz: KClass<*>): NumberSet {
            return newFromClass(clazz)
        }

        fun zero(clazz: KClass<*>): NumberSet {
            val set = newFromClass(clazz)
            set.addRange(clazz.getZero(), clazz.getZero())
            return set
        }

        fun fullRange(clazz: KClass<*>): NumberSet {
            val set = newFromClass(clazz)
            set.addRange(clazz.getMinValue(), clazz.getMaxValue())
            return set
        }

        fun fullPositiveRange(clazz: KClass<*>): NumberSet {
            val set = newFromClass(clazz)
            set.addRange(clazz.getZero(), clazz.getMaxValue())
            return set
        }

        fun fullNegativeRange(clazz: KClass<*>): NumberSet {
            val set = newFromClass(clazz)
            set.addRange(clazz.getMinValue(), clazz.getZero())
            return set
        }

        inline fun <reified T : Number> singleValue(value: T): NumberSet {
            val set = newFromClassTyped<T>(T::class)
            set.addRangeTyped(value, value)
            return set
        }
    }

    class NumberSetImpl<T : Number>(private val clazz: KClass<*>) : NumberSet {
        /**
         * These ranges are always sorted and never overlap.
         */
        private val ranges = mutableListOf<NumberRange>()

        /**
         * Adds a range to the set. No checks are performed.
         */
        fun addRangeTyped(start: T, end: T) {
            ranges.add(NumberRange(start, end))
        }

        override fun contains(element: Any): Boolean {
            if (element::class != clazz) {
                return false
            }

            @Suppress("UNCHECKED_CAST")
            return contains(element as T)
        }

        override fun <T : Number> castToType(clazz: KClass<*>): NumberSetImpl<T> {
            if (clazz != this.clazz) {
                throw IllegalArgumentException("Cannot cast to different type — $clazz != ${this.clazz}")
            }
            @Suppress("UNCHECKED_CAST")
            return this as NumberSetImpl<T>
        }

        override fun addRange(start: Number, end: Number) {
            if (start::class != clazz || end::class != clazz) {
                throw IllegalArgumentException("Cannot add range of different types")
            }

            @Suppress("UNCHECKED_CAST")
            addRangeTyped(start as T, end as T)
        }

        override fun getRange(): Pair<Number, Number>? {
            return getRangeTyped()
        }

        private fun getRangeTyped(): Pair<T, T>? {
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

        override fun getClass(): KClass<*> {
            return clazz
        }

        private fun <Q : IMoldableSet<Q>> castToThisType(other: Q): NumberSetImpl<T> {
            if (other.getClass() != clazz) {
                throw IllegalArgumentException("Cannot perform operation on different types ($clazz != ${other.getClass()})")
            }
            @Suppress("UNCHECKED_CAST")
            return other as NumberSetImpl<T>
        }

        override fun getSetSatisfying(comp: ComparisonOp): NumberSet {
            val (smallestValue, biggestValue) = getRangeTyped() ?: return this

            val newSet = newFromClassTyped<T>(clazz)

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

        override fun union(other: NumberSet): NumberSet {
            val newSet = newFromClassTyped<T>(clazz)
            newSet.ranges.addAll(ranges)
            newSet.ranges.addAll(castToThisType(other).ranges)
            newSet.mergeAndDeduplicate()
            return newSet
        }

        override fun intersect(other: NumberSet): NumberSet {
            val otherSet = castToThisType(other)
            val newSet = newFromClassTyped<T>(clazz)

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

        override fun invert(): NumberSet {
            val newSet = newFromClassTyped<T>(clazz)

            if (ranges.isEmpty()) {
                newSet.addRange(clazz.getMinValue(), clazz.getMaxValue())
                return newSet
            }

            var currentMin = clazz.getMinValue().castInto<T>(clazz)
            for (range in ranges) {
                if (currentMin < range.start) {
                    newSet.addRangeTyped(currentMin, range.start.downOneEpsilon())
                }
                currentMin = range.end.upOneEpsilon()
            }

            // Add final range if necessary
            val maxValue = clazz.getMaxValue().castInto<T>(clazz)
            if (currentMin < maxValue) {
                newSet.addRangeTyped(currentMin, maxValue)
            }

            return newSet
        }

        override fun negate(): NumberSet {
            val newSet = newFromClassTyped<T>(clazz)
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

        override fun arithmeticOperation(other: NumberSet, operation: BinaryNumberOp): NumberSet {
            val newSet = newFromClassTyped<T>(clazz)
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

        override fun comparisonOperation(other: NumberSet, operation: ComparisonOp): BooleanSet {
            val castOther = castToThisType(other)

            val mySmallestPossibleValue = ranges[0].start
            val myLargestPossibleValue = ranges[ranges.size - 1].end
            val otherSmallestPossibleValue = castOther.ranges[0].start
            val otherLargestPossibleValue = castOther.ranges[castOther.ranges.size - 1].end

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
            changeTerms: ConstraintSolver.EvaluatedCollectedTerms,
            valid: NumberSet
        ): NumberSet {
            val gaveUp = fullPositiveRange(clazz)

            val invalid = (valid.invert() as NumberSet).castToType<T>(clazz)
            val linearChange = (changeTerms.terms[1] ?: return gaveUp).castToType<T>(clazz)
            val constantChange = (changeTerms.terms[0] ?: return gaveUp).castToType<T>(clazz)
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

            val newSet = newFromClassTyped<T>(clazz)
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
            return ranges.size == 1 && ranges[0].start == ranges[0].end && ranges[0].start == clazz.getOne()
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
