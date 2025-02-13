package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.Ranges
import com.intellij.openapi.actionSystem.DataKey.allKeys
import com.intellij.refactoring.introduceVariable.IntroduceVariableUsagesCollector.changed
import kotlin.collections.filter
import kotlin.to

object NumberSimplifier {
    /**
     * The ranges are inclusive, in order, and non-overlapping.
     */
    fun <T : Number, Self : FullyTypedNumberSet<T, Self>> distillToSet(
        ind: NumberSetIndicator<T, Self>,
        data: NumberData<T>,
        shouldWrap: Boolean
    ): List<Pair<T, T>> {
        var data = data

        val maxIterations = 20

        for (i in 0..maxIterations) {
            val (result, changed) = applyRules(ind, data)

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

    private fun <T : Number, Self : FullyTypedNumberSet<T, Self>> applyRules(
        ind: NumberSetIndicator<T, Self>,
        data: NumberData<T>
    ): Pair<NumberData<T>, Boolean> {
        return when (data) {
            is Empty -> data to false
            is Ranges -> data to false
            is Cast<*, *> -> {
                // Somehow we got away without a single suppress here!
                fun <Q : Number> extra(cast: Cast<T, Q>): Pair<NumberData<T>, Boolean> {
                    val (set, changed) = applyRules(cast.insideInd, cast.set)

                    if (set is Ranges) {
                        return set.castTo(ind) to true
                    }

                    return Cast(set, cast.outsideInd, cast.insideInd) to changed
                }

                return extra(data as Cast<T, *>)
            }

            is Union -> {
                val (lhs, changed1) = applyRules(ind, data.setA)
                val (rhs, changed2) = applyRules(ind, data.setB)

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
                val (lhs, changed1) = applyRules(ind, data.setA)
                val (rhs, changed2) = applyRules(ind, data.setB)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs == rhs) {
                    return lhs to true
                }

                // priority system to give affines with more priority better chances of survival
                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.intersection(rhs) to true
                }

                return Intersection(lhs, rhs) to (changed1 || changed2)
            }

            is Inversion -> {
                val (set, changed) = applyRules(ind, data.set)
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
                val (lhs, changed1) = applyRules(ind, data.setA)
                val (rhs, changed2) = applyRules(ind, data.setB)

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
                val (lhs, changed1) = applyRules(ind, data.setA)
                val (rhs, changed2) = applyRules(ind, data.setB)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs == rhs) {
                    return Ranges.fromConstant(ind.getZero(), ind) to true
                }

                if (lhs is Ranges && rhs is Ranges) {
                    return lhs.subtract(rhs) to true
                }

                return Subtraction(lhs, rhs) to (changed1 || changed2)
            }

            is Multiplication -> {
                val (lhs, changed1) = applyRules(ind, data.setA)
                val (rhs, changed2) = applyRules(ind, data.setB)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs.isConfirmedToBe(0) || rhs.isConfirmedToBe(0)) {
                    return Ranges.fromConstant(ind.getZero(), ind) to true
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
                val (lhs, changed1) = applyRules(ind, data.setA)
                val (rhs, changed2) = applyRules(ind, data.setB)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                if (lhs.isConfirmedToBe(0)) {
                    return Ranges.fromConstant(ind.getZero(), ind) to true
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