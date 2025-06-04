package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet

data class BooleanVariances(private val value: BooleanSet) : Variances<Boolean> {
    override fun toString(): String = value.toString()

    fun invert(): BooleanVariances = BooleanVariances(value.invert())
    override val ind: SetIndicator<Boolean> = BooleanSetIndicator

    override fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q>? =
        throw IllegalArgumentException("Cannot cast boolean to $newInd")

    override fun collapse(constraints: Constraints): ISet<Boolean> = value

    override fun varsTracking(): Collection<Context.Key> = emptyList()

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<Boolean> {
        return this
    }

    override fun toDebugString(constraints: Constraints): String = value.toString()

    fun booleanOperation(other: BooleanVariances, operation: BooleanOp): BooleanVariances {
        return BooleanVariances(value.booleanOperation(other.value, operation))
    }
}