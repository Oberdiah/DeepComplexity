package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle

object CastSolver {
    fun <T : Any> castFrom(
        from: Bundle<*>,
        targetType: SetIndicator<T>,
        implicit: Boolean
    ): Bundle<T> {
        if (from.ind == targetType) {
            @Suppress("UNCHECKED_CAST")
            return from as Bundle<T>
        }

        return from.cast(targetType)!!
    }
}