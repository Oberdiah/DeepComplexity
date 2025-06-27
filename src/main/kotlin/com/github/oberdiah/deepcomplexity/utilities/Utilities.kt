package com.github.oberdiah.deepcomplexity.utilities

import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.intellij.psi.*
import org.apache.commons.numbers.core.DD
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.reflect.KClass

object Utilities {
    val DD_POSITIVE_INFINITY: DD = DD.of(Double.POSITIVE_INFINITY)
    val DD_NEGATIVE_INFINITY: DD = DD.of(Double.NEGATIVE_INFINITY)

    operator fun DD.compareTo(other: DD): Int {
        // First compare the high parts
        val hiComparison = this.hi().compareTo(other.hi())
        if (hiComparison != 0) {
            return hiComparison
        }

        // If high parts are equal, compare the low parts
        return this.lo().compareTo(other.lo())
    }

    fun DD.min(other: DD): DD {
        return if (this < other) this else other
    }

    fun DD.max(other: DD): DD {
        return if (this > other) this else other
    }

    fun DD.toStr(): String {
        if (!this.isFinite) {
            return if (this == DD_POSITIVE_INFINITY) "∞" else "-∞"
        }

        return this.bigDecimalValue().toPlainString()
    }

    fun numberToDD(value: Number): DD {
        when (value) {
            is Byte -> return DD.of(value.toInt())
            is Short -> return DD.of(value.toInt())
            is Int -> return DD.of(value)
            is Long -> return DD.of(value)
            is Float -> return DD.of(value.toDouble())
            is Double -> return DD.of(value)
        }
        throw IllegalArgumentException("Unsupported type for number conversion")
    }

    fun psiTypeToKClass(type: PsiType): KClass<*>? {
        when (type) {
            PsiTypes.byteType() -> return Byte::class
            PsiTypes.shortType() -> return Short::class
            PsiTypes.intType() -> return Int::class
            PsiTypes.longType() -> return Long::class
            PsiTypes.floatType() -> return Float::class
            PsiTypes.doubleType() -> return Double::class
            PsiTypes.booleanType() -> return Boolean::class
            PsiTypes.charType() -> return Char::class
        }
        return null
    }

    fun psiTypeToSetIndicator(type: PsiType): SetIndicator<*> {
        val clazz = psiTypeToKClass(type) ?: return GenericSetIndicator(Any::class)
        return SetIndicator.fromClass(clazz)
    }

    fun PsiElement.toStringPretty(): String {
        val name = "$this"
        // If ":" exists, we want to remove it and everything before it
        return if (name.contains(":")) {
            name.substring(name.indexOf(":") + 1)
        } else {
            name
        }
    }

    fun PsiElement.toKey(): Context.Key {
        return when (this) {
            is PsiVariable -> Context.Key.VariableKey(this)
            is PsiReturnStatement -> {
                val returnMethod = findContainingMethodOrLambda(this)
                    ?: throw IllegalArgumentException("Return statement is not inside a method or lambda")

                Context.Key.ReturnKey(
                    psiTypeToSetIndicator(((returnMethod as? PsiMethod)?.returnType)!!)
                )
            }

            else -> throw IllegalArgumentException("Unsupported PsiElement type: ${this::class} (${this.text})")
        }
    }

    fun findContainingMethodOrLambda(returnStatement: PsiReturnStatement): PsiElement? {
        var parent = returnStatement.parent
        while (parent != null) {
            when (parent) {
                is PsiMethod -> return parent // Found the containing method
                is PsiLambdaExpression -> return parent // Found the containing lambda
            }
            parent = parent.parent
        }
        return null // Not inside a method or lambda
    }

    fun PsiElement.resolveIfNeeded(): PsiElement {
        if (this is PsiReferenceExpression) {
            val startTime = System.currentTimeMillis()
            val resolved = this.resolve() ?: TODO(
                "Variable couldn't be resolved (${element.text})"
            )
            val duration = System.currentTimeMillis() - startTime
            if (duration > 10) {
                println("Slow resolution warning! ${this.element.text} took ${duration}ms")
            }
            return resolved
        }
        return this
    }

    /**
     * The length of the set of possible values of this type.
     */
    fun KClass<*>.getSetSize(): BigInteger {
        when (this) {
            Byte::class -> return BigInteger.valueOf(Byte.MAX_VALUE.toLong() - Byte.MIN_VALUE.toLong() + 1)
            Short::class -> return BigInteger.valueOf(Short.MAX_VALUE.toLong() - Short.MIN_VALUE.toLong() + 1)
            Int::class -> return BigInteger.valueOf(Int.MAX_VALUE.toLong() - Int.MIN_VALUE.toLong() + 1)
            Long::class -> return (BigInteger.valueOf(Long.MAX_VALUE) - BigInteger.valueOf(Long.MIN_VALUE)) + BigInteger.ONE
        }
        throw IllegalArgumentException("Unsupported type for set size $this")
    }

    fun KClass<*>.isFloatingPoint(): Boolean {
        return when (this) {
            Float::class, Double::class -> true
            else -> false
        }
    }

    fun Number.isOne(): Boolean {
        return when (this) {
            is Byte -> this == 1.toByte()
            is Short -> this == 1.toShort()
            is Int -> this == 1
            is Long -> this == 1L
            is Float -> this == 1.0f
            is Double -> this == 1.0
            else -> throw IllegalArgumentException("Unsupported type for isOne")
        }
    }

    fun Number.isZero(): Boolean {
        return when (this) {
            is Byte -> this == 0.toByte()
            is Short -> this == 0.toShort()
            is Int -> this == 0
            is Long -> this == 0L
            is Float -> this == 0.0f
            is Double -> this == 0.0
            else -> throw IllegalArgumentException("Unsupported type for isZero")
        }
    }

    fun <T : Number> Number.castInto(target: KClass<*>): T {
        @Suppress("UNCHECKED_CAST")
        return when (target) {
            Byte::class -> this.toByte()
            Short::class -> this.toShort()
            Int::class -> this.toInt()
            Long::class -> this.toLong()
            Float::class -> this.toFloat()
            Double::class -> this.toDouble()
            else -> throw IllegalArgumentException("Unsupported type for cast")
        } as T
    }

    inline fun <R> R?.orElse(block: () -> R): R {
        return this ?: block()
    }

    operator fun <T : Number> T.compareTo(other: T): Int {
        if (this is BigInteger && other is BigInteger) {
            return this.compareTo(other)
        }

        if (this is BigInteger || other is BigInteger) {
            throw IllegalArgumentException("Cannot compare BigInteger with non-BigInteger for now. $this $other")
        }

        val thisIsWhole = this is Byte || this is Short || this is Int || this is Long
        val otherIsWhole = other is Byte || other is Short || other is Int || other is Long

        if (thisIsWhole && otherIsWhole) {
            return this.toLong().compareTo(other.toLong())
        }

        return if (thisIsWhole) {
            println("Slow comparison warning! $this $other")
            BigDecimal.valueOf(this.toLong()).compareTo(BigDecimal.valueOf(other.toDouble()))
        } else if (otherIsWhole) {
            println("Slow comparison warning! $this $other")
            BigDecimal.valueOf(this.toDouble()).compareTo(BigDecimal.valueOf(other.toLong()))
        } else {
            this.toDouble().compareTo(other.toDouble())
        }
    }

    operator fun <T : Number> T.plus(other: T): T {
        assert(this::class == other::class) {
            "Types don't match: $this (${this::class}) and $other (${other::class})"
        }

        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> (this + other.toByte()).toByte()
            is Short -> (this + other.toShort()).toShort()
            is Int -> this + other as Int
            is Long -> this + other as Long
            is Float -> this + other as Float
            is Double -> this + other as Double
            is BigInteger -> this.add(other as BigInteger)
            is BigFraction -> this.add(other as BigFraction)
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for addition")
        } as T // This cast shouldn't be necessary.
    }

    operator fun <T : Number> T.minus(other: T): T {
        assert(this::class == other::class) {
            "Types don't match: $this (${this::class}) and $other (${other::class})"
        }

        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> (this - other.toByte()).toByte()
            is Short -> (this - other.toShort()).toShort()
            is Int -> this - other as Int
            is Long -> this - other as Long
            is Float -> this - other as Float
            is Double -> this - other as Double
            is BigInteger -> this.subtract(other as BigInteger)
            is BigFraction -> this.subtract(other as BigFraction)
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for subtraction")
        } as T // This cast shouldn't be necessary.
    }

    operator fun <T : Number> T.times(other: T): T {
        assert(this::class == other::class) {
            "Types don't match: $this (${this::class}) and $other (${other::class})"
        }

        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> (this.toByte() * other.toByte()).toByte()
            is Short -> (this.toShort() * other.toShort()).toShort()
            is Int -> this * other as Int
            is Long -> this * other as Long
            is Float -> this * other as Float
            is Double -> this * other as Double
            is BigInteger -> this.multiply(other as BigInteger)
            is BigFraction -> this.multiply(other as BigFraction)
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for multiplication")
        } as T // This cast shouldn't be necessary.
    }

    operator fun <T : Number> T.div(other: T): T {
        assert(this::class == other::class) {
            "Types don't match: $this (${this::class}) and $other (${other::class})"
        }

        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> (this.toByte() / other.toByte()).toByte()
            is Short -> (this.toShort() / other.toShort()).toShort()
            is Int -> this / other as Int
            is Long -> this / other as Long
            is Float -> this / other as Float
            is Double -> this / other as Double
            is BigInteger -> this.divide(other as BigInteger)
            is BigFraction -> this.divide(other as BigFraction)
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for division")
        } as T // This cast shouldn't be necessary.
    }

    fun <T : Number> max(vararg values: T?): T? {
        if (values.isEmpty()) return null
        var maxValue: T = values[0] ?: return null

        for (value in values) {
            if (value != null && value > maxValue) {
                maxValue = value
            }
        }
        return maxValue
    }

    fun <T : Number> min(vararg values: T?): T? {
        if (values.isEmpty()) return null
        var minValue: T = values[0] ?: return null

        for (value in values) {
            if (value != null && value < minValue) {
                minValue = value
            }
        }
        return minValue
    }

    // Min and max extension functions for Number
    fun <T : Number> T.min(other: T): T {
        assert(this::class == other::class) {
            "Types don't match: $this (${this::class}) and $other (${other::class})"
        }

        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> if (this < other.toByte()) this else other.toByte()
            is Short -> if (this < other.toShort()) this else other.toShort()
            is Int -> if (this < other.toInt()) this else other.toInt()
            is Long -> if (this < other.toLong()) this else other.toLong()
            is Float -> if (this < other.toFloat()) this else other.toFloat()
            is Double -> if (this < other.toDouble()) this else other.toDouble()
            is BigFraction -> if (this < other as BigFraction) this else other as BigFraction
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for min")
        } as T
    }

    fun <T : Number> T.max(other: T): T {
        assert(this::class == other::class) {
            "Types don't match: $this (${this::class}) and $other (${other::class})"
        }

        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> if (this > other.toByte()) this else other.toByte()
            is Short -> if (this > other.toShort()) this else other.toShort()
            is Int -> if (this > other.toInt()) this else other.toInt()
            is Long -> if (this > other.toLong()) this else other.toLong()
            is Float -> if (this > other.toFloat()) this else other.toFloat()
            is Double -> if (this > other.toDouble()) this else other.toDouble()
            is BigFraction -> if (this > other as BigFraction) this else other as BigFraction
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for max")
        } as T
    }

    /**
     * Goes down the smallest possible increment from the given number to the next.
     * Clamps if it's already at the minimum.
     */
    fun <T : Number> T.downOneEpsilon(): T {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> if (this > Byte.MIN_VALUE) (this - 1).toByte() else Byte.MIN_VALUE
            is Short -> if (this > Short.MIN_VALUE) (this - 1).toShort() else Short.MIN_VALUE
            is Int -> if (this > Int.MIN_VALUE) this - 1 else Int.MIN_VALUE
            is Long -> if (this > Long.MIN_VALUE) this - 1 else Long.MIN_VALUE
            is Float -> this.nextDown()
            is Double -> this.nextDown()
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for downOneEpsilon")
        } as T
    }

    /**
     * Goes up the smallest possible increment from the given number to the next.
     */
    fun <T : Number> T.upOneEpsilon(): T {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> if (this < Byte.MAX_VALUE) (this + 1).toByte() else Byte.MAX_VALUE
            is Short -> if (this < Short.MAX_VALUE) (this + 1).toShort() else Short.MAX_VALUE
            is Int -> if (this < Int.MAX_VALUE) this + 1 else Int.MAX_VALUE
            is Long -> if (this < Long.MAX_VALUE) this + 1 else Long.MAX_VALUE
            is Float -> this.nextUp()
            is Double -> this.nextUp()
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for upOneEpsilon")
        } as T
    }

    fun <T : Number> T.negate(): T {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is Byte -> (-this).toByte()
            is Short -> (-this).toShort()
            is Int -> -this
            is Long -> -this
            is Float -> -this
            is Double -> -this
            else -> throw IllegalArgumentException("Unsupported type (${this::class}) for negation")
        } as T
    }

    fun <T : Number> Pair<T, T>.intersect(other: Pair<T, T>): Pair<T, T>? {
        val start = this.first.max(other.first)
        val end = this.second.min(other.second)
        return if (start <= end) Pair(start, end) else null
    }

    fun <LOld : Any, LNew : Any, R : Any> Pair<LOld, R>.mapLeft(transform: (LOld) -> LNew): Pair<LNew, R> =
        Pair(transform(this.first), this.second)

    fun <L : Any, ROld : Any, RNew : Any> Pair<L, ROld>.mapRight(transform: (ROld) -> RNew): Pair<L, RNew> =
        Pair(this.first, transform(this.second))

    fun BigFraction.half(): BigFraction = this.divide(2)
}