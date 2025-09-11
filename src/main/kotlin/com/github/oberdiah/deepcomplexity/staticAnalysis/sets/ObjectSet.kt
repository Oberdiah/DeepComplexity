package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.evaluation.Context.Key
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
class ObjectSet private constructor(val values: MathematicalSet<Key.HeapKey>, override val ind: ObjectSetIndicator) :
    ISet<Key.HeapKey> {
    companion object {
        fun fromConstant(constant: Key.HeapKey): ObjectSet =
            ObjectSet(MathematicalSet.of(constant), constant.ind)

        fun newEmptySet(ind: ObjectSetIndicator): ObjectSet = ObjectSet(MathematicalSet.empty(), ind)
        fun newFullSet(ind: ObjectSetIndicator): ObjectSet = ObjectSet(MathematicalSet.full(), ind)
    }

    override fun toString(): String = values.toString()

    override fun size(): Long = values.size.toLong()
    override fun invert(): ISet<Key.HeapKey> = ObjectSet(values.invert(), ind)
    override fun contains(element: Key.HeapKey): Boolean = values.contains(element)
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun isFull(): Boolean = values.isFull()
    override fun toConstVariance(): Variances<Key.HeapKey> = ObjectVariances(this, ind)
    override fun intersect(other: ISet<Key.HeapKey>): ISet<Key.HeapKey> =
        ObjectSet(values.intersect(other.into().values), ind)

    override fun union(other: ISet<Key.HeapKey>): ISet<Key.HeapKey> = ObjectSet(values.union(other.into().values), ind)

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        TODO("Not yet implemented")
    }
}