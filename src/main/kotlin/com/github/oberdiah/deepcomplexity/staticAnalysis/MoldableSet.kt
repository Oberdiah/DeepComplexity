package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

/**
 * This is the set of possible values an expression can take.
 */
sealed interface MoldableSet {
    /**
     * The class of the elements in the set.
     *
     * T may not be equal to the class e.g. in the case of numbers,
     * T is a DD but the class is Int, Double, etc.
     */
    fun getClass(): KClass<*>

    companion object {
        fun psiClassToMoldableSetClass() {

        }
    }
}