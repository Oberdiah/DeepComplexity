package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

sealed class Expression<out T : MoldableSet>(val setClazz: KClass<*>) {
    abstract fun evaluate(): T

    inline fun <reified T : MoldableSet> attemptCastTo(): Expression<T>? {
        if (setClazz == T::class) {
            return this as Expression<T>
        }
        return null
    }
}