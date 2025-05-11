package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.GenericBundle

class GenericVariances<T : Any>(private val value: GenericBundle<T>) : Variances<T> {
    override val ind: SetIndicator<T>
        get() = TODO("Not yet implemented")

    override fun toDebugString(constraints: Constraints): String {
        TODO("Not yet implemented")
    }

    override fun collapse(constraints: Constraints): Bundle<T> = value

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Variances<Q>? {
        TODO("Not yet implemented")
    }
}