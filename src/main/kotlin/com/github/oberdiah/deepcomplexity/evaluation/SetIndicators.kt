package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.ByteSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.DoubleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.FloatSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.IntSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.LongSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.ShortSet
import kotlin.reflect.KClass

sealed interface SetIndicator<Set : IMoldableSet<Set>> {
    val clazz: KClass<*>

    /**
     * It's silly because it's so abundantly clear that the cast is safe when used in the intended way
     * (i.e. when other is the same as this).
     */
    fun <Set1 : IMoldableSet<Set1>> sillyCast(other: SetIndicator<Set1>, c: Set): Set1 {
        assert(other == this)
        @Suppress("UNCHECKED_CAST")
        return c as Set1
    }

    companion object {
        fun <T : IMoldableSet<T>> getSetIndicator(expr: IExpr<T>): SetIndicator<T> {
            @Suppress("UNCHECKED_CAST")
            return when (expr) {
                is IfExpression -> expr.trueExpr.getSetIndicator()
                is IntersectExpression -> expr.lhs.getSetIndicator()
                is UnionExpression -> expr.lhs.getSetIndicator()
                is InvertExpression -> expr.expr.getSetIndicator()

                is ArithmeticExpression -> expr.lhs.getSetIndicator()
                is NegateExpression -> expr.expr.getSetIndicator()
                is NumIterationTimesExpression -> expr.getSetIndicator()
                is NumberLimitsExpression -> expr.limit.getSetIndicator()

                is BooleanExpression -> BooleanSetIndicator
                is BooleanInvertExpression -> BooleanSetIndicator
                is ComparisonExpression<*> -> BooleanSetIndicator

                is ConstExpr -> expr.singleElementSet.getSetIndicator()
                is VariableExpression -> expr.setInd
                is TypeCastExpression -> expr.setInd
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
                Boolean::class -> BooleanSetIndicator
                else -> GenericSetIndicator
            }
        }

        fun <T : Any, Self : IMoldableSet<Self>> fromValue(value: T): SetIndicatorImpl<T, Self> {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is Number -> fromValue(value)
                is Boolean -> BooleanSetIndicator
                is String -> GenericSetIndicator
                else -> GenericSetIndicator
            } as SetIndicatorImpl<T, Self>
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> fromValue(value: T): SetIndicatorImpl<T, Self> {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is Double -> DoubleSetIndicator
                is Float -> FloatSetIndicator
                is Int -> IntSetIndicator
                is Long -> LongSetIndicator
                is Short -> ShortSetIndicator
                is Byte -> ByteSetIndicator
                else -> throw IllegalArgumentException("Unsupported number type: ${value::class}")
            } as SetIndicatorImpl<T, Self>
        }
    }
}

sealed class SetIndicatorImpl<T : Any, Set : IMoldableSet<Set>>(override val clazz: KClass<T>) : SetIndicator<Set>

sealed class NumberSetIndicator<T : Number, Set : NumberSetImpl<T, Set>>(clazz: KClass<T>) :
    SetIndicatorImpl<T, Set>(clazz) {

    /**
     * It's silly because it's so abundantly clear that the cast is safe when used in the intended way
     * (i.e. when other is the same as this).
     */
    fun <T1 : Number, Set1 : NumberSetImpl<T1, Set1>> sillyCast(other: NumberSetIndicator<T1, Set1>, c: T): T1 {
        assert(other == this)
        @Suppress("UNCHECKED_CAST")
        return c as T1
    }

    fun getMaxValue(): T {
        return when (this) {
            is ByteSetIndicator -> this.sillyCast(this, Byte.MAX_VALUE)
            is ShortSetIndicator -> this.sillyCast(this, Short.MAX_VALUE)
            is IntSetIndicator -> this.sillyCast(this, Int.MAX_VALUE)
            is LongSetIndicator -> this.sillyCast(this, Long.MAX_VALUE)
            is FloatSetIndicator -> this.sillyCast(this, Float.MAX_VALUE)
            is DoubleSetIndicator -> this.sillyCast(this, Double.MAX_VALUE)
        }
    }

    fun getMinValue(): T {
        return when (this) {
            is ByteSetIndicator -> this.sillyCast(this, Byte.MIN_VALUE)
            is ShortSetIndicator -> this.sillyCast(this, Short.MIN_VALUE)
            is IntSetIndicator -> this.sillyCast(this, Int.MIN_VALUE)
            is LongSetIndicator -> this.sillyCast(this, Long.MIN_VALUE)
            is FloatSetIndicator -> this.sillyCast(this, Float.MIN_VALUE)
            is DoubleSetIndicator -> this.sillyCast(this, Double.MIN_VALUE)
        }
    }

    fun getZero(): T {
        return getInt(0)
    }

    fun getOne(): T {
        return getInt(1)
    }

    fun getInt(int: Int): T {
        return when (this) {
            is ByteSetIndicator -> sillyCast(this, int.toByte())
            is ShortSetIndicator -> sillyCast(this, int.toShort())
            is IntSetIndicator -> sillyCast(this, int)
            is LongSetIndicator -> sillyCast(this, int.toLong())
            is FloatSetIndicator -> sillyCast(this, int.toFloat())
            is DoubleSetIndicator -> sillyCast(this, int.toDouble())
        }
    }
}

sealed interface Foo<T : Any, F : Any>

data object DoubleSetIndicator : NumberSetIndicator<Double, DoubleSet>(Double::class), Foo<Double, Double>
data object FloatSetIndicator : NumberSetIndicator<Float, FloatSet>(Float::class), Foo<Int, Int>
data object IntSetIndicator : NumberSetIndicator<Int, IntSet>(Int::class)
data object LongSetIndicator : NumberSetIndicator<Long, LongSet>(Long::class)
data object ShortSetIndicator : NumberSetIndicator<Short, ShortSet>(Short::class)
data object ByteSetIndicator : NumberSetIndicator<Byte, ByteSet>(Byte::class)

data object BooleanSetIndicator : SetIndicatorImpl<Boolean, BooleanSet>(Boolean::class)
data object GenericSetIndicator : SetIndicatorImpl<Any, GenericSet>(Any::class)