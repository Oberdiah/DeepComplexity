package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

interface GenericSet : MoldableSet {
    companion object {
        inline fun <reified T> singleValue(value: T): GenericSetImpl<T> {
            return GenericSetImpl(T::class, setOf(value))
        }
    }

    class GenericSetImpl<T>(private val clazz: KClass<*>, private val values: Set<T>) : GenericSet {
        override fun getClass(): KClass<*> {
            return clazz
        }

        override fun union(other: MoldableSet): MoldableSet {
            if (other !is GenericSetImpl<*>) {
                throw IllegalArgumentException("Cannot union with a different set type")
            }
            return GenericSetImpl(clazz, values.union(other.values))
        }

        override fun intersect(other: MoldableSet): MoldableSet {
            if (other !is GenericSetImpl<*>) {
                throw IllegalArgumentException("Cannot intersect with a different set type")
            }
            return GenericSetImpl(clazz, values.intersect(other.values))
        }

        override fun invert(): MoldableSet {
            TODO(
                "Not yet implemented. It's definitely possible, " +
                        "but requires a new InvertedGenericSet class and I've not bothered yet."
            )
        }

        fun contains(other: T): Boolean {
            return values.contains(other)
        }
    }
}