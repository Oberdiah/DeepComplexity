package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.SetClass
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface IMoldableSet<T : IMoldableSet<T>> {
    fun getSetIndicator(): SetIndicator<T>

    /**
     * The class of the elements in the set.
     */
    fun getClass(): KClass<*>

    /**
     * You might think this is completely pointless, but it allows us to get the interface back
     * from a class that implements it.
     */
    fun getSetClass(): SetClass
    fun union(other: T): T
    fun intersect(other: T): T
    fun invert(): T
    fun contains(element: Any): Boolean
}