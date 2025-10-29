package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ObjectSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into

data class ObjectVariances(private val value: ObjectSet, override val ind: ObjectSetIndicator) :
    Variances<HeapMarker> {
    override fun toString(): String = value.toString()

    fun invert(): ObjectVariances = ObjectVariances(value.invert().into(), ind)

    override fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q> =
        throw IllegalArgumentException("Cannot cast boolean to $newInd")

    override fun collapse(constraints: Constraints): ISet<HeapMarker> = value

    override fun varsTracking(): Collection<Key> = emptyList()

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<HeapMarker> {
        return this
    }

    override fun generateConstraintsFrom(
        other: Variances<HeapMarker>,
        comparisonOp: ComparisonOp,
        incomingConstraints: Constraints
    ): Constraints {
        // We might want to do something more interesting later.
        return Constraints.completelyUnconstrained()
    }

    override fun toDebugString(constraints: Constraints): String = value.toString()
}