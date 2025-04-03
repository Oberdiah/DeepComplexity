package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import kotlin.reflect.KClass

interface GenericSet : ConstrainedSet<GenericSet> {
    companion object {
        inline fun <reified T : Any> singleValue(value: T): GenericSetImpl<T> {
            return GenericSetImpl(T::class, setOf(value))
        }

        fun empty(): GenericSet {
            return GenericSetImpl<Any>(Any::class, emptySet())
        }

        fun everyValue(): GenericSet {
            TODO()
        }
    }

    class GenericSetImpl<T : Any>(private val clazz: KClass<T>, private val values: Set<T>) : GenericSet {
        override fun getSetIndicator(): SetIndicator<GenericSet> {
            return GenericSetIndicator
        }

        override fun union(other: GenericSet): GenericSet {
            if (other !is GenericSetImpl<T>) {
                throw IllegalArgumentException("Cannot union with a different set type")
            }
            return GenericSetImpl(clazz, values.union(other.values))
        }

        override fun toDebugString(): String {
            return toString()
        }

        override fun intersect(other: GenericSet): GenericSet {
            if (other !is GenericSetImpl<T>) {
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

        override fun <Q : ConstrainedSet<Q>> cast(indicator: SetIndicator<Q>): Q {
            TODO("Not yet implemented :)")
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