package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet

interface Variances<T : Any> {
    val ind: SetIndicator<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q>?
    fun collapse(constraints: Constraints): ISet<T>
    fun toDebugString(constraints: Constraints): String

    /**
     * Returns true if this variance is currently tracking the given key, i.e. the variance would
     * return a different value if that key was constrained.
     */
    fun varsTracking(): Collection<Context.Key>
    fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<T>
}