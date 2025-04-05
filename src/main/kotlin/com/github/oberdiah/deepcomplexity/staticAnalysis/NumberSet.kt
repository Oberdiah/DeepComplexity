package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.isOne

sealed interface NumberSet<Self> :
    ConstrainedSetCollection<Self> where Self : ConstrainedSetCollection<Self>, Self : NumberSet<Self> {
    /**
     * Returns the set of ranges that this number set represents.
     * The ranges are inclusive, in order, and non-overlapping.
     */
    fun getAsRanges(): List<Pair<Number, Number>>

    fun arithmeticOperation(other: Self, operation: BinaryNumberOp): Self
    fun comparisonOperation(other: Self, operation: ComparisonOp): BooleanSet
    fun negate(): Self

    /**
     * Given a set of `terms` to apply to this set on each iteration, evaluate
     * the number of iterations required to exit the `valid` number set.
     *
     * You can think of this set as being the 'initial' state.
     */
    fun evaluateLoopingRange(changeTerms: ConstraintSolver.EvaluatedCollectedTerms<Self>, valid: Self): Self

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): Self

    fun isOne(): Boolean {
        val ranges = getAsRanges()
        return ranges.size == 1 && ranges[0].first == ranges[0].second && ranges[0].first.isOne()
    }

    /**
     * Returns the full range of this set, from smallest to largest value.
     */
    fun getRange(): Pair<Number, Number>

    companion object {
        fun <T : NumberSet<T>> zero(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : TypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                return indicator.newFromConstant(indicator.getZero())
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> one(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : TypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                return indicator.newFromConstant(indicator.getOne())
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : Number, Self : TypedNumberSet<T, Self>> singleValue(value: T): Self {
            return SetIndicator.fromValue<T, Self>(value).newFromConstant(value)
        }
    }
}
