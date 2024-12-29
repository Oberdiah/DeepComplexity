package com.github.oberdiah.deepcomplexity.staticAnalysis

import org.apache.commons.numbers.core.DD
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

    fun KClass<*>.getMaxValue(): DD {
        when (this) {
            Byte::class -> return DD.of(Byte.MAX_VALUE.toInt())
            Short::class -> return DD.of(Short.MAX_VALUE.toInt())
            Int::class -> return DD.of(Int.MAX_VALUE)
            Long::class -> return DD.of(Long.MAX_VALUE)
            Float::class -> return DD.of(Float.MAX_VALUE.toDouble())
            Double::class -> return DD.of(Double.MAX_VALUE)
        }
        throw IllegalArgumentException("Unsupported type for max value")
    }

    fun KClass<*>.getMinValue(): DD {
        when (this) {
            Byte::class -> return DD.of(Byte.MIN_VALUE.toInt())
            Short::class -> return DD.of(Short.MIN_VALUE.toInt())
            Int::class -> return DD.of(Int.MIN_VALUE)
            Long::class -> return DD.of(Long.MIN_VALUE)
            Float::class -> return DD.of(Float.MIN_VALUE.toDouble())
            Double::class -> return DD.of(Double.MIN_VALUE)
        }
        throw IllegalArgumentException("Unsupported type for min value")
    }

    /**
     * The length of the set of possible values of this type.
     */
    fun KClass<*>.getSetSize(): DD {
        when (this) {
            Byte::class -> return DD.of(Byte.MAX_VALUE.toInt() - Byte.MIN_VALUE.toInt())
            Short::class -> return DD.of(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt())
            Int::class -> return DD.of(Int.MAX_VALUE).subtract(DD.of(Int.MIN_VALUE))
            // This is a bit suspicious — might be exceeding the DD precision threshold,
            // I've not checked.
            Long::class -> return DD.of(Long.MAX_VALUE).subtract(DD.of(Long.MIN_VALUE))
        }
        throw IllegalArgumentException("Unsupported type for zero value")
    }
}