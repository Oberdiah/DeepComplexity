package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ObjectSet

data class ObjectVariances(private val value: ObjectSet, override val ind: ObjectIndicator) :
    Variances<HeapMarker> {
    override fun toString(): String = value.toString()

    override fun <Q : Any> cast(newInd: Indicator<Q>, constraints: Constraints): Variances<Q> {
        if (newInd == ind) {
            // Safety: newInd == ind.
            @Suppress("UNCHECKED_CAST")
            return this as Variances<Q>
        }
        throw IllegalArgumentException("Cannot cast object to $newInd")
    }

    override fun collapse(constraints: Constraints): ISet<HeapMarker> = value

    override fun varsTracking(): Collection<Key> = emptyList()

    override fun toDebugString(constraints: Constraints): String = value.toString()
}