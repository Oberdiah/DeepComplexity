package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

class GenericSet<T>(private val clazz: KClass<*>, private val values: Set<T>) : MoldableSet {
    companion object {
        inline fun <reified T> singleValue(value: T): GenericSet<T> {
            return GenericSet(T::class, setOf(value))
        }
    }

    override fun getClass(): KClass<*> {
        return clazz
    }

    override fun union(other: MoldableSet): MoldableSet {
        if (other !is GenericSet<*>) {
            throw IllegalArgumentException("Cannot union with a different set type")
        }
        return GenericSet(clazz, values.union(other.values))
    }

    fun contains(other: T): Boolean {
        return values.contains(other)
    }
}