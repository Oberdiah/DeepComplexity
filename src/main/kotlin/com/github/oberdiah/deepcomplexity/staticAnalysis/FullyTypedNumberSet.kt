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
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Affine
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import java.util.stream.Stream
import kotlin.reflect.KClass

sealed class FullyTypedNumberSet<T : Number, Self : FullyTypedNumberSet<T, Self>>(
    private val setIndicator: NumberSetIndicator<T, Self>,
    private val affines: List<Affine<T>>
) : NumberSet<Self> {
    private val clazz: KClass<*> = setIndicator.clazz

    class DoubleSet(ranges: List<Affine<Double>> = emptyList()) :
        FullyTypedNumberSet<Double, DoubleSet>(DoubleSetIndicator, ranges)

    class FloatSet(ranges: List<Affine<Float>> = emptyList()) :
        FullyTypedNumberSet<Float, FloatSet>(FloatSetIndicator, ranges)

    class IntSet(ranges: List<Affine<Int>> = emptyList()) :
        FullyTypedNumberSet<Int, IntSet>(IntSetIndicator, ranges)

    class LongSet(ranges: List<Affine<Long>> = emptyList()) :
        FullyTypedNumberSet<Long, LongSet>(LongSetIndicator, ranges)

    class ShortSet(ranges: List<Affine<Short>> = emptyList()) :
        FullyTypedNumberSet<Short, ShortSet>(ShortSetIndicator, ranges)

    class ByteSet(ranges: List<Affine<Byte>> = emptyList()) :
        FullyTypedNumberSet<Byte, ByteSet>(ByteSetIndicator, ranges)

    val lazyRanges: List<Pair<T, T>> by lazy {
        toRangePairs()
    }

    override fun toString(): String = getAsRanges().joinToString(", ") { (start, end) ->
        if (start == end) {
            start.toString()
        } else {
            affines.joinToString(", ") { it.stringOverview() }
        }
    }

    override fun hashCode(): Int = affines.hashCode()

    override fun getRange(): Pair<Number, Number> = getRangeTyped()
    override fun getAsRanges(): List<Pair<Number, Number>> = lazyRanges

    fun getAsRangesTyped(): List<Pair<T, T>> = lazyRanges
    fun getRangeTyped(): Pair<T, T> {
        val ranges = getAsRangesTyped()
        return Pair(ranges.first().first, ranges.last().second)
    }

    fun toRangePairs(): List<Pair<T, T>> = NumberUtilities.mergeAndDeduplicate(pairsStream().toList())
    private fun pairsStream(): Stream<Pair<T, T>> = affines.stream().flatMap { it.toRanges().stream() }

    private fun makeNew(ranges: List<Affine<T>>): Self {
        return newFromDataAndInd(ranges, setIndicator)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FullyTypedNumberSet<*, *>) return false
        if (affines != other.affines) return false

        return true
    }

    override fun negate(): Self {
        val zero = Affine.fromConstant(setIndicator.getZero(), setIndicator)
        return makeNew(affines.map { zero.subtract(it)!! })
    }

    override fun <Q : IMoldableSet<Q>> cast(outsideInd: SetIndicator<Q>): Q {
        if (outsideInd !is NumberSetIndicator<*, *>) {
            throw IllegalArgumentException("Cannot cast a number to '$outsideInd'.")
        }

        fun <OutT : Number, OutSelf : FullyTypedNumberSet<OutT, OutSelf>> extra(outsideInd: NumberSetIndicator<OutT, OutSelf>): OutSelf {
            assert(outsideInd.isWholeNum()) // We can't handle/haven't thought about floating point yet.

            if (outsideInd.getMaxValue() > setIndicator.getMaxValue()) {
                // We're enlarging the possibility space. This means our optimization of keeping unlimited-sized affines
                // around until the final range-pairs step is no longer valid.
                // For example, (short) ((byte) x) + 5 has a range of [-123, 132]
                val newAffines = mutableListOf<Affine<OutT>>()
                for (affine in affines) {
                    val newAffine = affine.castTo(outsideInd)
                    val affineRanges = newAffine.toRanges()

                    if (affineRanges.size == 1) {
                        // We don't wrap under the new casting, we can keep the affine alive :)
                        newAffines.add(newAffine)
                    } else {
                        // We need to split the affine into multiple affines.
                        for (range in affineRanges) {
                            newAffines.add(Affine.fromRangeNoKey(range.first, range.second, outsideInd))
                        }
                    }
                }

                return newFromDataAndInd(newAffines, outsideInd)
            } else {
                // When we're shrinking things, I think (?) we're fine to stay as-is and just label with the new indicator.
                // I could be wrong here, but this feels intuitively correct to me.
                return newFromDataAndInd(affines.map { it.castTo(outsideInd) }, outsideInd)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return extra(outsideInd) as Q
    }

    override fun arithmeticOperation(
        other: Self,
        operation: BinaryNumberOp
    ): Self {
        assert(setIndicator == other.setIndicator)

        val affineOp = when (operation) {
            ADDITION -> Affine<T>::add
            SUBTRACTION -> Affine<T>::subtract
            MULTIPLICATION -> Affine<T>::multiply
            DIVISION -> Affine<T>::divide
        }

        val newList: MutableList<Affine<T>> = mutableListOf()
        for (range in affines) {
            for (otherRange in other.affines) {
                affineOp(range, otherRange)?.let {
                    newList.add(it)
                }
            }
        }

        return makeNew(newList)
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

        var newData: List<Affine<T>> = when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL ->
                listOf(
                    Affine.fromRangeNoKey(
                        setIndicator.getMinValue(),
                        smallestValue.downOneEpsilon(),
                        setIndicator
                    )
                )

            GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                listOf(
                    Affine.fromRangeNoKey(
                        biggestValue.upOneEpsilon(),
                        setIndicator.getMaxValue(),
                        setIndicator
                    )
                )
        }

        if (comp == LESS_THAN_OR_EQUAL) {
            newData = newData + affines
        } else if (comp == GREATER_THAN_OR_EQUAL) {
            newData = affines + newData
        }

        return makeNew(newData)
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

    override fun union(other: Self): Self {
        assert(setIndicator == other.setIndicator)

        // No affine killing here :)
        return makeNew(affines + other.affines)
    }

    override fun intersect(other: Self): Self {
        assert(setIndicator == other.setIndicator)

        val newList: MutableList<Affine<T>> = mutableListOf()

        // For each range in this set, find overlapping ranges in other set
        // This could be made much more efficient.
        for (affine in affines) {
            for (otherAffine in other.affines) {
                val otherRanges = otherAffine.toRanges()
                val ranges = affine.toRanges()
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

    override fun invert(): Self {
        val newList: MutableList<Affine<T>> = mutableListOf()
        // Affine death is inevitable here.

        val minValue = setIndicator.getMinValue()
        val maxValue = setIndicator.getMaxValue()

        if (affines.isEmpty()) {
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
        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromConstant(
            ind: NumberSetIndicator<T, Self>,
            value: T
        ): Self {
            return newFromDataAndInd(listOf(Affine.fromConstant(value, ind)), ind)
        }

        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromConstraints(
            ind: NumberSetIndicator<T, Self>,
            pair: Pair<T, T>,
            key: Context.Key
        ): Self {
            return newFromDataAndInd(listOf(Affine.fromConstraints(pair.first, pair.second, key, ind)), ind)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromDataAndInd(
            affines: List<Affine<T>>,
            ind: NumberSetIndicator<T, Self>
        ): Self {
            return when (ind) {
                is ByteSetIndicator -> ByteSet(affines as List<Affine<Byte>>)
                is ShortSetIndicator -> ShortSet(affines as List<Affine<Short>>)
                is IntSetIndicator -> IntSet(affines as List<Affine<Int>>)
                is LongSetIndicator -> LongSet(affines as List<Affine<Long>>)
                is FloatSetIndicator -> FloatSet(affines as List<Affine<Float>>)
                is DoubleSetIndicator -> DoubleSet(affines as List<Affine<Double>>)
            } as Self
        }
    }
}