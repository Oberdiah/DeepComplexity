package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.Constraints
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.Bundle

interface Variances<T : Any> {
    val ind: SetIndicator<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>): Variances<Q>?
    fun collapse(constraints: Constraints): Bundle<T>
    fun toDebugString(constraints: Constraints): String
}