package com.oberdiah.deepcomplexity.staticAnalysis

import com.oberdiah.deepcomplexity.context.HeapMarker

interface HasIndicator<T : Any> {
    val ind: Indicator<T>
}

interface CanBeCast<T : Any> : HasIndicator<T> {
    fun coerceToNumbers(): HasIndicator<out Number> {
        if (this.ind !is NumberIndicator<*>) throw IllegalStateException("Failed to cast to a number: $this ($ind)")
        @Suppress("UNCHECKED_CAST")
        return this as HasIndicator<out Number>
    }

    fun coerceToObject(): HasIndicator<HeapMarker> {
        if (this.ind !is ObjectIndicator) throw IllegalStateException("Failed to cast to an object: $this ($ind)")
        @Suppress("UNCHECKED_CAST")
        return this as HasIndicator<HeapMarker>
    }

    fun <Q : Any> coerceTo(newInd: Indicator<Q>): HasIndicator<Q> {
        if (this.ind != newInd) throw IllegalStateException("Failed to cast to $newInd: $this ($ind)")
        @Suppress("UNCHECKED_CAST")
        return this as HasIndicator<Q>
    }

    /**
     * If this object is already the type desired, does nothing (returns self),
     * otherwise returns a materially different cast version of the object.
     *
     * If that cannot be done (e.g. casting `Int` to `String`), returns null.
     */
    fun <Q : Any> tryCastTo(newInd: Indicator<Q>): HasIndicator<Q>?

    /**
     * If this object is already the type desired, does nothing.
     * Otherwise, performs a hard cast to the new type. If the hard cast fails, it throws an exception.
     */
    fun <Q : Any> castTo(newInd: Indicator<Q>): HasIndicator<Q> {
        @Suppress("UNCHECKED_CAST")
        if (this.ind == newInd) return this as HasIndicator<Q>
        return tryCastTo(newInd) ?: throw IllegalStateException("Failed to cast to $newInd: $this ($ind)")
    }
}