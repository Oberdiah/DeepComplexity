package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.FullyTypedNumberSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

sealed interface SetIndicator<Set : IMoldableSet<Set>> {
    val clazz: KClass<*>

    fun newEmptySet(): Set
    fun newFullSet(key: Context.Key): Set

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

        fun <T : IMoldableSet<T>> newEmptySet(ind: SetIndicator<T>): T = ind.newEmptySet()
        fun <T : Any, Self : IMoldableSet<Self>> newEmptySetFromValue(v: T): Self = newEmptySet(fromValue(v))

        fun <T : Any, Self : IMoldableSet<Self>> fromValue(value: T): SetIndicatorImpl<T, Self> {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is Number -> fromValue(value)
                is Boolean -> BooleanSetIndicator
                is String -> GenericSetIndicator
                else -> GenericSetIndicator
            } as SetIndicatorImpl<T, Self>
        }

        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> fromValue(value: T): SetIndicatorImpl<T, Self> {
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

sealed class NumberSetIndicator<T : Number, Set : FullyTypedNumberSet<T, Set>>(clazz: KClass<T>) :
    SetIndicatorImpl<T, Set>(clazz) {

    override fun newFullSet(key: Context.Key): Set = NumberSet.fullRange(this, key)
    abstract fun getMaxValue(): T
    abstract fun getMinValue(): T
    abstract fun getInt(int: Int): T

    fun getZero(): T {
        return getInt(0)
    }

    fun getOne(): T {
        return getInt(1)
    }
}

data object DoubleSetIndicator : NumberSetIndicator<Double, DoubleSet>(Double::class) {
    override fun newEmptySet(): DoubleSet = DoubleSet()
    override fun getMaxValue(): Double = Double.MAX_VALUE
    override fun getMinValue(): Double = Double.MIN_VALUE
    override fun getInt(int: Int): Double = int.toDouble()
}

data object FloatSetIndicator : NumberSetIndicator<Float, FloatSet>(Float::class) {
    override fun newEmptySet(): FloatSet = FloatSet()
    override fun getMaxValue(): Float = Float.MAX_VALUE
    override fun getMinValue(): Float = Float.MIN_VALUE
    override fun getInt(int: Int): Float = int.toFloat()
}

data object IntSetIndicator : NumberSetIndicator<Int, IntSet>(Int::class) {
    override fun newEmptySet(): IntSet = IntSet()
    override fun getMaxValue(): Int = Int.MAX_VALUE
    override fun getMinValue(): Int = Int.MIN_VALUE
    override fun getInt(int: Int): Int = int
}

data object LongSetIndicator : NumberSetIndicator<Long, LongSet>(Long::class) {
    override fun newEmptySet(): LongSet = LongSet()
    override fun getMaxValue(): Long = Long.MAX_VALUE
    override fun getMinValue(): Long = Long.MIN_VALUE
    override fun getInt(int: Int): Long = int.toLong()
}

data object ShortSetIndicator : NumberSetIndicator<Short, ShortSet>(Short::class) {
    override fun newEmptySet(): ShortSet = ShortSet()
    override fun getMaxValue(): Short = Short.MAX_VALUE
    override fun getMinValue(): Short = Short.MIN_VALUE
    override fun getInt(int: Int): Short = int.toShort()
}

data object ByteSetIndicator : NumberSetIndicator<Byte, ByteSet>(Byte::class) {
    override fun newEmptySet(): ByteSet = ByteSet()
    override fun getMaxValue(): Byte = Byte.MAX_VALUE
    override fun getMinValue(): Byte = Byte.MIN_VALUE
    override fun getInt(int: Int): Byte = int.toByte()
}

data object BooleanSetIndicator : SetIndicatorImpl<Boolean, BooleanSet>(Boolean::class) {
    override fun newEmptySet(): BooleanSet = BooleanSet.NEITHER
    override fun newFullSet(key: Context.Key): BooleanSet = BooleanSet.BOTH
}

data object GenericSetIndicator : SetIndicatorImpl<Any, GenericSet>(Any::class) {
    override fun newEmptySet(): GenericSet = GenericSet.empty()
    override fun newFullSet(key: Context.Key): GenericSet = GenericSet.everyValue()
}