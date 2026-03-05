package com.oberdiah.deepcomplexity.staticAnalysis

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour

interface HasIndicator<T : Any> {
    val ind: Indicator<T>
}

interface CanBeCast<T : Any> : HasIndicator<T> {
    fun tryCastToNumbers(): HasIndicator<out Number>? {
        if (this.ind !is NumberIndicator<*>) return null
        @Suppress("UNCHECKED_CAST")
        return this as HasIndicator<out Number>
    }

    fun castToNumbersOrThrow(): HasIndicator<out Number> =
        tryCastToNumbers() ?: throw IllegalStateException("Failed to cast to a number: $this ($ind)")

    fun tryCastToObject(): HasIndicator<HeapMarker>? {
        if (this.ind !is ObjectIndicator) return null
        @Suppress("UNCHECKED_CAST")
        return this as HasIndicator<HeapMarker>
    }

    fun castToObjectOrThrow(): HasIndicator<HeapMarker> =
        tryCastToObject() ?: throw IllegalStateException("Failed to cast to an object: $this ($ind)")

    fun <Q : Any> tryCastTo(newInd: Indicator<Q>): HasIndicator<Q>? {
        if (this.ind != newInd) return null
        @Suppress("UNCHECKED_CAST")
        return this as HasIndicator<Q>
    }

    fun <Q : Any> castOrThrow(newInd: Indicator<Q>): HasIndicator<Q> = castTo(newInd, Behaviour.Throw)

    /**
     * If this object is already the type desired, does nothing (returns self),
     * otherwise returns a materially different cast version of the object.
     *
     * If that cannot be done (e.g. casting `Int` to `String`), returns null.
     */
    fun <Q : Any> attemptHardCastTo(newInd: Indicator<Q>): HasIndicator<Q>?

    /**
     * If this object is already the type desired, does nothing. Otherwise, follows
     * [nonTrivial] and either immediately throws an exception or attempts to perform a hard cast.
     *
     * If the hard cast fails, it throws an exception.
     */
    fun <Q : Any> castTo(newInd: Indicator<Q>, nonTrivial: Behaviour): HasIndicator<Q> {
        this.tryCastTo(newInd)?.let { return it }

        return when (nonTrivial) {
            Behaviour.Throw ->
                throw IllegalStateException("Failed to cast '$this' to $newInd; (${this.ind} != $newInd)")

            Behaviour.PerformHardCast ->
                this.attemptHardCastTo(newInd)
                    ?: throw IllegalStateException("Failed to hard cast '$this' to $newInd; (${this.ind} != $newInd)")
        }
    }
}