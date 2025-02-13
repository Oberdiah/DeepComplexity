package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet

object CastSolver {
    fun <T : IMoldableSet<T>> castFrom(from: IMoldableSet<*>, targetType: SetIndicator<T>, implicit: Boolean): T {
        val currentType = from.getSetIndicator()
        if (currentType == targetType) {
            @Suppress("UNCHECKED_CAST")
            return from as T
        }

        return from.cast(targetType)
    }
}