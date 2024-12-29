package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet

interface Expression<T : MoldableSet<*>> {
    fun evaluate(): T
}