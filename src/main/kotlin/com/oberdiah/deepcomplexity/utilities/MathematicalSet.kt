package com.github.oberdiah.deepcomplexity.utilities

class MathematicalSet<T> private constructor(
    private val impl: Impl<T>
) : Set<T> by impl {
    override fun toString(): String = impl.toString()
    fun invert(): MathematicalSet<T> = MathematicalSet(impl.invert())
    fun isFull(): Boolean = impl.isFull()
    fun add(element: T): MathematicalSet<T> = MathematicalSet(impl.add(element))
    fun remove(element: T): MathematicalSet<T> = MathematicalSet(impl.remove(element))
    fun union(other: MathematicalSet<T>): MathematicalSet<T> =
        MathematicalSet(impl.union(other.impl))

    fun intersect(other: MathematicalSet<T>): MathematicalSet<T> =
        MathematicalSet(impl.intersect(other.impl))

    companion object {
        fun <T> empty(): MathematicalSet<T> =
            MathematicalSet(Normal(emptySet()))

        fun <T> full(): MathematicalSet<T> =
            MathematicalSet(Inverted(emptySet()))

        fun <T> of(vararg elements: T): MathematicalSet<T> =
            MathematicalSet(Normal(elements.toSet()))
    }

    private sealed interface Impl<T> : Set<T> {
        fun invert(): Impl<T>
        fun isFull(): Boolean
        fun add(element: T): Impl<T>
        fun remove(element: T): Impl<T>
        fun union(other: Impl<T>): Impl<T>
        fun intersect(other: Impl<T>): Impl<T>
    }

    private data class Normal<T>(val elements: Set<T>) : Impl<T> {
        override val size: Int get() = elements.size
        override fun isEmpty(): Boolean = elements.isEmpty()
        override fun contains(element: T): Boolean = elements.contains(element)
        override fun containsAll(elements: Collection<T>): Boolean = this.elements.containsAll(elements)
        override fun iterator(): Iterator<T> = elements.iterator()

        override fun invert(): Impl<T> = Inverted(elements)
        override fun isFull(): Boolean = false
        override fun add(element: T): Impl<T> = Normal(elements + element)
        override fun remove(element: T): Impl<T> = Normal(elements - element)
        override fun union(other: Impl<T>): Impl<T> = when (other) {
            is Normal -> Normal(elements + other.elements)
            is Inverted -> Inverted(other.excluded - elements)
        }

        override fun intersect(other: Impl<T>): Impl<T> = when (other) {
            is Normal -> Normal(elements intersect other.elements)
            is Inverted -> Normal(elements - other.excluded)
        }

        override fun toString(): String {
            if (elements.isEmpty()) return "∅"

            return if (elements.size <= 10) {
                elements.toString()
            } else {
                elements.take(10).toString().dropLast(1) + ", ...}"
            }
        }
    }

    private data class Inverted<T>(val excluded: Set<T>) : Impl<T> {
        override fun toString(): String {
            // Full set symbol
            if (excluded.isEmpty()) return "{ Everything }"

            return if (excluded.size <= 10) {
                "All except $excluded"
            } else {
                "All except ${excluded.take(10).toString().dropLast(1)}, ...}"
            }
        }

        override val size: Int get() = throw UnsupportedOperationException("Infinite set, no size")
        override fun isEmpty(): Boolean = false
        override fun contains(element: T): Boolean = !excluded.contains(element)
        override fun containsAll(elements: Collection<T>): Boolean =
            elements.all { !excluded.contains(it) }

        override fun iterator(): Iterator<T> =
            throw UnsupportedOperationException("Infinite set, cannot iterate")

        override fun invert(): Impl<T> = Normal(excluded)
        override fun isFull(): Boolean = excluded.isEmpty()
        override fun add(element: T): Impl<T> = Inverted(excluded - element)
        override fun remove(element: T): Impl<T> = Inverted(excluded + element)
        override fun union(other: Impl<T>): Impl<T> = when (other) {
            is Normal -> Inverted(excluded - other.elements)
            is Inverted -> Inverted(excluded intersect other.excluded)
        }

        override fun intersect(other: Impl<T>): Impl<T> = when (other) {
            is Normal -> Normal(other.elements - excluded)
            is Inverted -> Inverted(excluded union other.excluded)
        }
    }
}
