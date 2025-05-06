package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

interface Bundle<T : Any> {
    fun getIndicator(): SetIndicator<T>
    fun invert(): Bundle<T>
    fun <Q : Any> cast(indicator: SetIndicator<Q>): Bundle<Q>?
    fun contains(element: T): Boolean
    fun isEmpty(): Boolean
    fun intersect(other: Bundle<T>): Bundle<T>
    fun union(other: Bundle<T>): Bundle<T>
    fun toDebugString(): String
}