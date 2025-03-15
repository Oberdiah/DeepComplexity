package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.IntegerAffine
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getSetSize
import java.math.BigInteger
import java.math.BigInteger.valueOf

@ConsistentCopyVisibility
data class Affine<T : Number> private constructor(
    private val setIndicator: NumberSetIndicator<T, *>,
    // Obviously at one point we'll want to support floating point too.
    private val affine: IntegerAffine
) {
    val clazz = setIndicator.clazz

    fun stringOverview() = affine.stringOverview()
    override fun toString(): String = affine.toString()

    fun <NewT : Number> castTo(newInd: NumberSetIndicator<NewT, *>): Affine<NewT> = Affine(newInd, affine)

    fun getKeys(): List<Context.Key> = affine.getKeys()

    fun isExactly(i: Int): Boolean = affine.isExactly(i)
    fun toRanges(): List<Pair<T, T>> {
        assert(setIndicator.isWholeNum()) // We can't handle/haven't thought about floating point yet.

        val max = valueOf(setIndicator.getMaxValue().toLong())
        val min = valueOf(setIndicator.getMinValue().toLong())
        val setSize = valueOf(setIndicator.clazz.getSetSize().toLong())

        val (initialLower, initialUpper) = affine.toRange()
        val distanceToShunt = setSize * if (initialLower < min) {
            (min - initialLower - BigInteger.ONE) / setSize + BigInteger.ONE
        } else {
            (min - initialLower) / setSize
        }

        // The first step is to shift the affine so that the lower value is definitely between min and max.
        // Makes things a lot easier to reason about.
        val (lower, upper) = initialLower + distanceToShunt to initialUpper + distanceToShunt

        assert(lower >= min) {
            "Lower bound $lower is below minimum value $min"
        }
        assert(lower <= max) {
            "Lower bound $lower is above maximum value $max"
        }

        if (upper - lower >= setSize) {
            // In this case we're covering the whole range no matter what.
            return listOf(min.castInto<T>(clazz) to max.castInto<T>(clazz))
        }

        if (upper <= max) {
            // Easy-peasy, we fit, all is good.
            return listOf(lower.castInto<T>(clazz) to upper.castInto<T>(clazz))
        }

        return listOf(
            upper.castInto<T>(clazz) to max.castInto<T>(clazz),
            min.castInto<T>(clazz) to lower.castInto<T>(clazz)
        )
    }

    fun add(other: Affine<T>): Affine<T>? = affine.add(other.affine)?.let { Affine(setIndicator, it) }
    fun subtract(other: Affine<T>): Affine<T>? = affine.subtract(other.affine)?.let { Affine(setIndicator, it) }
    fun multiply(other: Affine<T>): Affine<T>? = affine.multiply(other.affine)?.let { Affine(setIndicator, it) }
    fun divide(other: Affine<T>): Affine<T>? = TODO()

    companion object {
        fun <T : Number> fromConstant(constant: T, ind: NumberSetIndicator<T, *>): Affine<T> =
            Affine(ind, IntegerAffine.fromConstant(constant.toLong()))

        // todo investigate if usages of this can be converted to fromConstraints
        fun <T : Number> fromRangeNoKey(start: T, end: T, ind: NumberSetIndicator<T, *>): Affine<T> =
            Affine(ind, IntegerAffine.fromRangeNoKey(start.toLong(), end.toLong()))

        fun <T : Number> fromConstraints(start: T, end: T, key: Context.Key, ind: NumberSetIndicator<T, *>): Affine<T> =
            Affine(ind, IntegerAffine.fromConstraints(ind, start.toLong(), end.toLong(), key))
    }
}