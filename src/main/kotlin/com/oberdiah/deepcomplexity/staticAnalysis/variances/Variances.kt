package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.HasIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.utilities.into

/**
 * A set of possible values of type T, alongside optional variance data that can be used to
 * track the values as operations are performed on them.
 *
 * Doesn't inherit `CanBeCast` because it requires `constraints` in order to do so in the best manner.
 */
interface Variances<T : Any> : HasIndicator<T> {
    override val ind: Indicator<T>

    fun <Q : Any> attemptHardCastTo(newInd: Indicator<Q>, constraints: Constraints): Variances<Q>?
    fun <Q : Any> coerceTo(newInd: Indicator<Q>): Variances<Q> {
        if (this.ind != newInd) throw IllegalStateException("Failed to cast to $newInd: $this ($ind)")
        @Suppress("UNCHECKED_CAST")
        return this as Variances<Q>
    }

    fun collapse(constraints: Constraints): ISet<T>
    fun toDebugString(constraints: Constraints): String

    fun constrainedBy(constraints: Constraints) = Bundle.ConstrainedVariances.new(this, constraints)

    /**
     * Returns true if this variance is currently tracking the given key, i.e. the variance would
     * return a different value if that key was constrained.
     */
    fun varsTracking(): Collection<EvaluationKey<*>>

    /**
     * Note: Only Numbers need to worry about handling [comparisonOp]s that aren't equality or inequality,
     * all other types can throw if they receive one.
     *
     * "At the very least, there are improvements to this that could be made for numbers."
     * I wrote the above down in a previous comment, but there was no further information,
     * so I couldn't tell you specifically what those improvements might be.
     */
    fun comparisonOperation(
        other: Variances<T>,
        comparisonOp: ComparisonOp,
        constraints: Constraints
    ): BooleanVariances {
        return collapse(constraints)
            .comparisonOperation(other.collapse(constraints), comparisonOp)
            .toConstVariance()
            .into()
    }

    /**
     * Note: Only Numbers need to worry about handling [op]s that aren't equality or inequality,
     * all other types can throw if they receive one.
     *
     * Generate all constraints arising from a given comparison operation between this (lhs) and other (rhs).
     * These constraints are the conditions that must be satisfied for the comparison to be able to return true.
     * As usual, unreachable will be returned if the comparison cannot be satisfied under any circumstances.
     */
    fun generateConstraintsFrom(
        other: Variances<T>,
        op: ComparisonOp,
        constraints: Constraints
    ): Constraints {
        val thisCollapsed = collapse(constraints)
        val otherCollapsed = other.collapse(constraints)
        return when (thisCollapsed.comparisonOperation(otherCollapsed, op)) {
            BooleanSet.TRUE,
            BooleanSet.EITHER -> constraints

            BooleanSet.FALSE,
            BooleanSet.NEITHER -> Constraints.unreachable()
        }
    }
}