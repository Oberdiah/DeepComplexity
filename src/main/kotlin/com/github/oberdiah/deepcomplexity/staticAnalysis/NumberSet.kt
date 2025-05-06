package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
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

typealias Affines<T> = List<Affine<T>>

sealed class NumberSet<T : Number>(
    val ind: NumberSetIndicator<T>,
    val affines: List<Affine<T>>
) : Bundle<T> {
    override fun getIndicator(): SetIndicator<T> = ind
    val clazz: KClass<T> = ind.clazz

    class DoubleSet(affines: Affines<Double> = emptyList()) :
        NumberSet<Double>(DoubleSetIndicator, affines)

    class FloatSet(affines: Affines<Float> = emptyList()) :
        NumberSet<Float>(FloatSetIndicator, affines)

    class IntSet(affines: Affines<Int> = emptyList()) :
        NumberSet<Int>(IntSetIndicator, affines)

    class LongSet(affines: Affines<Long> = emptyList()) :
        NumberSet<Long>(LongSetIndicator, affines)

    class ShortSet(affines: Affines<Short> = emptyList()) :
        NumberSet<Short>(ShortSetIndicator, affines)

    class ByteSet(affines: Affines<Byte> = emptyList()) :
        NumberSet<Byte>(ByteSetIndicator, affines)

    override fun toString(): String = getAsRanges().joinToString(", ") { (start, end) ->
        if (start == end) {
            start.toString()
        } else if (start == ind.getMinValue() && end == ind.getMaxValue()) {
            "*"
        } else if (start == ind.getMinValue()) {
            "..$end"
        } else if (end == ind.getMaxValue()) {
            "$start.."
        } else {
            "$start..$end"
        }
    }

    override fun isEmpty(): Boolean = affines.isEmpty()

    fun getRange(): Pair<Number, Number> = getRangeTyped()
    fun getAsRanges(): List<Pair<Number, Number>> = lazyRanges

    fun getAsRangesTyped(): List<Pair<T, T>> = lazyRanges
    fun getRangeTyped(): Pair<T, T> {
        val ranges = getAsRangesTyped()
        return Pair(ranges.first().first, ranges.last().second)
    }

    val lazyRanges: List<Pair<T, T>> by lazy {
        toRangePairs()
    }

    fun toRangePairs(): List<Pair<T, T>> = NumberUtilities.mergeAndDeduplicate(pairsStream().toList())
    private fun pairsStream(): Stream<Pair<T, T>> =
        affines.stream().flatMap { it.toRanges().stream() }

    private fun makeNew(affines: Affines<T>): NumberSet<T> {
        return newFromDataAndInd(ind, affines)
    }

    fun negate(): NumberSet<T> {
        val zero = Affine.fromConstant(ind, ind.getZero())
        return makeNew(affines.map { elem -> zero.subtract(elem) })
    }

    override fun <Q : Any> cast(outsideInd: SetIndicator<Q>): Bundle<Q>? {
        if (outsideInd !is NumberSetIndicator<*>) {
            return null
        }

        fun <OutT : Number> extra(outsideInd: NumberSetIndicator<OutT>): NumberSet<OutT> {
            assert(outsideInd.isWholeNum()) // We can't handle/haven't thought about floating point yet.

            if (outsideInd.getMaxValue() > ind.getMaxValue()) {
                // We're enlarging the possibility space. This means our optimization of keeping unlimited-sized affines
                // around until the final range-pairs step is no longer valid.
                // For example, (short) ((byte) x) + 5 has a range of [-123, 132]
                val newAffines: MutableList<Affine<OutT>> = mutableListOf()
                for (element in affines) {
                    val newAffine = element.castTo(outsideInd)
                    val affineRanges = newAffine.toRanges()

                    if (affineRanges.size == 1) {
                        // We don't wrap under the new casting, we can keep the affine alive :)
                        newAffines.add(newAffine)
                    } else {
                        // We need to split the affine into multiple affines.
                        for (range in affineRanges) {
                            newAffines.add(
                                Affine.fromRange(outsideInd, range.first, range.second)
                            )
                        }
                    }
                }

                return newFromDataAndInd(outsideInd, newAffines)
            } else {
                // When we're shrinking things, I think (?) we're fine to stay as-is and just label with the new indicator.
                // I could be wrong here, but this feels intuitively correct to me.
                return newFromDataAndInd(
                    outsideInd,
                    affines.map { it.castTo(outsideInd) },
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        // Safety: Trivially true by checking the signature of extra().
        return extra(outsideInd) as Bundle<Q>
    }

    fun arithmeticOperation(
        other: NumberSet<T>,
        operation: BinaryNumberOp
    ): NumberSet<T> {
        assert(ind == other.ind)

        val affineOp = when (operation) {
            ADDITION -> Affine<T>::add
            SUBTRACTION -> Affine<T>::subtract
            MULTIPLICATION -> Affine<T>::multiply
            DIVISION -> Affine<T>::divide
        }

        val newList: MutableList<Affine<T>> = mutableListOf()
        for (range in affines) {
            for (otherRange in other.affines) {
                newList.add(affineOp(range, otherRange))
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

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right-hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp, key: Context.Key): NumberSet<T> {
        val range = getRange()
        val smallestValue = range.first.castInto<T>(clazz)
        val biggestValue = range.second.castInto<T>(clazz)

        var newData: List<Affine<T>> = when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL ->
                listOf(
                    Affine.fromRange(
                        ind,
                        key,
                        ind.getMinValue(),
                        smallestValue.downOneEpsilon()
                    )
                )

            GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                listOf(
                    Affine.fromRange(
                        ind,
                        key,
                        biggestValue.upOneEpsilon(),
                        ind.getMaxValue()
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

    override fun contains(element: T): Boolean {
        val value = element
        for (range in getAsRanges()) {
            if (value >= range.first && value <= range.second) {
                return true
            }
        }
        return false
    }

    override fun union(other: Bundle<T>): Bundle<T> {
        assert(other is NumberSet<T>)
        other as NumberSet<T>
        assert(ind == other.ind)

        // No affine killing here :)
        return makeNew(affines + other.affines)
    }

    override fun intersect(other: Bundle<T>): Bundle<T> {
        assert(other is NumberSet<T>)
        other as NumberSet<T>
        assert(ind == other.ind)

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
                            newList.add(
                                if (avoidedWrapping && firstRangeEntirelyEnclosed) {
                                    affine
                                } else if (avoidedWrapping && secondRangeEntirelyEnclosed) {
                                    otherAffine
                                } else {
                                    Affine.fromRange(ind, start, end)
                                }
                            )
                        }
                    }
                }
            }
        }

        return makeNew(newList)
    }

    override fun invert(): NumberSet<T> {
        val newList: MutableList<Affine<T>> = mutableListOf()

        // Affine death is inevitable here.
        val minValue = ind.getMinValue()
        val maxValue = ind.getMaxValue()

        if (affines.isEmpty()) {
            return makeNew(
                listOf(
                    Affine.fromRange(ind, minValue, maxValue)
                )
            )
        }

        var currentMin = minValue

        val orderedPairs = toRangePairs().sortedWith { a, b -> a.first.compareTo(b.first) }
        for (range in orderedPairs) {
            if (currentMin < range.first) {
                newList.add(
                    Affine.fromRange(ind, currentMin, range.first.downOneEpsilon())
                )
            }
            currentMin = range.second.upOneEpsilon()
        }

        // Add final range if necessary
        if (currentMin < maxValue) {
            newList.add(
                Affine.fromRange(ind, currentMin, maxValue)
            )
        }

        return makeNew(newList)
    }

    fun isOne(): Boolean {
        return affines.all { it.isExactly(1) }
    }

    companion object {
        fun <T : Number> zero(ind: NumberSetIndicator<T>): NumberSet<T> = newFromDataAndInd(
            ind,
            listOf(Affine.fromConstant(ind, ind.getZero())),
        )

        fun <T : Number> one(ind: NumberSetIndicator<T>): NumberSet<T> = newFromDataAndInd(
            ind,
            listOf(Affine.fromConstant(ind, ind.getOne())),
        )

        fun <T : Number> newFromConstant(
            constant: T
        ): NumberSet<T> {
            val ind = SetIndicator.fromValue(constant)
            return newFromDataAndInd(
                ind,
                listOf(Affine.fromConstant(ind, constant)),
            )
        }

        fun <T : Number> newFromVariance(
            ind: NumberSetIndicator<T>,
            varianceSource: Context.Key
        ): NumberSet<T> =
            newFromDataAndInd(
                ind,
                listOf(Affine.fromVariance(ind, varianceSource)),
            )

        @Suppress("UNCHECKED_CAST")
        private fun <T : Number> newFromDataAndInd(
            ind: NumberSetIndicator<T>,
            affines: Affines<T>,
        ): NumberSet<T> {
            return when (ind) {
                is ByteSetIndicator -> ByteSet(affines as Affines<Byte>)
                is ShortSetIndicator -> ShortSet(affines as Affines<Short>)
                is IntSetIndicator -> IntSet(affines as Affines<Int>)
                is LongSetIndicator -> LongSet(affines as Affines<Long>)
                is FloatSetIndicator -> FloatSet(affines as Affines<Float>)
                is DoubleSetIndicator -> DoubleSet(affines as Affines<Double>)
            } as NumberSet<T>
        }
    }
}