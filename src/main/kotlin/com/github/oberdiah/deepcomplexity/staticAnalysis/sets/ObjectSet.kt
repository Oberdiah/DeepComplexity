package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.staticAnalysis.HeapIdent
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

/**
 * Tracks the possible heap allocations of a variable.
 * Doesn't track the values of the fields in the object, just which one out of several
 * possible objects it could be.
 */
class ObjectSet(val values: Set<HeapIdent>) : ISet<HeapIdent> {
    override val ind: SetIndicator<HeapIdent>
        get() = TODO("Not yet implemented")

    override fun size(): Long? = values.size.toLong()

    override fun invert(): ISet<HeapIdent> {
        TODO("Not yet implemented")
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        TODO("Not yet implemented")
    }

    override fun contains(element: HeapIdent): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isFull(): Boolean {
        TODO("Not yet implemented")
    }

    override fun intersect(other: ISet<HeapIdent>): ISet<HeapIdent> {
        return ObjectSet(values.intersect(other.into().values))
    }

    override fun union(other: ISet<HeapIdent>): ISet<HeapIdent> {
        return ObjectSet(values.union(other.into().values))
    }

    override fun toConstVariance(): Variances<HeapIdent> {
        TODO("Not yet implemented")
    }
}