package com.oberdiah.deepcomplexity.evaluation

import java.lang.ref.WeakReference
import java.util.*
import kotlin.reflect.jvm.isAccessible

object ExprPool {
    private data class IntWrapper(val value: Int)

    private val expressions = WeakHashMap<IntWrapper, MutableList<WeakReference<Expr<*>>>>()
    private val creationFlag = ThreadLocal.withInitial { false }
    fun isCreating(): Boolean = creationFlag.get()

    internal inline fun <reified T : Expr<*>> create(vararg args: Any?): T {
        val constructor = T::class.constructors.firstOrNull()
            ?: throw IllegalArgumentException("No constructor found for ${T::class}")
        creationFlag.set(true)
        val instance = try {
            constructor.isAccessible = true
            constructor.call(*args)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create ${T::class.simpleName}", e)
        } finally {
            creationFlag.set(false)
        }

        // TODO should be
        // return intern(instance)
        return instance
    }

    @Synchronized
    private fun <T : Expr<*>> intern(candidate: T): T {
        val hash = candidate.hashCode()
        val bucket = expressions.computeIfAbsent(IntWrapper(hash)) { mutableListOf() }

        val iterator = bucket.iterator()
        while (iterator.hasNext()) {
            val ref = iterator.next()
            val existing = ref.get()
            if (existing == null) {
                iterator.remove()
            } else if (structuralEquals(existing, candidate)) {
                @Suppress("UNCHECKED_CAST")
                return existing as T
            }
        }

        bucket.add(WeakReference(candidate))
        return candidate
    }

    private fun structuralEquals(a: Expr<*>, b: Expr<*>): Boolean {
        if (a::class != b::class) return false
        return a.parts() == b.parts() && a.ind == b.ind
    }
}