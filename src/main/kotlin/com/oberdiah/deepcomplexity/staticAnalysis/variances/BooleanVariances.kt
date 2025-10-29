package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet

data class BooleanVariances(private val value: BooleanSet) : Variances<Boolean> {
    override fun toString(): String = value.toString()

    fun invert(): BooleanVariances = BooleanVariances(value.invert())
    override val ind: SetIndicator<Boolean> = BooleanSetIndicator

    override fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q> =
        throw IllegalArgumentException("Cannot cast boolean to $newInd")

    override fun collapse(constraints: Constraints): ISet<Boolean> = value

    override fun varsTracking(): Collection<Key> = emptyList()

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<Boolean> {
        return this
    }

    override fun generateConstraintsFrom(
        other: Variances<Boolean>,
        comparisonOp: ComparisonOp,
        incomingConstraints: Constraints
    ): Constraints {
        TODO("Not yet implemented")
    }

    override fun toDebugString(constraints: Constraints): String = value.toString()

    fun booleanOperation(other: BooleanVariances, operation: BooleanOp): BooleanVariances {
        return BooleanVariances(value.booleanOperation(other.value, operation))
    }
}