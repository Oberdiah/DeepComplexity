package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface IMoldableSet<T : IMoldableSet<T>> {
    fun getSetIndicator(): SetIndicator<T>
    fun union(other: T): T
    fun intersect(other: T): T
    fun invert(): T
    fun contains(element: Any): Boolean

    /**
     * Implementations don't need to check for their own type, that's
     * already been handled.
     */
    fun <Q : IMoldableSet<Q>> cast(indicator: SetIndicator<Q>): Q
}