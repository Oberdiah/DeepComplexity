package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.GenericSetClass
import com.github.oberdiah.deepcomplexity.evaluation.SetClass
import kotlin.reflect.KClass

interface GenericSet : IMoldableSet<GenericSet> {
    override fun getSetClass(): SetClass<GenericSet> {
        return GenericSetClass
    }

    companion object {
        inline fun <reified T> singleValue(value: T): GenericSetImpl<T> {
            return GenericSetImpl(T::class, setOf(value))
        }

        fun empty(): GenericSet {
            return GenericSetImpl<Any>(Any::class, emptySet())
        }

        fun everyValue(): GenericSet {
            TODO()
        }
    }

    class GenericSetImpl<T>(private val clazz: KClass<*>, private val values: Set<T>) : GenericSet {
        override fun getClass(): KClass<*> {
            return clazz
        }

        override fun union(other: GenericSet): GenericSet {
            if (other !is GenericSetImpl<*>) {
                throw IllegalArgumentException("Cannot union with a different set type")
            }
            return GenericSetImpl(clazz, values.union(other.values))
        }

        override fun intersect(other: GenericSet): GenericSet {
            if (other !is GenericSetImpl<*>) {
                throw IllegalArgumentException("Cannot intersect with a different set type")
            }
            return GenericSetImpl(clazz, values.intersect(other.values))
        }

        override fun invert(): GenericSet {
            TODO(
                "Not yet implemented. It's definitely possible, " +
                        "but requires a new InvertedGenericSet class and I've not bothered yet."
            )
        }

        override fun contains(element: Any): Boolean {
            if (element::class != clazz) {
                return false
            }

            @Suppress("UNCHECKED_CAST")
            return values.contains(element as T)
        }
    }
}