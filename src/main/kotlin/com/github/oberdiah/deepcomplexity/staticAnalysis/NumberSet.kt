package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.github.oberdiah.deepcomplexity.settings.Settings
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.ALLOW
import com.github.oberdiah.deepcomplexity.settings.Settings.OverflowBehaviour.CLAMP
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMaxValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMinValue
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

sealed interface NumberSet : IMoldableSet {
    fun arithmeticOperation(other: NumberSet, operation: BinaryNumberOp): NumberSet
    fun comparisonOperation(other: NumberSet, operation: ComparisonOp): BooleanSet
    fun addRange(start: Number, end: Number)
    fun negate(): NumberSet

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

        override fun addRange(start: Number, end: Number) {
            if (start::class != clazz || end::class != clazz) {
                throw IllegalArgumentException("Cannot add range of different types")
            }

            @Suppress("UNCHECKED_CAST")
            addRangeTyped(start as T, end as T)
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

        private fun castToThisType(other: IMoldableSet): NumberSetImpl<T> {
            if (other.getClass() != clazz) {
                throw IllegalArgumentException("Cannot perform operation on different types")
            }
            @Suppress("UNCHECKED_CAST")
            return other as NumberSetImpl<T>
        }

        override fun getSetSatisfying(comp: ComparisonOp): NumberSet {
            if (ranges.isEmpty()) {
                return this
            }

            val biggestValue = ranges[ranges.size - 1].end
            val smallestValue = ranges[0].start

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

        override fun union(other: IMoldableSet): IMoldableSet {
            val newSet = newFromClassTyped<T>(clazz)
            newSet.ranges.addAll(ranges)
            newSet.ranges.addAll(castToThisType(other).ranges)
            newSet.mergeAndDeduplicate()
            return newSet
        }

        override fun intersect(other: IMoldableSet): IMoldableSet {
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

        override fun invert(): IMoldableSet {
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
