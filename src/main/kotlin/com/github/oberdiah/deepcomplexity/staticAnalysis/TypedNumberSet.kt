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
import com.github.oberdiah.deepcomplexity.evaluation.Constraints
import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.AbstractSet.ConstrainedSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.downOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.min
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.upOneEpsilon
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Affine
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberUtilities
import java.util.stream.Stream

typealias Elements<T> = List<ConstrainedSet<Affine<T>>>
typealias MutableElements<T> = MutableList<ConstrainedSet<Affine<T>>>

sealed class TypedNumberSet<T : Number, Self : TypedNumberSet<T, Self>>(
    private val ind: NumberSetIndicator<T, Self>,
    elements: Elements<T>
) : AbstractSet<Self, Affine<T>>(ind, elements), NumberSet<Self> {

    class DoubleSet(elements: Elements<Double> = emptyList()) :
        TypedNumberSet<Double, DoubleSet>(DoubleSetIndicator, elements)

    class FloatSet(elements: Elements<Float> = emptyList()) :
        TypedNumberSet<Float, FloatSet>(FloatSetIndicator, elements)

    class IntSet(elements: Elements<Int> = emptyList()) :
        TypedNumberSet<Int, IntSet>(IntSetIndicator, elements)

    class LongSet(elements: Elements<Long> = emptyList()) :
        TypedNumberSet<Long, LongSet>(LongSetIndicator, elements)

    class ShortSet(elements: Elements<Short> = emptyList()) :
        TypedNumberSet<Short, ShortSet>(ShortSetIndicator, elements)

    class ByteSet(elements: Elements<Byte> = emptyList()) :
        TypedNumberSet<Byte, ByteSet>(ByteSetIndicator, elements)

    override fun toDebugString(): String = pairsStream().toList().joinToString(", ") { (start, end) ->
        if (start == end) {
            start.toString()
        } else {
            "$start..$end"
        }
    }

    override fun toString(): String = getAsRanges().joinToString(", ") { (start, end) ->
        if (start == end) {
            start.toString()
        } else {
            "$start..$end"
        }
    }

    override fun getRange(): Pair<Number, Number> = getRangeTyped()
    override fun getAsRanges(): List<Pair<Number, Number>> = lazyRanges

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
        elements.stream().flatMap { it.set.toRanges().stream() }

    private fun makeNew(elements: Elements<T>): Self {
        return newFromDataAndInd(elements, ind)
    }

    override fun negate(): Self {
        val zero = Affine.fromConstant(ind.getZero(), ind)
        return makeNew(elements.map { elem -> elem.map { zero.subtract(it)!! } })
    }

    override fun <Q : ConstrainedSet<Q>> cast(outsideInd: SetIndicator<Q>): Q? {
        if (outsideInd !is NumberSetIndicator<*, *>) {
            return null
        }

        fun <OutT : Number, OutSelf : TypedNumberSet<OutT, OutSelf>> extra(outsideInd: NumberSetIndicator<OutT, OutSelf>): OutSelf {
            assert(outsideInd.isWholeNum()) // We can't handle/haven't thought about floating point yet.

            if (outsideInd.getMaxValue() > ind.getMaxValue()) {
                // We're enlarging the possibility space. This means our optimization of keeping unlimited-sized affines
                // around until the final range-pairs step is no longer valid.
                // For example, (short) ((byte) x) + 5 has a range of [-123, 132]
                val newAffines: MutableElements<OutT> = mutableListOf()
                for (element in elements) {
                    val newAffine = element.set.castTo(outsideInd)
                    val affineRanges = newAffine.toRanges()

                    if (affineRanges.size == 1) {
                        // We don't wrap under the new casting, we can keep the affine alive :)
                        newAffines.add(ConstrainedSet(element.constraints, newAffine))
                    } else {
                        // We need to split the affine into multiple affines.
                        for (range in affineRanges) {
                            newAffines.add(
                                ConstrainedSet(
                                    element.constraints,
                                    Affine.fromRangeNoKey(range.first, range.second, outsideInd)
                                )
                            )
                        }
                    }
                }

                return newFromDataAndInd(newAffines, outsideInd)
            } else {
                // When we're shrinking things, I think (?) we're fine to stay as-is and just label with the new indicator.
                // I could be wrong here, but this feels intuitively correct to me.
                return newFromDataAndInd(
                    elements.map { ConstrainedSet(it.constraints, it.set.castTo(outsideInd)) },
                    outsideInd
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        return extra(outsideInd) as Q
    }

    override fun arithmeticOperation(
        other: Self,
        operation: BinaryNumberOp
    ): Self {
        assert(ind == other.ind)

        val affineOp = when (operation) {
            ADDITION -> Affine<T>::add
            SUBTRACTION -> Affine<T>::subtract
            MULTIPLICATION -> Affine<T>::multiply
            DIVISION -> Affine<T>::divide
        }

        val newList: MutableElements<T> = mutableListOf()
        for (range in elements) {
            for (otherRange in other.elements) {
                affineOp(range.set, otherRange.set)?.let {
                    newList.add(
                        ConstrainedSet(
                            range.constraints.and(otherRange.constraints),
                            it
                        )
                    )
                }
            }
        }

        return makeNew(newList)
    }

    override fun comparisonOperation(
        other: Self,
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

        var newData: List<ConstrainedSet<Affine<T>>> = when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL ->
                listOf(
                    ConstrainedSet.unconstrained(
                        Affine.fromRangeNoKey(
                            ind.getMinValue(),
                            smallestValue.downOneEpsilon(),
                            ind
                        )
                    )
                )

            GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                listOf(
                    ConstrainedSet.unconstrained(
                        Affine.fromRangeNoKey(
                            biggestValue.upOneEpsilon(),
                            ind.getMaxValue(),
                            ind
                        )
                    )
                )
        }

        if (comp == LESS_THAN_OR_EQUAL) {
            newData = newData + elements
        } else if (comp == GREATER_THAN_OR_EQUAL) {
            newData = elements + newData
        }

        return makeNew(newData)
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
        assert(ind == other.ind)

        // No affine killing here :)
        return makeNew(elements + other.elements)
    }

    override fun intersect(other: Self): Self {
        assert(ind == other.ind)

        val newList: MutableElements<T> = mutableListOf()

        // For each range in this set, find overlapping ranges in other set
        // This could be made much more efficient.
        for (affine in elements) {
            for (otherAffine in other.elements) {
                val otherRanges = otherAffine.set.toRanges()
                val ranges = affine.set.toRanges()
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
                                ConstrainedSet(
                                    affine.constraints.and(otherAffine.constraints),
                                    if (avoidedWrapping && firstRangeEntirelyEnclosed) {
                                        affine.set
                                    } else if (avoidedWrapping && secondRangeEntirelyEnclosed) {
                                        otherAffine.set
                                    } else {
                                        Affine.fromRangeNoKey(start, end, ind)
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        return makeNew(newList)
    }

    override fun invert(): Self {
        val newList: MutableElements<T> = mutableListOf()

        // Affine death is inevitable here.
        val minValue = ind.getMinValue()
        val maxValue = ind.getMaxValue()

        if (elements.isEmpty()) {
            return makeNew(
                listOf(
                    ConstrainedSet.unconstrained(
                        Affine.fromRangeNoKey(minValue, maxValue, ind)
                    )
                )
            )
        }

        var currentMin = minValue

        val orderedPairs = toRangePairs().sortedWith { a, b -> a.first.compareTo(b.first) }
        for (range in orderedPairs) {
            if (currentMin < range.first) {
                newList.add(
                    ConstrainedSet.unconstrained(
                        Affine.fromRangeNoKey(currentMin, range.first.downOneEpsilon(), ind)
                    )
                )
            }
            currentMin = range.second.upOneEpsilon()
        }

        // Add final range if necessary
        if (currentMin < maxValue) {
            newList.add(
                ConstrainedSet.unconstrained(
                    Affine.fromRangeNoKey(currentMin, maxValue, ind)
                )
            )
        }

        return makeNew(newList)
    }

    companion object {
        fun <T : Number, Self : TypedNumberSet<T, Self>> newFromConstant(
            ind: NumberSetIndicator<T, Self>,
            value: T
        ): Self {
            return newFromDataAndInd(listOf(ConstrainedSet.unconstrained(Affine.fromConstant(value, ind))), ind)
        }

        fun <T : Number, Self : TypedNumberSet<T, Self>> newFromConstraints(
            ind: NumberSetIndicator<T, Self>,
            key: Context.Key,
            constraints: List<Constraints>
        ): Self {
            return newFromDataAndInd(
                constraints.map {
                    val constraint =
                        it.getConstraint(ind, key)?.getRangeTyped() ?: (ind.getMinValue() to ind.getMaxValue())

                    ConstrainedSet(
                        it,
                        Affine.fromConstraints(
                            constraint.first,
                            constraint.second,
                            key,
                            ind
                        )
                    )
                }, ind
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Number, Self : TypedNumberSet<T, Self>> newFromDataAndInd(
            elements: Elements<T>,
            ind: NumberSetIndicator<T, Self>
        ): Self {
            return when (ind) {
                is ByteSetIndicator -> ByteSet(elements as Elements<Byte>)
                is ShortSetIndicator -> ShortSet(elements as Elements<Short>)
                is IntSetIndicator -> IntSet(elements as Elements<Int>)
                is LongSetIndicator -> LongSet(elements as Elements<Long>)
                is FloatSetIndicator -> FloatSet(elements as Elements<Float>)
                is DoubleSetIndicator -> DoubleSet(elements as Elements<Double>)
            } as Self
        }
    }
}