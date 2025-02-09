package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet.*
import kotlin.collections.filter
import kotlin.to

class NumberSimplifier<T : Number, Self : FullyTypedNumberSet<T, Self>>(private val setIndicator: NumberSetIndicator<T, Self>) {
    fun distillToSet(data: NumberData<T>, shouldWrap: Boolean): List<Pair<T, T>> {
        val distilled = distillNumberData(data)

        println(distilled)

        TODO()
    }

    fun distillNumberData(data: NumberData<T>): NumberData<T> {
        var data = data
        while (true) {
            val allKeys = data.getKeys()
            val lonelyKeys = allKeys.filter { key -> allKeys.count { it == key } == 1 }.toSet()
            val (result, changed) = applyRules(data, lonelyKeys)

            if (!changed) {
                break
            }

            data = result
        }

        return data
    }

    private fun applyRules(data: NumberData<T>, lonelyKeys: Set<Context.Key>): Pair<NumberData<T>, Boolean> {
        return when (data) {
            is Empty -> data to false
            is Constant -> data to false
            is UnkeyedRange -> data to false
            is Range -> {
                if (data.key in lonelyKeys) {
                    UnkeyedRange(data.start, data.end) to true
                } else {
                    data to false
                }
            }

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

                if (lhs == rhs) {
                    return lhs to true
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

                return Intersection(lhs, rhs) to (changed1 || changed2)
            }

            is Inversion -> {
                val (set, changed) = applyRules(data.set, lonelyKeys)
                // Double inversion cancels out
                if (set is Inversion) {
                    return set.set to true
                }

                return Inversion(set) to changed
            }

            // Addition, Subtraction, Multiplication, Division
            is BinaryNumberData -> {
                val (lhs, changed1) = applyRules(data.setA, lonelyKeys)
                val (rhs, changed2) = applyRules(data.setB, lonelyKeys)

                if (lhs is Empty || rhs is Empty) {
                    return Empty<T>() to true
                }

                return Addition(lhs, rhs) to (changed1 || changed2)
            }
        }
    }
}