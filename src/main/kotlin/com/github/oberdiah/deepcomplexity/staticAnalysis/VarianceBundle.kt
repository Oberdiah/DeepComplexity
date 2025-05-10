package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

interface VarianceBundle<T : Any> {
    fun getIndicator(): SetIndicator<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>): VarianceBundle<Q>?
    fun union(other: VarianceBundle<T>): VarianceBundle<T>
    fun collapse(): Bundle<T>
}