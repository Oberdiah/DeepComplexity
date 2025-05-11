package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.BundleSet

object CastSolver {
    fun <T : Any> castFrom(
        from: BundleSet<*>,
        targetType: SetIndicator<T>,
        implicit: Boolean
    ): BundleSet<T> {
        if (from.ind == targetType) {
            @Suppress("UNCHECKED_CAST")
            return from as BundleSet<T>
        }

        return from.cast(targetType)!!
    }
}