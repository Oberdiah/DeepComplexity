package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface IMoldableSet<T : IMoldableSet<T>> {
    fun getSetIndicator(): SetIndicator<T>
    fun union(other: T): T

    /**
     * Although intersect is a commutative operation, calculations and optimizations
     * are implemented assuming the left hand side (this set) is the more complex set and
     * the right hand side (`other`) is a constraint on that set.
     */
    fun intersect(other: T): T
    fun invert(): T
    fun contains(element: Any): Boolean

    /**
     * Implementations don't need to check for their own type, that's
     * already been handled.
     */
    fun <Q : IMoldableSet<Q>> cast(indicator: SetIndicator<Q>): Q
}