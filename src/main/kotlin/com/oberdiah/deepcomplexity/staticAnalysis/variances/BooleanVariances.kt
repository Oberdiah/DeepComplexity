package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet

data class BooleanVariances(private val value: BooleanSet) : Variances<Boolean> {
    override fun toString(): String = value.toString()

    fun booleanInvert(): BooleanVariances = BooleanVariances(value.booleanInvert())
    override val ind: Indicator<Boolean> = BooleanIndicator

    override fun <Q : Any> cast(newInd: Indicator<Q>, constraints: Constraints): Variances<Q> {
        if (newInd == ind) {
            // Safety: newInd == ind.
            @Suppress("UNCHECKED_CAST")
            return this as Variances<Q>
        }
        throw IllegalArgumentException("Cannot cast boolean to $newInd")
    }

    override fun collapse(constraints: Constraints): ISet<Boolean> = value

    override fun varsTracking(): Collection<Key> = emptyList()

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<Boolean> {
        return this
    }

    override fun toDebugString(constraints: Constraints): String = value.toString()

    fun booleanOperation(other: BooleanVariances, operation: BooleanOp): BooleanVariances {
        return BooleanVariances(value.booleanOperation(other.value, operation))
    }
}