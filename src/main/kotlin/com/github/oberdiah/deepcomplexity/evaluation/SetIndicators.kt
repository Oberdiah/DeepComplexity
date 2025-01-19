package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.ByteSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.DoubleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.FloatSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.IntSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.LongSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.NumberSetImpl.ShortSet
import kotlin.reflect.KClass

sealed interface SetIndicator<Self : IMoldableSet<Self>> {
    val clazz: KClass<*>

    companion object {
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

sealed class SetIndicatorImpl<T : Any, Self : IMoldableSet<Self>>(override val clazz: KClass<T>) : SetIndicator<Self>

sealed class NumberSetIndicator<T : Number, Self : NumberSetImpl<T, Self>>(clazz: KClass<T>) :
    SetIndicatorImpl<T, Self>(clazz)

sealed interface Foo<T : Any, F : Any>

data object DoubleSetIndicator : NumberSetIndicator<Double, DoubleSet>(Double::class), Foo<Double, Double>
data object FloatSetIndicator : NumberSetIndicator<Float, FloatSet>(Float::class), Foo<Int, Int>
data object IntSetIndicator : NumberSetIndicator<Int, IntSet>(Int::class)
data object LongSetIndicator : NumberSetIndicator<Long, LongSet>(Long::class)
data object ShortSetIndicator : NumberSetIndicator<Short, ShortSet>(Short::class)
data object ByteSetIndicator : NumberSetIndicator<Byte, ByteSet>(Byte::class)

data object BooleanSetIndicator : SetIndicatorImpl<Boolean, BooleanSet>(Boolean::class)
data object GenericSetIndicator : SetIndicatorImpl<Any, GenericSet>(Any::class)