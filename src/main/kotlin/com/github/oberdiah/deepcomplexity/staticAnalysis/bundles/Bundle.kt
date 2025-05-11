package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

interface Bundle<T : Any> {
    val ind: SetIndicator<T>
    fun invert(): Bundle<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>?
    fun contains(element: T): Boolean
    fun isEmpty(): Boolean
    fun intersect(other: Bundle<T>): Bundle<T>
    fun union(other: Bundle<T>): Bundle<T>
    fun toConstVariance(): Variances<T>
}