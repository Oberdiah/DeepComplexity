package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.weisj.jsvg.T
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import org.apache.commons.numbers.core.DD
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

    fun PsiElement.resolveIfNeeded(): PsiElement {
        if (this is PsiReferenceExpression) {
            return this.resolve() ?: TODO(
                "Variable couldn't be resolved (${element.text})"
            )
        }
        return this
    }

    fun KClass<*>.getMaxValue(): Number {
        return when (this) {
            Byte::class -> Byte.MAX_VALUE
            Short::class -> Short.MAX_VALUE
            Int::class -> Int.MAX_VALUE
            Long::class -> Long.MAX_VALUE
            Float::class -> Float.MAX_VALUE
            Double::class -> Double.MAX_VALUE
            else -> throw IllegalArgumentException("Unsupported type for max value (got $this)")
        }
    }

    fun KClass<*>.getMinValue(): Number {
        return when (this) {
            Byte::class -> Byte.MIN_VALUE
            Short::class -> Short.MIN_VALUE
            Int::class -> Int.MIN_VALUE
            Long::class -> Long.MIN_VALUE
            Float::class -> Float.MIN_VALUE
            Double::class -> Double.MIN_VALUE
            else -> throw IllegalArgumentException("Unsupported type for min value (got $this)")
        }
    }

    fun KClass<*>.getZero(): Number {
        return getInt(0)
    }

    fun KClass<*>.getOne(): Number {
        return getInt(1)
    }

    fun KClass<*>.getInt(int: Int): Number {
        return when (this) {
            Byte::class -> int.toByte()
            Short::class -> int.toShort()
            Int::class -> int
            Long::class -> int.toLong()
            Float::class -> int.toFloat()
            Double::class -> int.toDouble()
            else -> throw IllegalArgumentException("Unsupported type for int value")
        }
    }

    /**
     * The length of the set of possible values of this type.
     */
    fun KClass<*>.getSetSize(): BigInteger {
        when (this) {
            Byte::class -> return BigInteger.valueOf(Byte.MAX_VALUE.toLong() - Byte.MIN_VALUE.toLong())
            Short::class -> return BigInteger.valueOf(Short.MAX_VALUE.toLong() - Short.MIN_VALUE.toLong())
            Int::class -> return BigInteger.valueOf(Int.MAX_VALUE.toLong() - Int.MIN_VALUE.toLong())
            Long::class -> return BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.valueOf(Long.MIN_VALUE))
        }
        throw IllegalArgumentException("Unsupported type for zero value")
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
        return when (this) {
            is Byte -> this.toInt().compareTo(other.toInt())
            is Short -> this.toInt().compareTo(other.toInt())
            is Int -> this.compareTo(other as Int)
            is Long -> this.compareTo(other as Long)
            is Float -> this.compareTo(other as Float)
            is Double -> this.compareTo(other as Double)
            else -> throw IllegalArgumentException("Unsupported type for comparison")
        }
    }

    operator fun <T : Number> T.plus(other: T): T {
        return when (this) {
            is Byte -> (this + other.toByte()).toByte()
            is Short -> (this + other.toShort()).toShort()
            is Int -> this + other as Int
            is Long -> this + other as Long
            is Float -> this + other as Float
            is Double -> this + other as Double
            else -> throw IllegalArgumentException("Unsupported type for addition")
        } as T // This cast shouldn't be necessary.
    }

    operator fun <T : Number> T.minus(other: T): T {
        return when (this) {
            is Byte -> (this - other.toByte()).toByte()
            is Short -> (this - other.toShort()).toShort()
            is Int -> this - other as Int
            is Long -> this - other as Long
            is Float -> this - other as Float
            is Double -> this - other as Double
            else -> throw IllegalArgumentException("Unsupported type for subtraction")
        } as T // This cast shouldn't be necessary.
    }

    operator fun <T : Number> T.times(other: T): T {
        return when (this) {
            is Byte -> (this.toByte() * other.toByte()).toByte()
            is Short -> (this.toShort() * other.toShort()).toShort()
            is Int -> this * other as Int
            is Long -> this * other as Long
            is Float -> this * other as Float
            is Double -> this * other as Double
            else -> throw IllegalArgumentException("Unsupported type for multiplication")
        } as T // This cast shouldn't be necessary.
    }

    operator fun <T : Number> T.div(other: T): T {
        return when (this) {
            is Byte -> (this.toByte() / other.toByte()).toByte()
            is Short -> (this.toShort() / other.toShort()).toShort()
            is Int -> this / other as Int
            is Long -> this / other as Long
            is Float -> this / other as Float
            is Double -> this / other as Double
            else -> throw IllegalArgumentException("Unsupported type for division")
        } as T // This cast shouldn't be necessary.
    }

    // Min and max extension functions for Number
    fun <T : Number> T.min(other: T): T {
        return when (this) {
            is Byte -> if (this < other.toByte()) this else other.toByte()
            is Short -> if (this < other.toShort()) this else other.toShort()
            is Int -> if (this < other.toInt()) this else other.toInt()
            is Long -> if (this < other.toLong()) this else other.toLong()
            is Float -> if (this < other.toFloat()) this else other.toFloat()
            is Double -> if (this < other.toDouble()) this else other.toDouble()
            else -> throw IllegalArgumentException("Unsupported type for min")
        } as T
    }

    fun <T : Number> T.max(other: T): T {
        return when (this) {
            is Byte -> if (this > other.toByte()) this else other.toByte()
            is Short -> if (this > other.toShort()) this else other.toShort()
            is Int -> if (this > other.toInt()) this else other.toInt()
            is Long -> if (this > other.toLong()) this else other.toLong()
            is Float -> if (this > other.toFloat()) this else other.toFloat()
            is Double -> if (this > other.toDouble()) this else other.toDouble()
            else -> throw IllegalArgumentException("Unsupported type for max")
        } as T
    }

    /**
     * Goes down the smallest possible increment from the given number to the next.
     * Clamps if it's already at the minimum.
     */
    fun <T : Number> T.downOneEpsilon(): T {
        return when (this) {
            is Byte -> if (this > Byte.MIN_VALUE) (this - 1).toByte() else Byte.MIN_VALUE
            is Short -> if (this > Short.MIN_VALUE) (this - 1).toShort() else Short.MIN_VALUE
            is Int -> if (this > Int.MIN_VALUE) this - 1 else Int.MIN_VALUE
            is Long -> if (this > Long.MIN_VALUE) this - 1 else Long.MIN_VALUE
            is Float -> this.nextDown()
            is Double -> this.nextDown()
            else -> throw IllegalArgumentException("Unsupported type for downOneEpsilon")
        } as T
    }

    /**
     * Goes up the smallest possible increment from the given number to the next.
     */
    fun <T : Number> T.upOneEpsilon(): T {
        return when (this) {
            is Byte -> if (this < Byte.MAX_VALUE) (this + 1).toByte() else Byte.MAX_VALUE
            is Short -> if (this < Short.MAX_VALUE) (this + 1).toShort() else Short.MAX_VALUE
            is Int -> if (this < Int.MAX_VALUE) this + 1 else Int.MAX_VALUE
            is Long -> if (this < Long.MAX_VALUE) this + 1 else Long.MAX_VALUE
            is Float -> this.nextUp()
            is Double -> this.nextUp()
            else -> throw IllegalArgumentException("Unsupported type for upOneEpsilon")
        } as T
    }

    fun <T : Number> T.negate(): T {
        return when (this) {
            is Byte -> (-this).toByte()
            is Short -> (-this).toShort()
            is Int -> -this
            is Long -> -this
            is Float -> -this
            is Double -> -this
            else -> throw IllegalArgumentException("Unsupported type for negation")
        } as T
    }
}