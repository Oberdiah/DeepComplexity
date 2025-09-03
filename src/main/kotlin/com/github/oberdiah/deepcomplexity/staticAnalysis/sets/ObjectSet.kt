package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.staticAnalysis.HeapIdent
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
class ObjectSet private constructor(val values: MathematicalSet<HeapIdent>, override val ind: ObjectSetIndicator) :
    ISet<HeapIdent> {
    companion object {
        fun fromConstant(ind: ObjectSetIndicator, constant: HeapIdent): ObjectSet =
            ObjectSet(MathematicalSet.of(constant), ind)

        fun newEmptySet(ind: ObjectSetIndicator): ObjectSet = ObjectSet(MathematicalSet.empty(), ind)
        fun newFullSet(ind: ObjectSetIndicator): ObjectSet = ObjectSet(MathematicalSet.full(), ind)
    }

    override fun size(): Long = values.size.toLong()
    override fun invert(): ISet<HeapIdent> = ObjectSet(values.invert(), ind)
    override fun contains(element: HeapIdent): Boolean = values.contains(element)
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun isFull(): Boolean = values.isFull()
    override fun toConstVariance(): Variances<HeapIdent> = ObjectVariances(this, ind)
    override fun intersect(other: ISet<HeapIdent>): ISet<HeapIdent> =
        ObjectSet(values.intersect(other.into().values), ind)

    override fun union(other: ISet<HeapIdent>): ISet<HeapIdent> = ObjectSet(values.union(other.into().values), ind)

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        TODO("Not yet implemented")
    }
}