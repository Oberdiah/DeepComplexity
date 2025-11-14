package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle

object CastSolver {
    fun <T : Any> castFrom(
        from: Bundle<*>,
        targetType: Indicator<T>,
        implicit: Boolean
    ): Bundle<T> {
        if (from.ind == targetType) {
            @Suppress("UNCHECKED_CAST")
            return from as Bundle<T>
        }

        return from.cast(targetType)!!
    }
}