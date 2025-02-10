package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.IntegerAffine
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.castInto
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.getSetSize
import java.math.BigInteger
import java.math.BigInteger.valueOf

class Affine<T : Number> private constructor(
    private val setIndicator: NumberSetIndicator<T, *>,
    // Obviously at one point we'll want to support floating point too.
    private val affine: IntegerAffine
) {
    val clazz = setIndicator.clazz

    override fun toString(): String = affine.toString()

    fun isExactly(i: Int): Boolean = affine.isExactly(i)
    fun toRanges(): List<Pair<T, T>> {
        val (lower, upper) = affine.toRange()
        // We know for a fact that the lower end is in bounds.
        // The question is whether the upper end is too.

        val max = valueOf(setIndicator.getMaxValue().toLong())
        val min = valueOf(setIndicator.getMinValue().toLong())
        val setSize = valueOf(setIndicator.clazz.getSetSize().toLong())

        if (upper - lower > setSize) {
            // In this case we're covering the whole range no matter what.
            return listOf(lower.castInto<T>(clazz) to upper.castInto<T>(clazz))
        }

        if (upper < max) {
            // Easy-peasy, we fit, all is good.
            return listOf(lower.castInto<T>(clazz) to upper.castInto<T>(clazz))
        }

        return listOf(
            upper.castInto<T>(clazz) to max.castInto<T>(clazz),
            min.castInto<T>(clazz) to lower.castInto<T>(clazz)
        )
    }

    fun add(other: Affine<T>): Affine<T> = Affine(setIndicator, affine.add(other.affine))
    fun subtract(other: Affine<T>): Affine<T> = Affine(setIndicator, affine.subtract(other.affine))
    fun multiply(other: Affine<T>): Affine<T> = Affine(setIndicator, affine.multiply(other.affine))
    fun divide(other: Affine<T>): Affine<T> = TODO()

    companion object {
        fun <T : Number> fromConstant(constant: T, ind: NumberSetIndicator<T, *>): Affine<T> =
            Affine(ind, IntegerAffine.fromConstant(constant.toLong(), ind))

        fun <T : Number> fromRangeNoKey(start: T, end: T, ind: NumberSetIndicator<T, *>): Affine<T> =
            Affine(ind, IntegerAffine.fromRangeNoKey(start.toLong(), end.toLong(), ind))

        fun <T : Number> fromRange(start: T, end: T, key: Context.Key, ind: NumberSetIndicator<T, *>): Affine<T> =
            Affine(ind, IntegerAffine.fromRange(start.toLong(), end.toLong(), key, ind))
    }
}