package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface IMoldableSet<T : IMoldableSet<T>> {
    /**
     * The class of the elements in the set.
     */
    fun getClass(): KClass<*>

    /**
     * You might think this is completely pointless, but it allows us to get the interface back
     * from a class that implements it.
     */
    fun getSetClass(): KClass<*>
    fun union(other: T): T
    fun intersect(other: T): T
    fun invert(): T
    fun contains(element: Any): Boolean
}