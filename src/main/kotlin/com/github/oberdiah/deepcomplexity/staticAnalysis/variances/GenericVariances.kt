package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet

class GenericVariances<T : Any>(private val value: GenericSet<T>) : Variances<T> {
    override val ind: SetIndicator<T>
        get() = TODO("Not yet implemented")

    override fun toDebugString(): String {
        TODO("Not yet implemented")
    }

    override fun varsTracking(): Collection<Context.Key> = emptyList()

    override fun collapse(): ISet<T> = value

    override fun reduceAndSimplify(scope: ExprEvaluate.Scope): Variances<T> {
        return this
    }

    override fun updateConstraints(constraints: Constraints): Variances<T> {
        TODO("Not yet implemented")
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Variances<Q>? {
        TODO("Not yet implemented")
    }
}