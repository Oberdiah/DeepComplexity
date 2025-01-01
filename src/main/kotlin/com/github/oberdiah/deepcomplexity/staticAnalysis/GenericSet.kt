package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

class GenericSet<T>(private val clazz: KClass<*>, private val value: T) : MoldableSet {
    companion object {
        inline fun <reified T> singleValue(value: T): GenericSet<T> {
            return GenericSet(T::class, value)
        }
    }

    override fun getClass(): KClass<*> {
        return clazz
    }

    fun contains(other: T): Boolean {
        return other == value
    }
}