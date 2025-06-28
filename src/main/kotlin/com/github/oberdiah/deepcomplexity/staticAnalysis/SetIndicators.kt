package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
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
    abstract fun newEmptySet(): ISet<T>

    /**
     * Instantiate a bundle that says 'my value is always equal to this constant'
     */
    abstract fun newConstantSet(constant: T): ISet<T>

    /**
     * Instantiate a bundle representing a full set of values.
     */
    abstract fun newFullSet(): ISet<T>

    abstract fun newVariance(key: Context.Key): Variances<T>

    companion object {
        fun <T : Any> getSetIndicator(expr: Expr<T>): SetIndicator<T> {
            @Suppress("UNCHECKED_CAST")
            return when (expr) {
                is VoidExpression -> throw IllegalStateException("Void expression has no set indicator")
                is IfExpression -> expr.trueExpr.ind
                is UnionExpression -> expr.lhs.ind
                is ArithmeticExpression -> expr.lhs.ind
                is NegateExpression -> expr.expr.ind
                is NumIterationTimesExpression -> expr.ind
                is BooleanExpression -> BooleanSetIndicator
                is BooleanInvertExpression -> BooleanSetIndicator
                is ComparisonExpression<*> -> BooleanSetIndicator
                is ConstExpr -> expr.constSet.ind
                is VariableExpression -> expr.key.ind
                is TypeCastExpression<*, *> -> expr.setInd
                // This is presumably wrong.
                is ClassExpr -> GenericSetIndicator(Any::class)
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
    override fun newFullSet(): NumberSet<T> = NumberSet.newFull(this)
    override fun newEmptySet(): NumberSet<T> = NumberSet.newEmpty(this)
    override fun newConstantSet(constant: T): NumberSet<T> = NumberSet.newFromConstant(constant)
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

    fun onlyZeroSet(): NumberSet<T> = newConstantSet(getZero())
    fun onlyOneSet(): NumberSet<T> = newConstantSet(getOne())
    fun positiveNumbersAndZero(): NumberSet<T> = onlyZeroSet().getSetSatisfying(ComparisonOp.GREATER_THAN_OR_EQUAL)
    fun negativeNumbersAndZero(): NumberSet<T> = onlyZeroSet().getSetSatisfying(ComparisonOp.LESS_THAN_OR_EQUAL)
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

data object LongSetIndicator : NumberSetIndicator<Long>(Long::class) {
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
    override fun newVariance(key: Context.Key): Variances<Boolean> = BooleanVariances(BooleanSet.BOTH)
    override fun newFullSet(): ISet<Boolean> = BooleanSet.BOTH
    override fun newEmptySet(): BooleanSet = BooleanSet.NEITHER
    override fun newConstantSet(constant: Boolean): ISet<Boolean> =
        BooleanSet.fromBoolean(constant)
}

class GenericSetIndicator<T : Any>(clazz: KClass<T>) : SetIndicator<T>(clazz) {
    override fun newVariance(key: Context.Key): Variances<T> = TODO()
    override fun newConstantSet(constant: T): ISet<T> = GenericSet(setOf(constant))
    override fun newEmptySet(): ISet<T> = GenericSet(emptySet())
    override fun newFullSet(): ISet<T> = TODO()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenericSetIndicator<*>) return false

        if (clazz != other.clazz) return false

        return true
    }

    override fun hashCode(): Int {
        return clazz.hashCode()
    }
}