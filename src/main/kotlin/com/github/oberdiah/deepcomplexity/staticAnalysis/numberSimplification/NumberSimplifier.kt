package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.Ranges
import kotlin.collections.filter
import kotlin.to

class NumberSimplifier<T : Number, Self : FullyTypedNumberSet<T, Self>>(private val setIndicator: NumberSetIndicator<T, Self>) {
    /**
     * The ranges are inclusive, in order, and non-overlapping.
     */
    fun distillToSet(data: NumberData<T>, shouldWrap: Boolean): List<Pair<T, T>> {
        var data = data

        val maxIterations = 20

        for (i in 0..maxIterations) {
            println("Distillation iteration $i")

            val allKeys = data.getKeys()
            val lonelyKeys = allKeys.filter { key -> allKeys.count { it == key } == 1 }.toSet()
            val (result, changed) = applyRules(data, lonelyKeys)

            if (!changed) {
                if (result !is Ranges) {
                    throw IllegalStateException("Distillation did not converge to Ranges")
                }

                return result.toRangePairs()
            }

            data = result
        }

        throw IllegalStateException("Distillation did not converge after $maxIterations iterations")
    }

    private fun applyRules(data: NumberData<T>, lonelyKeys: Set<Context.Key>): Pair<NumberData<T>, Boolean> {
        return when (data) {
            is Empty -> data to false
            is Ranges -> data to false
            is Union -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty && rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs is Empty) {
                    return rhs to true
                }

                if (rhs is Empty) {
                    return lhs to true
                }

                // These equalities will give us really bad O(n) performance.
                if (lhs == rhs) {
                    return lhs to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.union(rhs) to true
                }

                return Union(lhs, rhs) to (changed1 || changed2)
            }

            is Intersection -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs == rhs) {
                    return lhs to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.intersection(rhs) to true
                }

                return Intersection(lhs, rhs) to (changed1 || changed2)
            }

            is Inversion -> {
                val (set, changed) = applyRules(data.set, lonelyKeys)
                // Double inversion cancels out
                if (set is Inversion) {
                    return set.set to true
                }

                if (set is Ranges) {
                    return set.invert() to true
                }

                return Inversion(set) to changed
            }

            is Addition -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs.isConfirmedToBe(0)) {
                    return rhs to true
                }

                if (rhs.isConfirmedToBe(0)) {
                    return lhs to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.add(rhs) to true
                }

                return Addition(lhs, rhs) to (changed1 || changed2)
            }

            is Subtraction -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs == rhs) {
                    return Ranges.fromConstant(setIndicator.getZero(), setIndicator) to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.subtract(rhs) to true
                }

                return Subtraction(lhs, rhs) to (changed1 || changed2)
            }

            is Multiplication -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs.isConfirmedToBe(0) || rhs.isConfirmedToBe(0)) {
                    return Ranges.fromConstant(setIndicator.getZero(), setIndicator) to true
                }

                if (lhs.isConfirmedToBe(1)) {
                    return rhs to true
                }

                if (rhs.isConfirmedToBe(1)) {
                    return lhs to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.multiply(rhs) to true
                }

                return Multiplication(lhs, rhs) to (changed1 || changed2)
            }

            is Division -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs.isConfirmedToBe(0)) {
                    return Ranges.fromConstant(setIndicator.getZero(), setIndicator) to true
                }

                if (rhs.isConfirmedToBe(1)) {
                    return lhs to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.divide(rhs) to true
                }

                return Division(lhs, rhs) to (changed1 || changed2)
            }
        }
    }
}