package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet

object CastSolver {
    fun <T : IMoldableSet<T>> castFrom(from: IMoldableSet<*>, targetType: SetIndicator<T>, implicit: Boolean): T {
        if (from.getSetIndicator() == targetType) {
            @Suppress("UNCHECKED_CAST")
            return from as T
        }

        TODO()
    }
}