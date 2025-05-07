package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

class GenericSet<T : Any>(val values: Set<T>) : Bundle<T> {
    override fun associateVariance(key: Context.Key): Bundle<T> {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getIndicator(): SetIndicator<T> {
        TODO("Not yet implemented")
    }

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