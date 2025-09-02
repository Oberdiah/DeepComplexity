package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.github.oberdiah.deepcomplexity.staticAnalysis.HeapIdent
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ObjectSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.into

data class ObjectVariances(private val value: ObjectSet, override val ind: ObjectSetIndicator) : Variances<HeapIdent> {
    override fun toString(): String = value.toString()

    fun invert(): ObjectVariances = ObjectVariances(value.invert().into(), ind)

    override fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q>? =
        throw IllegalArgumentException("Cannot cast boolean to $newInd")

    override fun collapse(constraints: Constraints): ISet<HeapIdent> = value

    override fun varsTracking(): Collection<Context.Key> = emptyList()

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<HeapIdent> {
        return this
    }

    override fun comparisonOperation(
        other: Variances<HeapIdent>,
        comparisonOp: ComparisonOp,
        constraints: Constraints
    ): BooleanVariances {
        TODO("Not yet implemented")
    }

    override fun generateConstraintsFrom(
        other: Variances<HeapIdent>,
        comparisonOp: ComparisonOp,
        incomingConstraints: Constraints
    ): Constraints {
        TODO("Not yet implemented")
    }

    override fun toDebugString(constraints: Constraints): String = value.toString()
}