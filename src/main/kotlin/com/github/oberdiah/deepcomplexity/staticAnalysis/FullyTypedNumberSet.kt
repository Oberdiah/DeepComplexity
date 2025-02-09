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
import io.kinference.core.operators.math.Reciprocal
import kotlin.reflect.KClass

sealed class FullyTypedNumberSet<T : Number, Self : FullyTypedNumberSet<T, Self>>(
    private val setIndicator: NumberSetIndicator<T, Self>,
    private val data: NumberData<T>
) : NumberSet<Self> {
    private val clazz: KClass<*> = setIndicator.clazz

    override fun <T : NumberSet<T>> castToType(clazz: KClass<*>): T {
        if (clazz != this.clazz) {
            throw IllegalArgumentException("Cannot cast to different type — $clazz != ${this.clazz}")
        }
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    class DoubleSet(data: NumberData<Double> = Empty()) :
        FullyTypedNumberSet<Double, DoubleSet>(DoubleSetIndicator, data)

    class FloatSet(data: NumberData<Float> = Empty()) : FullyTypedNumberSet<Float, FloatSet>(FloatSetIndicator, data)
    class IntSet(data: NumberData<Int> = Empty()) : FullyTypedNumberSet<Int, IntSet>(IntSetIndicator, data)
    class LongSet(data: NumberData<Long> = Empty()) : FullyTypedNumberSet<Long, LongSet>(LongSetIndicator, data)
    class ShortSet(data: NumberData<Short> = Empty()) : FullyTypedNumberSet<Short, ShortSet>(ShortSetIndicator, data)
    class ByteSet(data: NumberData<Byte> = Empty()) : FullyTypedNumberSet<Byte, ByteSet>(ByteSetIndicator, data)

    interface NumberData<T : Number>
    class Empty<T : Number>() : NumberData<T> {
        override fun toString(): String = "∅"
    }

    data class Constant<T : Number>(val value: T) : NumberData<T> {
        override fun toString(): String = value.toString()
    }

    data class Range<T : Number>(val start: T, val end: T, val key: Context.Key) : NumberData<T> {
        override fun toString(): String = "[$start, $end] @ $key"
    }

    data class Union<T : Number>(val setA: NumberData<T>, val setB: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "($setA ∪ $setB)"
    }

    data class Intersection<T : Number>(val setA: NumberData<T>, val setB: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "($setA ∩ $setB)"
    }

    data class Inversion<T : Number>(val set: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "¬$set"
    }

    data class Addition<T : Number>(val setA: NumberData<T>, val setB: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "($setA + $setB)"
    }

    data class Negation<T : Number>(val set: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "-$set"
    }

    data class Multiplication<T : Number>(val setA: NumberData<T>, val setB: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "($setA * $setB)"
    }

    data class Division<T : Number>(val setA: NumberData<T>, val setB: NumberData<T>) : NumberData<T> {
        override fun toString(): String = "($setA / $setB)"
    }

    override fun getAsRanges(): List<Pair<Number, Number>> {
        println(data)

        TODO("Not yet implemented")
    }

    fun withRange(start: T, end: T, key: Context.Key): Self {
        return fromData(Union(data, Range(start, end, key)))
    }

    fun withConstant(value: T): Self {
        return fromData(Union(data, Constant(value)))
    }

    override fun negate(): Self {
        return fromData(Negation(data))
    }

    override fun union(other: Self): Self {
        return fromData(Union(data, other.data))
    }

    override fun intersect(other: Self): Self {
        return fromData(Intersection(data, other.data))
    }

    override fun invert(): Self {
        return fromData(Inversion(data))
    }

    override fun arithmeticOperation(
        other: Self,
        operation: BinaryNumberOp
    ): Self {
        return fromData(
            when (operation) {
                ADDITION -> Addition(data, other.data)
                SUBTRACTION -> Addition(data, Negation(other.data))
                MULTIPLICATION -> Multiplication(data, other.data)
                DIVISION -> Division(data, other.data)
            }
        )
    }

    override fun comparisonOperation(
        other: Self,
        operation: ComparisonOp
    ): BooleanSet {
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

        var newData: NumberData<T> = when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL ->
                Range(
                    setIndicator.getMinValue(),
                    smallestValue.downOneEpsilon(),
                    Context.Key.EphemeralKey.new()
                )

            GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                Range(
                    biggestValue.upOneEpsilon(),
                    setIndicator.getMaxValue(),
                    Context.Key.EphemeralKey.new()
                )
        }

        if (comp == LESS_THAN_OR_EQUAL) {
            newData = Union(newData, data)
        } else if (comp == GREATER_THAN_OR_EQUAL) {
            newData = Union(data, newData)
        }

        return fromData(newData)
    }

    override fun getSetIndicator(): SetIndicator<Self> {
        return setIndicator
    }

    @Suppress("UNCHECKED_CAST")
    private fun fromData(data: NumberData<T>): Self {
        return when (this) {
            is ByteSet -> ByteSet(data as NumberData<Byte>)
            is ShortSet -> ShortSet(data as NumberData<Short>)
            is IntSet -> IntSet(data as NumberData<Int>)
            is LongSet -> LongSet(data as NumberData<Long>)
            is FloatSet -> FloatSet(data as NumberData<Float>)
            is DoubleSet -> DoubleSet(data as NumberData<Double>)
        } as Self
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
}