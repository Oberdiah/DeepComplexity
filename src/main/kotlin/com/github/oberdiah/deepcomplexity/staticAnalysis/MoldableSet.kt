package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface MoldableSet {
    /**
     * The class of the elements in the set.
     */
    fun getClass(): KClass<*>
    fun union(other: MoldableSet): MoldableSet
    fun intersect(other: MoldableSet): MoldableSet
    fun invert(): MoldableSet
}