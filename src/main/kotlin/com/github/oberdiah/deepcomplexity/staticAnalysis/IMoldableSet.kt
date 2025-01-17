package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface IMoldableSet {
    /**
     * The class of the elements in the set.
     */
    fun getClass(): KClass<*>
    fun union(other: IMoldableSet): IMoldableSet
    fun intersect(other: IMoldableSet): IMoldableSet
    fun invert(): IMoldableSet
    fun contains(element: Any): Boolean
}