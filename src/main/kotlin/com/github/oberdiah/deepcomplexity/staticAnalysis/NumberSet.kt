package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression.BinaryNumberOperation
import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression.BinaryNumberOperation.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonExpression.ComparisonOperation
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonExpression.ComparisonOperation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.div
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMaxValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getMinValue
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.minus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.plus
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.times
import com.github.weisj.jsvg.T
import kotlin.reflect.KClass

sealed interface NumberSet : IMoldableSet {
    fun arithmeticOperation(otherN: NumberSet, operation: BinaryNumberOperation): NumberSet
    fun comparisonOperation(otherN: NumberSet, operation: ComparisonOperation): BooleanSet
    fun addRange(start: Number, end: Number)

    fun toImpl(): NumberSetImpl<*> {
        return this as NumberSetImpl<*>
    }

    companion object {
        fun fromClass(clazz: KClass<*>): NumberSet {
            return when (clazz) {
                Double::class -> FloatingPointSet<Double>(clazz)
                Float::class -> FloatingPointSet<Float>(clazz)
                Long::class -> IntegerSet<Long>(clazz)
                Int::class -> IntegerSet<Int>(clazz)
                Short::class -> IntegerSet<Short>(clazz)
                Byte::class -> IntegerSet<Byte>(clazz)
                else -> throw IllegalArgumentException("Unknown number class")
            }
        }

        fun <T : Number> fromClassTyped(clazz: KClass<*>): NumberSetImpl<T> {
            return when (clazz) {
                Double::class -> FloatingPointSet(clazz)
                Float::class -> FloatingPointSet(clazz)
                Long::class -> IntegerSet(clazz)
                Int::class -> IntegerSet(clazz)
                Short::class -> IntegerSet(clazz)
                Byte::class -> IntegerSet(clazz)
                else -> throw IllegalArgumentException("Unknown number class")
            }
        }

        fun fullRange(clazz: KClass<*>): NumberSet {
            val set = fromClass(clazz)
            set.addRange(clazz.getMinValue(), clazz.getMaxValue())
            return set
        }

        inline fun <reified T : Number> singleValue(value: T): NumberSet {
            val clazz = T::class
            val set = fromClassTyped<T>(clazz)
            set.addRangeUnsafe(value, value)
            return set
        }
    }

    abstract class NumberSetImpl<T : Number>(private val clazz: KClass<*>) : NumberSet {
        /**
         * These ranges are always sorted and never overlap.
         */
        private val ranges = mutableListOf<NumberRange<T>>()

        /**
         * Adds a range to the set. No checks are performed.
         */
        fun addRangeUnsafe(start: T, end: T) {
            ranges.add(NumberRange(start, end))
        }

        override fun addRange(start: Number, end: Number) {
            if (start::class != clazz || end::class != clazz) {
                throw IllegalArgumentException("Cannot add range of different types")
            }

            addRangeUnsafe(start as T, end as T)
        }

        override fun toString(): String {
            if (ranges.isEmpty()) {
                return "∅"
            }

            if (ranges.size == 1 && ranges[0].start == ranges[0].end) {
                return ranges[0].start.toString()
            }

            return ranges.joinToString(", ") {
                "[${it.start}, ${it.end}]"
            }
        }

        override fun getClass(): KClass<*> {
            return clazz
        }

        override fun union(other: IMoldableSet): IMoldableSet {
            TODO("Not yet implemented")
        }

        override fun intersect(other: IMoldableSet): IMoldableSet {
            TODO("Not yet implemented")
        }

        override fun invert(): IMoldableSet {
            TODO("Not yet implemented")
        }

        override fun arithmeticOperation(otherN: NumberSet, operation: BinaryNumberOperation): NumberSet {
            if (otherN.getClass() != clazz) {
                throw IllegalArgumentException("Cannot perform arithmetic operations on different types ($clazz vs ${otherN.getClass()})")
            }
            val other = otherN as NumberSetImpl<T>

            val newSet = fromClassTyped<T>(clazz)
            for (range in ranges) {
                for (otherRange in other.ranges) {
                    val values: Iterable<NumberRange<T>> = when (operation) {
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

        override fun comparisonOperation(otherN: NumberSet, operation: ComparisonOperation): BooleanSet {
            if (otherN.getClass() != clazz) {
                throw IllegalArgumentException("Cannot perform arithmetic operations on different types")
            }
            val other = otherN as NumberSetImpl<*>

            val mySmallestPossibleValue = ranges[0].start
            val myLargestPossibleValue = ranges[ranges.size - 1].end
            val otherSmallestPossibleValue = other.ranges[0].start
            val otherLargestPossibleValue = other.ranges[other.ranges.size - 1].end

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
            val newRanges = mutableListOf<NumberRange<T>>()
            var currentRange = ranges[0]
            for (i in 1 until ranges.size) {
                val nextRange = ranges[i]
                if (currentRange.end >= nextRange.start) {
                    currentRange = NumberRange(currentRange.start, nextRange.end)
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
        private inner class NumberRange<T : Number>(val start: T, val end: T) {
            fun contains(other: T): Boolean {
                return start <= other && other <= end
            }

            fun addition(other: NumberRange<T>): Iterable<NumberRange<T>> {
                return resolvePotentialOverflow(
                    start + other.start,
                    end + other.end,
                )
            }

            fun subtraction(other: NumberRange<T>): Iterable<NumberRange<T>> {
                return resolvePotentialOverflow(
                    start - other.start,
                    end - other.end,
                )
            }

            fun multiplication(other: NumberRange<T>): Iterable<NumberRange<T>> {
                val a = start * other.start
                val b = start * other.end
                val c = end * other.start
                val d = end * other.end
                return resolvePotentialOverflow(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                )
            }

            fun division(other: NumberRange<T>): Iterable<NumberRange<T>> {
                val a = start / other.start
                val b = start / other.end
                val c = end / other.start
                val d = end / other.end
                return resolvePotentialOverflow(
                    a.min(b).min(c).min(d),
                    a.max(b).max(c).max(d),
                )
            }

            private fun resolvePotentialOverflow(min: T, max: T): Iterable<NumberRange<T>> {
                // Not bothering with all this for now...
                return listOf(NumberRange(min, max))

//                val minValue = clazz.getMinValue()
//                val maxValue = clazz.getMaxValue()
//
//                if (clazz == Double::class || clazz == Float::class) {
//                    val biggest = if (max > maxValue) DD_POSITIVE_INFINITY else max
//                    val smallest = if (min < minValue) DD_NEGATIVE_INFINITY else min
//                    return listOf(NumberRange(smallest, biggest))
//                }
//
//                when (Settings.overflowBehaviour) {
//                    CLAMP -> {
//                        return listOf(NumberRange(min.max(minValue), max.min(maxValue)))
//                    }
//
//                    ALLOW -> {
//                        // Check if we're overflowing and if we are, we must split the range.
//                        // If we're overflowing in both directions we can just return the full range.
//
//                        if (min < minValue && max > maxValue) {
//                            return listOf(NumberRange(minValue, maxValue))
//                        } else if (min < minValue) {
//                            val wrappedMin = min.add(clazz.getSetSize())
//                            if (wrappedMin < minValue) {
//                                // We're overflowing so much in a single direction
//                                // that the overflow will cover the full range anyway.
//                                return listOf(NumberRange(minValue, maxValue))
//                            }
//                            return listOf(
//                                NumberRange(wrappedMin, maxValue),
//                                NumberRange(minValue, max)
//                            )
//                        } else if (max > maxValue) {
//                            val wrappedMax = max.subtract(clazz.getSetSize())
//                            if (wrappedMax > maxValue) {
//                                // We're overflowing so much in a single direction
//                                // that the overflow will cover the full range anyway.
//                                return listOf(NumberRange(minValue, maxValue))
//                            }
//                            return listOf(
//                                NumberRange(minValue, wrappedMax),
//                                NumberRange(min, maxValue)
//                            )
//                        } else {
//                            return listOf(NumberRange(min, max))
//                        }
//                    }
//                }
            }
        }
    }

    class FloatingPointSet<T : Number>(clazz: KClass<*>) : NumberSetImpl<T>(clazz) {

    }

    class IntegerSet<T : Number>(clazz: KClass<*>) : NumberSetImpl<T>(clazz) {

    }
}