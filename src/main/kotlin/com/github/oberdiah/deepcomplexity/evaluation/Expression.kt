package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

sealed class Expression<T : MoldableSet<T>>(val setClazz: KClass<*>) {
    abstract fun evaluate(): T

    inline fun <reified R : MoldableSet<R>> attemptCastTo(): Expression<R>? {
        if (setClazz == R::class) {
            return this as Expression<R>
        }
        return null
    }
}