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
    private val ind: NumberSetIndicator<T>,
    // At one point we'll want to support floating point too.
    private val affine: IntegerAffine
) {
    val clazz = ind.clazz
    val indMax = valueOf(ind.getMaxValue().toLong())
    val indMin = valueOf(ind.getMinValue().toLong())

    fun stringOverview(): String {
        val (min, max) = affine.toRange()

        val firstHalf = if (min == max) {
            min.toString()
        } else if (min == indMin && max == indMax) {
            "*"
        } else if (min == indMin) {
            "..$max"
        } else if (max == indMax) {
            "$min.."
        } else {
            "$min..$max"
        }

        val keys = affine.getKeys()

        val secondHalf = if (keys.all { it is Context.Key.EphemeralKey }) {
            ""
        } else {
            " (${keys.joinToString { it.toString() }})"
        }

        return "$firstHalf$secondHalf"
    }

    override fun toString(): String = affine.toString()

    fun <NewT : Number> castTo(newInd: NumberSetIndicator<NewT>): Affine<NewT> = Affine(newInd, affine)

    fun getKeys(): List<Context.Key> = affine.getKeys()

    fun isExactly(i: Int): Boolean = affine.isExactly(i)
    fun toRanges(): List<Pair<T, T>> {
        assert(ind.isWholeNum()) // We can't handle/haven't thought about floating point yet.

        val setSize = valueOf(ind.clazz.getSetSize().toLong())

        val (initialLower, initialUpper) = affine.toRange()
        val distanceToShunt = setSize * if (initialLower < indMin) {
            (indMin - initialLower - BigInteger.ONE) / setSize + BigInteger.ONE
        } else {
            (indMin - initialLower) / setSize
        }

        // The first step is to shift the affine so that the lower value is definitely between min and max.
        // Makes things a lot easier to reason about.
        val (lower, upper) = initialLower + distanceToShunt to initialUpper + distanceToShunt

        assert(lower >= indMin) {
            "Lower bound $lower is below minimum value $indMin"
        }
        assert(lower <= indMax) {
            "Lower bound $lower is above maximum value $indMax"
        }

        if (upper - lower >= setSize) {
            // In this case we're covering the whole range no matter what.
            return listOf(indMin.castInto<T>(clazz) to indMax.castInto(clazz))
        }

        if (upper <= indMax) {
            // Easy-peasy, we fit, all is good.
            return listOf(lower.castInto<T>(clazz) to upper.castInto(clazz))
        }

        return listOf(
            upper.castInto<T>(clazz) to indMax.castInto(clazz),
            indMin.castInto<T>(clazz) to lower.castInto(clazz)
        )
    }

    fun add(other: Affine<T>): Affine<T> = Affine(ind, affine.add(other.affine))
    fun subtract(other: Affine<T>): Affine<T> = Affine(ind, affine.subtract(other.affine))
    fun multiply(other: Affine<T>): Affine<T> = Affine(ind, affine.multiply(other.affine))
    fun divide(other: Affine<T>): Affine<T> {
        if (other.isExactly(1)) {
            return this
        }
        if (this.isExactly(0)) {
            return this
        }

        TODO("Attempted to divide $this by $other")
    }

    companion object {
        fun <T : Number> fromConstant(ind: NumberSetIndicator<T>, constant: T): Affine<T> =
            Affine(ind, IntegerAffine.fromConstant(constant.toLong()))

        fun <T : Number> full(ind: NumberSetIndicator<T>): Affine<T> =
            Affine(
                ind,
                IntegerAffine.fromRange(
                    ind.getMinValue().toLong(),
                    ind.getMaxValue().toLong(),
                    Context.Key.EphemeralKey.new()
                )
            )

        /**
         * Try to avoid using if possible, creates an ephemeral key and therefore
         * drops any variance information.
         */
        fun <T : Number> fromRange(ind: NumberSetIndicator<T>, lower: T, upper: T): Affine<T> =
            fromRange(ind, Context.Key.EphemeralKey.new(), lower, upper)

        fun <T : Number> fromRange(ind: NumberSetIndicator<T>, key: Context.Key, lower: T, upper: T): Affine<T> =
            Affine(
                ind,
                IntegerAffine.fromRange(
                    lower.toLong(),
                    upper.toLong(),
                    key
                )
            )
    }
}