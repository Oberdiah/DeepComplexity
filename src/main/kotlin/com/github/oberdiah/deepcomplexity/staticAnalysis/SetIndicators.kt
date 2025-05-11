package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.BooleanBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.GenericBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.NumberBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.github.oberdiah.deepcomplexity.utilities.Utilities.castInto
import java.math.BigInteger
import kotlin.reflect.KClass

sealed class SetIndicator<T : Any>(val clazz: KClass<T>) {
    /**
     * Instantiate a bundle with no valid values.
     */
    abstract fun newEmptyBundle(): Bundle<T>

    /**
     * Instantiate a bundle that says 'my value is always equal to this constant'
     */
    abstract fun newConstantBundle(constant: T): Bundle<T>

    /**
     * Instantiate a bundle representing a full set of values.
     */
    abstract fun newFullBundle(): Bundle<T>

    abstract fun newVariance(key: Context.Key): Variances<T>

    companion object {
        fun <T : Any> getSetIndicator(expr: IExpr<T>): SetIndicator<T> {
            @Suppress("UNCHECKED_CAST")
            return when (expr) {
                is IfExpression -> expr.trueExpr.ind
                is UnionExpression -> expr.lhs.ind
                is ArithmeticExpression -> expr.lhs.ind
                is NegateExpression -> expr.expr.ind
                is NumIterationTimesExpression -> expr.ind
                is BooleanExpression -> BooleanSetIndicator
                is BooleanInvertExpression -> BooleanSetIndicator
                is ComparisonExpression<*> -> BooleanSetIndicator
                is ConstExpr -> expr.constSet.ind
                is VariableExpression -> expr.setInd
                is TypeCastExpression<*, *> -> expr.setInd
            } as SetIndicator<T>
        }

        fun fromClass(clazz: KClass<*>): SetIndicator<*> {
            return when (clazz) {
                Double::class -> DoubleSetIndicator
                Float::class -> FloatSetIndicator
                Int::class -> IntSetIndicator
                Long::class -> LongSetIndicator
                Short::class -> ShortSetIndicator
                Byte::class -> ByteSetIndicator
                Char::class -> TODO()
                Boolean::class -> BooleanSetIndicator
                else -> GenericSetIndicator(clazz)
            }
        }

        fun <T : Any> fromValue(value: T): SetIndicator<T> {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is Number -> fromValue(value)
                is Boolean -> BooleanSetIndicator
                is String -> GenericSetIndicator(String::class)
                else -> GenericSetIndicator(value::class)
            } as SetIndicator<T>
        }

        fun <T : Number> fromValue(value: T): NumberSetIndicator<T> {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is Double -> DoubleSetIndicator
                is Float -> FloatSetIndicator
                is Int -> IntSetIndicator
                is Long -> LongSetIndicator
                is Short -> ShortSetIndicator
                is Byte -> ByteSetIndicator
                else -> TODO()
            } as NumberSetIndicator<T>
        }
    }
}

sealed class NumberSetIndicator<T : Number>(clazz: KClass<T>) : SetIndicator<T>(clazz) {
    override fun newFullBundle(): NumberBundle<T> = NumberBundle.newFull(this)
    override fun newConstantBundle(constant: T): NumberBundle<T> = NumberBundle.newFromConstant(constant)
    override fun newEmptyBundle(): NumberBundle<T> = NumberBundle(this, emptyList())
    override fun newVariance(key: Context.Key): Variances<T> = NumberVariances.newFromVariance(this, key)

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
        return this is IntSetIndicator
                || this is LongSetIndicator
                || this is ShortSetIndicator
                || this is ByteSetIndicator
    }

    fun onlyZeroSet(): NumberBundle<T> = newConstantBundle(getZero())
    fun onlyOneSet(): NumberBundle<T> = newConstantBundle(getOne())
    fun allPositiveNumbers(): NumberBundle<T> = onlyZeroSet().getSetSatisfying(ComparisonOp.GREATER_THAN_OR_EQUAL)
    fun allNegativeNumbers(): NumberBundle<T> = onlyZeroSet().getSetSatisfying(ComparisonOp.LESS_THAN_OR_EQUAL)
    fun getZero(): T = getInt(0)
    fun getOne(): T = getInt(1)

    fun castToMe(v: Number): T = v.castInto(clazz)
}

data object DoubleSetIndicator : NumberSetIndicator<Double>(Double::class) {
    override fun getMaxValue(): Double = Double.MAX_VALUE
    override fun getMinValue(): Double = Double.MIN_VALUE
    override fun getInt(int: Int): Double = int.toDouble()
}

data object FloatSetIndicator : NumberSetIndicator<Float>(Float::class) {
    override fun getMaxValue(): Float = Float.MAX_VALUE
    override fun getMinValue(): Float = Float.MIN_VALUE
    override fun getInt(int: Int): Float = int.toFloat()
}

data object IntSetIndicator : NumberSetIndicator<Int>(Int::class) {
    override fun getMaxValue(): Int = Int.MAX_VALUE
    override fun getMinValue(): Int = Int.MIN_VALUE
    override fun getInt(int: Int): Int = int
}

object LongSetIndicator : NumberSetIndicator<Long>(Long::class) {
    override fun getMaxValue(): Long = Long.MAX_VALUE
    override fun getMinValue(): Long = Long.MIN_VALUE
    override fun getInt(int: Int): Long = int.toLong()
}

data object ShortSetIndicator : NumberSetIndicator<Short>(Short::class) {
    override fun getMaxValue(): Short = Short.MAX_VALUE
    override fun getMinValue(): Short = Short.MIN_VALUE
    override fun getInt(int: Int): Short = int.toShort()
}

data object ByteSetIndicator : NumberSetIndicator<Byte>(Byte::class) {
    override fun getMaxValue(): Byte = Byte.MAX_VALUE
    override fun getMinValue(): Byte = Byte.MIN_VALUE
    override fun getInt(int: Int): Byte = int.toByte()
}

data object BooleanSetIndicator : SetIndicator<Boolean>(Boolean::class) {
    override fun newVariance(key: Context.Key): Variances<Boolean> = BooleanVariances(BooleanBundle.BOTH)
    override fun newFullBundle(): Bundle<Boolean> = BooleanBundle.BOTH
    override fun newEmptyBundle(): BooleanBundle = BooleanBundle.NEITHER
    override fun newConstantBundle(constant: Boolean): Bundle<Boolean> =
        BooleanBundle.fromBoolean(constant)
}

class GenericSetIndicator<T : Any>(clazz: KClass<T>) : SetIndicator<T>(clazz) {
    override fun newVariance(key: Context.Key): Variances<T> = TODO()
    override fun newConstantBundle(constant: T): Bundle<T> = GenericBundle(setOf(constant))
    override fun newEmptyBundle(): Bundle<T> = GenericBundle(emptySet())
    override fun newFullBundle(): Bundle<T> = TODO()
}