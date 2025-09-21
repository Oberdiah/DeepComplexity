package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.evaluation.HeapMarker
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.ObjectVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.github.oberdiah.deepcomplexity.utilities.MathematicalSet

/**
 * Tracks the possible heap allocations of a variable.
 * Doesn't track the values of the fields in the object, just which one out of several
 * possible objects it could be.
 */
class ObjectSet private constructor(val values: MathematicalSet<HeapMarker>, override val ind: ObjectSetIndicator) :
    ISet<HeapMarker> {
    companion object {
        fun fromConstant(constant: HeapMarker): ObjectSet =
            ObjectSet(MathematicalSet.of(constant), constant.ind)

        fun newEmptySet(ind: ObjectSetIndicator): ObjectSet = ObjectSet(MathematicalSet.empty(), ind)
        fun newFullSet(ind: ObjectSetIndicator): ObjectSet = ObjectSet(MathematicalSet.full(), ind)
    }

    override fun toString(): String = values.toString()

    override fun size(): Long = values.size.toLong()
    override fun invert(): ISet<HeapMarker> = ObjectSet(values.invert(), ind)
    override fun contains(element: HeapMarker): Boolean = values.contains(element)
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun isFull(): Boolean = values.isFull()
    override fun toConstVariance(): Variances<HeapMarker> = ObjectVariances(this, ind)
    override fun intersect(other: ISet<HeapMarker>): ISet<HeapMarker> =
        ObjectSet(values.intersect(other.into().values), ind)

    override fun union(other: ISet<HeapMarker>): ISet<HeapMarker> = ObjectSet(values.union(other.into().values), ind)

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        TODO("Not yet implemented")
    }
}