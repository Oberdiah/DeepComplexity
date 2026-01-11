package com.oberdiah.deepcomplexity.staticAnalysis

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.evaluation.Key
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ObjectSet
import com.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.ObjectVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT
import com.oberdiah.deepcomplexity.utilities.Utilities.castInto
import com.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty
import java.math.BigInteger
import kotlin.reflect.KClass

/**
 * Can be useful when you want to cast two things to the same indicator<*>.
 */
fun <T : Any, S : Indicator<T>, Q> S.with(doIt: (S) -> Q) = doIt(this)

/**
 * An indicator for values that can be held in a set.
 */
sealed class Indicator<T : Any>(val clazz: KClass<T>) {
    /**
     * Instantiate a set with no valid values.
     */
    abstract fun newEmptySet(): ISet<T>

    /**
     * Instantiate a set that says 'my value is always equal to this constant'
     */
    abstract fun newConstantSet(constant: T): ISet<T>

    /**
     * Instantiate a set representing a full set of values.
     */
    abstract fun newFullSet(): ISet<T>

    abstract fun newVariance(key: Key): Variances<T>

    companion object {
        fun <T : Any> fromClass(clazz: KClass<T>): Indicator<T> {
            val ind = when (clazz) {
                Double::class -> DoubleIndicator
                Float::class -> FloatIndicator
                Int::class -> IntIndicator
                Long::class -> LongIndicator
                Short::class -> ShortIndicator
                Byte::class -> ByteIndicator
                Char::class -> TODO()
                Boolean::class -> BooleanIndicator
                VarsMarker::class -> VarsIndicator
                else -> TODO()
            }

            assert(ind.clazz == clazz) {
                "Mismatched class: ${ind.clazz} != $clazz"
            }
            // Safety: We checked the class matches.
            @Suppress("UNCHECKED_CAST")
            return ind as Indicator<T>
        }

        fun <T : Any> fromValue(value: T): Indicator<T> {
            val ind = when (value) {
                is Number -> NumberIndicator.fromValue(value)
                is Boolean -> BooleanIndicator
                is HeapMarker -> ObjectIndicator(value.type)
                else -> TODO()
            }

            assert(ind.clazz == value::class) {
                "Mismatched class: ${ind.clazz} != ${value::class}"
            }
            // Safety: We checked the class matches.
            @Suppress("UNCHECKED_CAST")
            return ind as Indicator<T>
        }
    }
}

sealed class NumberIndicator<T : Number>(clazz: KClass<T>) : Indicator<T>(clazz) {
    companion object {
        fun <T : Number> fromValue(value: T): NumberIndicator<T> {
            val ind = when (value) {
                is Double -> DoubleIndicator
                is Float -> FloatIndicator
                is Int -> IntIndicator
                is Long -> LongIndicator
                is Short -> ShortIndicator
                is Byte -> ByteIndicator
                else -> TODO("No NumberIndicator for ${value::class}")
            }

            assert(ind.clazz == value::class) {
                "Mismatched class: ${ind.clazz} != ${value::class}"
            }
            // Safety: We checked the class matches.
            @Suppress("UNCHECKED_CAST")
            return ind as NumberIndicator<T>
        }
    }

    override fun newFullSet(): NumberSet<T> = NumberSet.newFull(this)
    override fun newEmptySet(): NumberSet<T> = NumberSet.newEmpty(this)
    override fun newConstantSet(constant: T): NumberSet<T> = NumberSet.newFromConstant(constant)
    override fun newVariance(key: Key): Variances<T> = NumberVariances.newFromVariance(this, key)

    abstract fun getMaxValue(): T
    abstract fun getMinValue(): T

    abstract fun getInt(int: Int): T

    /**
     * Returns a string representation of the number, or the empty string
     * if the number is the minimum or maximum value. Useful for printing ranges.
     */
    fun stringify(i: T): String {
        return if (i == getMaxValue() || i == getMinValue()) {
            ""
        } else {
            i.toString()
        }
    }

    /**
     * Returns max(abs(minValue), abs(maxValue))
     */
    fun getRadius(): BigInteger {
        return maxOf(
            BigInteger.valueOf(getMaxValue().toLong()).abs(),
            BigInteger.valueOf(getMinValue().toLong()).abs()
        )
    }

    fun isWholeNum(): Boolean {
        return this is IntIndicator
                || this is LongIndicator
                || this is ShortIndicator
                || this is ByteIndicator
    }

    fun onlyZeroSet(): NumberSet<T> = newConstantSet(getZero())
    fun onlyOneSet(): NumberSet<T> = newConstantSet(getOne())
    fun positiveNumbersAndZero(): NumberSet<T> = onlyZeroSet().getSetSatisfying(ComparisonOp.GREATER_THAN_OR_EQUAL)
    fun negativeNumbersAndZero(): NumberSet<T> = onlyZeroSet().getSetSatisfying(ComparisonOp.LESS_THAN_OR_EQUAL)
    fun getZero(): T = getInt(0)
    fun getOne(): T = getInt(1)

    fun castToMe(v: Number): T = v.castInto(clazz)
}

data object DoubleIndicator : NumberIndicator<Double>(Double::class) {
    override fun getMaxValue(): Double = Double.MAX_VALUE
    override fun getMinValue(): Double = Double.MIN_VALUE
    override fun getInt(int: Int): Double = int.toDouble()
}

data object FloatIndicator : NumberIndicator<Float>(Float::class) {
    override fun getMaxValue(): Float = Float.MAX_VALUE
    override fun getMinValue(): Float = Float.MIN_VALUE
    override fun getInt(int: Int): Float = int.toFloat()
}

data object IntIndicator : NumberIndicator<Int>(Int::class) {
    override fun getMaxValue(): Int = Int.MAX_VALUE
    override fun getMinValue(): Int = Int.MIN_VALUE
    override fun getInt(int: Int): Int = int
}

data object LongIndicator : NumberIndicator<Long>(Long::class) {
    override fun getMaxValue(): Long = Long.MAX_VALUE
    override fun getMinValue(): Long = Long.MIN_VALUE
    override fun getInt(int: Int): Long = int.toLong()
}

data object ShortIndicator : NumberIndicator<Short>(Short::class) {
    override fun getMaxValue(): Short = Short.MAX_VALUE
    override fun getMinValue(): Short = Short.MIN_VALUE
    override fun getInt(int: Int): Short = int.toShort()
}

data object ByteIndicator : NumberIndicator<Byte>(Byte::class) {
    override fun getMaxValue(): Byte = Byte.MAX_VALUE
    override fun getMinValue(): Byte = Byte.MIN_VALUE
    override fun getInt(int: Int): Byte = int.toByte()
}

data object BooleanIndicator : Indicator<Boolean>(Boolean::class) {
    override fun newVariance(key: Key): Variances<Boolean> = BooleanVariances(BooleanSet.EITHER)
    override fun newFullSet(): ISet<Boolean> = BooleanSet.EITHER
    override fun newEmptySet(): BooleanSet = BooleanSet.NEITHER
    override fun newConstantSet(constant: Boolean): ISet<Boolean> =
        BooleanSet.fromBoolean(constant)
}

data class ObjectIndicator(val type: PsiType) : Indicator<HeapMarker>(HeapMarker::class) {
    override fun toString(): String = "ObjectIndicator(${type.toStringPretty()})"

    override fun newVariance(key: Key): Variances<HeapMarker> =
        ObjectVariances(ObjectSet.newEmptySet(this), this)

    override fun newConstantSet(constant: HeapMarker): ISet<HeapMarker> =
        ObjectSet.fromConstant(constant)

    override fun newEmptySet(): ISet<HeapMarker> = ObjectSet.newEmptySet(this)
    override fun newFullSet(): ISet<HeapMarker> = ObjectSet.newFullSet(this)
}

object VarsMarker
object VarsIndicator : Indicator<VarsMarker>(VarsMarker::class) {
    override fun toString(): String = "VarsIndicator"
    override fun newEmptySet(): ISet<VarsMarker> = WONT_IMPLEMENT()
    override fun newConstantSet(constant: VarsMarker): ISet<VarsMarker> = WONT_IMPLEMENT()
    override fun newFullSet(): ISet<VarsMarker> = WONT_IMPLEMENT()
    override fun newVariance(key: Key): Variances<VarsMarker> = WONT_IMPLEMENT()
}