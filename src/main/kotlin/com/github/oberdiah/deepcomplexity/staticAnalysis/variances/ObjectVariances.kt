package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.Context.Key
import com.github.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ObjectSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.into

data class ObjectVariances(private val value: ObjectSet, override val ind: ObjectSetIndicator) :
    Variances<Key.HeapKey> {
    override fun toString(): String = value.toString()

    fun invert(): ObjectVariances = ObjectVariances(value.invert().into(), ind)

    override fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q> =
        throw IllegalArgumentException("Cannot cast boolean to $newInd")

    override fun collapse(constraints: Constraints): ISet<Key.HeapKey> = value

    override fun varsTracking(): Collection<Key> = emptyList()

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<Key.HeapKey> {
        return this
    }

    override fun generateConstraintsFrom(
        other: Variances<Key.HeapKey>,
        comparisonOp: ComparisonOp,
        incomingConstraints: Constraints
    ): Constraints {
        // We might want to do something more interesting later.
        return Constraints.completelyUnconstrained()
    }

    override fun toDebugString(constraints: Constraints): String = value.toString()
}