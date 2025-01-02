package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

sealed interface Expression<T : MoldableSet<T>> {
    fun getSetClass(): KClass<*>

    fun evaluate(): T
}

inline fun <reified R : MoldableSet<R>> Expression<*>.attemptCastTo(): Expression<R>? {
    if (getSetClass() == R::class) {
        @Suppress("UNCHECKED_CAST")
        return this as Expression<R>
    }
    return null
}