package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

class GenericSet<T : Any>(val values: Set<T>) : ISet<T> {
    override fun toConstVariance(): Variances<T> {
        TODO("Not yet implemented")
    }

    override fun isFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val ind: SetIndicator<T>
        get() = TODO("Not yet implemented")

    override fun invert(): ISet<T> {
        TODO("Not yet implemented")
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        TODO("Not yet implemented")
    }

    override fun contains(element: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun intersect(other: ISet<T>): ISet<T> {
        TODO("Not yet implemented")
    }

    override fun union(other: ISet<T>): ISet<T> {
        TODO("Not yet implemented")
    }
}