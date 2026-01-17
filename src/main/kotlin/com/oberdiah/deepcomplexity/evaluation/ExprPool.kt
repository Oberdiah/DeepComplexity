package com.oberdiah.deepcomplexity.evaluation

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.jvm.isAccessible

object ExprPool {
    private val creationFlag = ThreadLocal.withInitial { false }
    fun isCreating(): Boolean = creationFlag.get()

    private val nextId = AtomicLong(1)
    private val queue = ReferenceQueue<Expr<*>>()

    private data class Key(
        val kind: Class<*>,
        val parts: List<Any?>
    )

    private class Entry(
        val key: Key,
        expr: Expr<*>,
        queue: ReferenceQueue<Expr<*>>
    ) : WeakReference<Expr<*>>(expr, queue)

    private val table = ConcurrentHashMap<Key, Entry>()

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

        return intern(instance)
    }

    @Synchronized
    private fun <T : Expr<*>> intern(candidate: T): T {
        while (true) {
            val ref = queue.poll() as Entry? ?: break
            table.remove(ref.key, ref)
        }

        require(candidate.transientInternIdDoNotUse == 0L) {
            "Expr $candidate is already interned."
        }

        val key = buildKey(candidate)

        table[key]?.get()?.let { existing ->
            @Suppress("UNCHECKED_CAST")
            return existing as T
        }

        candidate.assignInternId(nextId.getAndIncrement())

        table[key] = Entry(key, candidate, queue)
        return candidate
    }

    private fun buildKey(expr: Expr<*>): Key {
        val normalizedParts = expr.myParts().map { p ->
            when (p) {
                is Expr<*> -> {
                    val id = p.transientInternIdDoNotUse
                    require(id != 0L) {
                        "Child Expr is not interned yet (internId==0)."
                    }
                    id
                }

                else -> p
            }
        }
        return Key(expr.javaClass, normalizedParts)
    }
}
