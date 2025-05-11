package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

class GenericBundle<T : Any>(val values: Set<T>) : Bundle<T> {
    override fun toConstVariance(): Variances<T> {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val ind: SetIndicator<T>
        get() = TODO("Not yet implemented")

    override fun invert(): Bundle<T> {
        TODO("Not yet implemented")
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>? {
        TODO("Not yet implemented")
    }

    override fun contains(element: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun intersect(other: Bundle<T>): Bundle<T> {
        TODO("Not yet implemented")
    }

    override fun union(other: Bundle<T>): Bundle<T> {
        TODO("Not yet implemented")
    }
}