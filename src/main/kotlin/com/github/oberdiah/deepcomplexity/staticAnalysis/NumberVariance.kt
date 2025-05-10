package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver

class NumberVariance<T : Number>(private val set: NumberSet<T>) : VarianceBundle<T> {
    override fun getIndicator(): SetIndicator<T> = set.getIndicator()
    override fun <Q : Any> cast(newInd: SetIndicator<Q>): VarianceBundle<Q>? {
        if (newInd !is NumberSetIndicator<*>) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return set.cast(newInd)?.let { NumberVariance(it.into()) } as NumberVariance<Q>?
    }

    fun negate(): NumberVariance<T> = set.negate().withEphemeralVariance().into()

    override fun union(other: VarianceBundle<T>): VarianceBundle<T> =
        NumberVariance(set.union(other.into().set) as NumberSet<T>)

    override fun collapse(): Bundle<T> = set

    fun isOne(): Boolean = set.isOne()
    fun arithmeticOperation(
        other: NumberVariance<T>,
        operation: BinaryNumberOp
    ): NumberVariance<T> = set.arithmeticOperation(other.set.into(), operation).withEphemeralVariance().into()

    fun comparisonOperation(
        other: NumberVariance<T>,
        operation: ComparisonOp
    ): BooleanSet.BooleanVariance = set.comparisonOperation(other.set, operation).withEphemeralVariance().into()

    fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<T>,
        valid: NumberSet<T>
    ): NumberVariance<T> = set.evaluateLoopingRange(changeTerms, valid).withEphemeralVariance().into()

}