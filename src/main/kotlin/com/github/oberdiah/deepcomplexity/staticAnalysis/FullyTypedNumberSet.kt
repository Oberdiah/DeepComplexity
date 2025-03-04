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
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.NumberSimplifier
import kotlin.reflect.KClass

sealed class FullyTypedNumberSet<T : Number, Self : FullyTypedNumberSet<T, Self>>(
    private val setIndicator: NumberSetIndicator<T, Self>,
    private val data: NumberData<T>
) : NumberSet<Self> {
    private val clazz: KClass<*> = setIndicator.clazz

    override fun debugString(): String = data.toString()

    override fun toString(): String = getAsRanges().joinToString(", ") { (start, end) ->
        if (start == end) {
            start.toString()
        } else {
            "$start..$end"
        }
    }

    class DoubleSet(data: NumberData<Double> = Empty()) :
        FullyTypedNumberSet<Double, DoubleSet>(DoubleSetIndicator, data)

    class FloatSet(data: NumberData<Float> = Empty()) : FullyTypedNumberSet<Float, FloatSet>(FloatSetIndicator, data)
    class IntSet(data: NumberData<Int> = Empty()) : FullyTypedNumberSet<Int, IntSet>(IntSetIndicator, data)
    class LongSet(data: NumberData<Long> = Empty()) : FullyTypedNumberSet<Long, LongSet>(LongSetIndicator, data)
    class ShortSet(data: NumberData<Short> = Empty()) : FullyTypedNumberSet<Short, ShortSet>(ShortSetIndicator, data)
    class ByteSet(data: NumberData<Byte> = Empty()) : FullyTypedNumberSet<Byte, ByteSet>(ByteSetIndicator, data)

    sealed interface NumberData<T : Number> {
        // The visitor is NumberData<*> because it may be cast half-way through.
        fun traverse(visitor: (NumberData<*>) -> Unit) = visitor(this)
        fun isConfirmedToBe(i: Int): Boolean = false
    }

    // Note that although setA and setB are ostensibly of equal importance,
    // when it comes to resolution setA gets priority and its affine is more likely to live.
    sealed interface BinaryNumberData<T : Number> : NumberData<T> {
        val setA: NumberData<T>
        val setB: NumberData<T>

        override fun traverse(visitor: (NumberData<*>) -> Unit) {
            setA.traverse(visitor)
            setB.traverse(visitor)
        }
    }

    data class Empty<T : Number>(val unused: Int = 0) : NumberData<T> {
        override fun toString(): String = "∅"
    }

    data class Inversion<T : Number>(val set: NumberData<T>) : NumberData<T> {
        override fun traverse(visitor: (NumberData<*>) -> Unit) = set.traverse(visitor)
        override fun toString(): String = "¬$set"
    }

    data class Union<T : Number>(override val setA: NumberData<T>, override val setB: NumberData<T>) :
        BinaryNumberData<T> {
        override fun toString(): String {
            if (setA is Empty) {
                return setB.toString()
            }
            if (setB is Empty) {
                return setA.toString()
            }

            return "(\n${
                setA.toString().prependIndent()
            }\n\t∪\n${
                setB.toString().prependIndent()
            }\n)"
        }
    }

    data class Cast<Outside : Number, Inside : Number>(
        val set: NumberData<Inside>,
        val outsideInd: NumberSetIndicator<Outside, *>,
        val insideInd: NumberSetIndicator<Inside, *>
    ) : NumberData<Outside> {
        override fun traverse(visitor: (NumberData<*>) -> Unit) = super.traverse(visitor)
        override fun toString(): String = "(${outsideInd.clazz}) ($set)"
    }

    data class Intersection<T : Number>(override val setA: NumberData<T>, override val setB: NumberData<T>) :
        BinaryNumberData<T> {
        override fun toString(): String = "(\n${
            setA.toString().prependIndent()
        }\n\t∩\n${
            setB.toString().prependIndent()
        }\n)"
    }

    data class Addition<T : Number>(override val setA: NumberData<T>, override val setB: NumberData<T>) :
        BinaryNumberData<T> {
        override fun toString(): String = "(\n${
            setA.toString().prependIndent()
        }\n\t+\n${
            setB.toString().prependIndent()
        }\n)"
    }

    data class Subtraction<T : Number>(override val setA: NumberData<T>, override val setB: NumberData<T>) :
        BinaryNumberData<T> {
        override fun toString(): String = "(\n${
            setA.toString().prependIndent()
        }\n\t-\n${
            setB.toString().prependIndent()
        }\n)"
    }

    data class Multiplication<T : Number>(override val setA: NumberData<T>, override val setB: NumberData<T>) :
        BinaryNumberData<T> {
        override fun toString(): String = "(\n${
            setA.toString().prependIndent()
        }\n\t*\n${
            setB.toString().prependIndent()
        }\n)"
    }

    data class Division<T : Number>(override val setA: NumberData<T>, override val setB: NumberData<T>) :
        BinaryNumberData<T> {
        override fun toString(): String = "(\n${
            setA.toString().prependIndent()
        }\n\t/\n${
            setB.toString().prependIndent()
        }\n)"
    }

    val lazyRanges: List<Pair<Number, Number>> by lazy {
        NumberSimplifier.distillToSet(setIndicator, data, true)
    }

    override fun getAsRanges(): List<Pair<Number, Number>> = lazyRanges

    fun withRange(start: T, end: T, key: Context.Key): Self {
        if (start == end) {
            return newFromData(Union(data, Ranges.fromConstant(start, setIndicator)))
        }

        return newFromData(Union(data, Ranges.fromRange(start, end, key, setIndicator)))
    }

    fun withConstant(value: T): Self {
        return newFromData(Union(data, Ranges.fromConstant(value, setIndicator)))
    }

    override fun negate(): Self {
        return newFromData(Subtraction(Ranges.fromConstant(setIndicator.getZero(), setIndicator), data))
    }

    override fun union(other: Self): Self {
        assert(setIndicator == other.setIndicator)
        return newFromData(Union(data, other.data))
    }

    override fun intersect(other: Self): Self {
        assert(setIndicator == other.setIndicator)
        return newFromData(Intersection(data, other.data))
    }

    override fun invert(): Self {
        return newFromData(Inversion(data))
    }

    override fun <Q : IMoldableSet<Q>> cast(outsideInd: SetIndicator<Q>): Q {
        if (outsideInd !is NumberSetIndicator<*, *>) {
            throw IllegalArgumentException("Cannot cast a number to '$outsideInd'.")
        }

        fun <OutT : Number, OutSelf : FullyTypedNumberSet<OutT, OutSelf>> extra(outsideInd: NumberSetIndicator<OutT, OutSelf>): OutSelf {
            return newFromDataAndInd(Cast(data, outsideInd, setIndicator), outsideInd)
        }

        @Suppress("UNCHECKED_CAST")
        return extra(outsideInd) as Q
    }


    override fun arithmeticOperation(
        other: Self,
        operation: BinaryNumberOp
    ): Self {
        assert(setIndicator == other.setIndicator)
        return newFromData(
            when (operation) {
                ADDITION -> Addition(data, other.data)
                SUBTRACTION -> Subtraction(data, other.data)
                MULTIPLICATION -> Multiplication(data, other.data)
                DIVISION -> Division(data, other.data)
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

        var newData: NumberData<T> = when (comp) {
            LESS_THAN, LESS_THAN_OR_EQUAL ->
                Ranges.fromRange(
                    setIndicator.getMinValue(),
                    smallestValue.downOneEpsilon(),
                    Context.Key.EphemeralKey.new(),
                    setIndicator
                )

            GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                Ranges.fromRange(
                    biggestValue.upOneEpsilon(),
                    setIndicator.getMaxValue(),
                    Context.Key.EphemeralKey.new(),
                    setIndicator
                )
        }

        if (comp == LESS_THAN_OR_EQUAL) {
            newData = Union(newData, data)
        } else if (comp == GREATER_THAN_OR_EQUAL) {
            newData = Union(data, newData)
        }

        return newFromData(newData)
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

    private fun newFromData(data: NumberData<T>): Self {
        return newFromDataAndInd(data, setIndicator)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T : Number, Self : FullyTypedNumberSet<T, Self>> newFromDataAndInd(
            data: NumberData<T>,
            ind: NumberSetIndicator<T, Self>
        ): Self {
            return when (ind) {
                is ByteSetIndicator -> ByteSet(data as NumberData<Byte>)
                is ShortSetIndicator -> ShortSet(data as NumberData<Short>)
                is IntSetIndicator -> IntSet(data as NumberData<Int>)
                is LongSetIndicator -> LongSet(data as NumberData<Long>)
                is FloatSetIndicator -> FloatSet(data as NumberData<Float>)
                is DoubleSetIndicator -> DoubleSet(data as NumberData<Double>)
            } as Self
        }
    }
}