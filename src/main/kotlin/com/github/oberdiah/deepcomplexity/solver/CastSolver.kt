package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.ConstrainedSet

object CastSolver {
    fun <T : ConstrainedSet<T>> castFrom(from: ConstrainedSet<*>, targetType: SetIndicator<T>, implicit: Boolean): T {
        val currentType = from.getSetIndicator()
        if (currentType == targetType) {
            @Suppress("UNCHECKED_CAST")
            return from as T
        }

        return from.cast(targetType)!!
    }
}