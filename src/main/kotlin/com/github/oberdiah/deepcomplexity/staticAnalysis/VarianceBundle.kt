package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.Constraints
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

interface VarianceBundle<T : Any> {
    val ind: SetIndicator<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>): VarianceBundle<Q>?
    fun collapse(constraints: Constraints): Bundle<T>
    fun toDebugString(constraints: Constraints): String
}