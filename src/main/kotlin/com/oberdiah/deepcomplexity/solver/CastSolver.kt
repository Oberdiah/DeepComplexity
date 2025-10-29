package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle

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