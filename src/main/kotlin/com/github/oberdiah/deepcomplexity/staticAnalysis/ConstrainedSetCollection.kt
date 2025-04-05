package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

/**
 * This is the set of possible values an expression can take.
 */
sealed interface ConstrainedSetCollection<Self : ConstrainedSetCollection<Self>> {
    fun getSetIndicator(): SetIndicator<Self>
    fun union(other: Self): Self

    /**
     * Might return additional information beyond simply what the set contains.
     */
    fun toDebugString(): String

    /**
     * Although intersect is a commutative operation, calculations and optimizations
     * are implemented assuming the left hand side (this set) is the more complex set and
     * the right hand side (`other`) is a constraint on that set.
     */
    fun intersect(other: Self): Self
    fun invert(): Self
    fun contains(element: Any): Boolean

    fun <Q : ConstrainedSetCollection<Q>> cast(indicator: SetIndicator<Q>): Q?
}