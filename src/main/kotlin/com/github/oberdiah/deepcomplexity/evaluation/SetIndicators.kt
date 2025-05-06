package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.*
import java.math.BigInteger
import kotlin.reflect.KClass

sealed class SetIndicator<T : Any>(val clazz: KClass<T>) {
    /*
     To get things straight in my head, here's the situation:
     We have two different concepts here that are easy to get confused.
     1.  Constraints.
         These are used to combine variables and ensure we can correctly
         track variables that come from if statements.

         For example,
         ```
         if (a > 5) {
             x = 0
         } else {
             x = a
         }
         ```
         After this, we know that x = [a: {6..} -> {0}, a: {..5} -> {a}]
         Note that the constraints must be mutually exclusive from one another. We don't really
         verify this anywhere, but it should intuitively be true.

     2.  Variance origins.
         These are used to ensure that `x - y = 0` if `x = a` and `y = a`.

         There are no constraints here, only the ones implied by the bundle set.
         Numbers store this information in Affines, other types could do it some other way.

         It's tracking where our variance came from. This is optional for a bundle type implementation to do
         but can improve resolution.

     You can create new empty bundles (this can be no values), and it might be sometimes necessary to create an
     unknown variance where we just throw our hands up and say 'I have no idea what's going on'.
     A bundle with variance matching another variable is also a common need. For example, `a = b`.
     You can, of course, also create constant bundles.

     That should be all. So we've really just got three options:
      - Create an empty bundle
      - Create a constant bundle
      - Create a bundle with a variance origin (unknown or known)
    */

    /**
     * Instantiate a bundle with no valid values.
     */
    abstract fun newEmptyBundle(): Bundle<T>

    /**
     * Instantiate a bundle that says 'my value is always equal to this constant'
     */
    abstract fun newConstantBundle(constant: T): Bundle<T>

    /**
     * Instantiate a bundle that says 'my value is always equal to this variance source'.
     * This is effectively a 'full' bundle.
     *
     * It's important to note that any constraints on this bundle, even under the correct key,
     * will not constrain these values alone, and indeed they should not. A set needs to be able
     * to act correctly regardless of its constraint status, and tracking a variance source
     * for its entire life is not part of that remit.
     *
     * Set restrictions like that should originate from within; see NumberSet::getSetSatisfying
     * for an example.
     *
     * If you don't know where the variance came from (e.g., you've given up),
     * you can use an unknown key instead.
     */
    abstract fun newVarianceDefinedBundle(varianceSource: Context.Key): Bundle<T>

    companion object {
        fun <T : Any> getSetIndicator(expr: IExpr<T>): SetIndicator<T> {
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
    override fun newVarianceDefinedBundle(varianceSource: Context.Key): Bundle<T> {
        return NumberSet.newFromVariance(this, varianceSource)
    }

    override fun newConstantBundle(constant: T): Bundle<T> = NumberSet.newFromConstant(constant)

    abstract fun getMaxValue(): T
    abstract fun getMinValue(): T

    abstract fun getInt(int: Int): T

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

    fun getZero(): T {
        return getInt(0)
    }

    fun getOne(): T {
        return getInt(1)
    }
}

data object DoubleSetIndicator : NumberSetIndicator<Double>(Double::class) {
    override fun newEmptyBundle(): DoubleSet = DoubleSet()
    override fun getMaxValue(): Double = Double.MAX_VALUE
    override fun getMinValue(): Double = Double.MIN_VALUE
    override fun getInt(int: Int): Double = int.toDouble()
}

data object FloatSetIndicator : NumberSetIndicator<Float>(Float::class) {
    override fun newEmptyBundle(): FloatSet = FloatSet()
    override fun getMaxValue(): Float = Float.MAX_VALUE
    override fun getMinValue(): Float = Float.MIN_VALUE
    override fun getInt(int: Int): Float = int.toFloat()
}

data object IntSetIndicator : NumberSetIndicator<Int>(Int::class) {
    override fun newEmptyBundle(): IntSet = IntSet()
    override fun getMaxValue(): Int = Int.MAX_VALUE
    override fun getMinValue(): Int = Int.MIN_VALUE
    override fun getInt(int: Int): Int = int
}

object LongSetIndicator : NumberSetIndicator<Long>(Long::class) {
    override fun newEmptyBundle(): LongSet = LongSet()
    override fun getMaxValue(): Long = Long.MAX_VALUE
    override fun getMinValue(): Long = Long.MIN_VALUE
    override fun getInt(int: Int): Long = int.toLong()
}

data object ShortSetIndicator : NumberSetIndicator<Short>(Short::class) {
    override fun newEmptyBundle(): ShortSet = ShortSet()
    override fun getMaxValue(): Short = Short.MAX_VALUE
    override fun getMinValue(): Short = Short.MIN_VALUE
    override fun getInt(int: Int): Short = int.toShort()
}

data object ByteSetIndicator : NumberSetIndicator<Byte>(Byte::class) {
    override fun newEmptyBundle(): ByteSet = ByteSet()
    override fun getMaxValue(): Byte = Byte.MAX_VALUE
    override fun getMinValue(): Byte = Byte.MIN_VALUE
    override fun getInt(int: Int): Byte = int.toByte()
}

data object BooleanSetIndicator : SetIndicator<Boolean>(Boolean::class) {
    override fun newEmptyBundle(): BooleanSet = BooleanSet.NEITHER
    override fun newVarianceDefinedBundle(varianceSource: Context.Key): Bundle<Boolean> = BooleanSet.BOTH
    override fun newConstantBundle(constant: Boolean): Bundle<Boolean> =
        BooleanSet.fromBoolean(constant)
}

class GenericSetIndicator<T : Any>(clazz: KClass<T>) : SetIndicator<T>(clazz) {
    override fun newConstantBundle(constant: T): Bundle<T> = GenericSet(setOf(constant))
    override fun newEmptyBundle(): Bundle<T> = GenericSet(emptySet())
    override fun newVarianceDefinedBundle(varianceSource: Context.Key): Bundle<T> {
        TODO("Not yet implemented")
    }
}