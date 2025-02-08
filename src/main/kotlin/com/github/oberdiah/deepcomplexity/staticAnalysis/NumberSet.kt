package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.intellij.psi.compiled.ClassFileDecompilers
import kotlin.reflect.KClass

// To swap between the two implementations of number sets, you should only have to change the SetIndicators.
sealed interface NumberSet<Self> : IMoldableSet<Self> where Self : IMoldableSet<Self>, Self : NumberSet<Self> {
    fun <T : NumberSet<T>> castToType(clazz: KClass<*>): T
    fun arithmeticOperation(other: Self, operation: BinaryNumberOp): Self
    fun comparisonOperation(other: Self, operation: ComparisonOp): BooleanSet

    /**
     * Returns the range of the set. i.e., the smallest value in the set and the largest.
     *
     * Returns null if the set is empty.
     */
    fun getRange(): Pair<Number, Number>?
    fun negate(): Self
    fun isOne(): Boolean

    /**
     * Given a set of `terms` to apply to this set on each iteration, evaluate
     * the number of iterations required to exit the `valid` number set.
     *
     * You can think of this set as being the 'initial' state.
     */
    fun evaluateLoopingRange(changeTerms: ConstraintSolver.EvaluatedCollectedTerms<Self>, valid: Self): Self

    /**
     * Returns a new set that satisfies the comparison operation.
     * We're the right hand side of the equation.
     */
    fun getSetSatisfying(comp: ComparisonOp): Self

    sealed interface FullyTypedNumberSet<T : Number, Self : FullyTypedNumberSet<T, Self>> : NumberSet<Self> {
        fun addRange(start: T, end: T, key: Context.Key)
        fun addConstant(value: T)

        sealed interface DoubleSet<Self : FullyTypedNumberSet<Double, Self>> : FullyTypedNumberSet<Double, Self>
        sealed interface FloatSet<Self : FullyTypedNumberSet<Float, Self>> : FullyTypedNumberSet<Float, Self>
        sealed interface IntSet<Self : FullyTypedNumberSet<Int, Self>> : FullyTypedNumberSet<Int, Self>
        sealed interface LongSet<Self : FullyTypedNumberSet<Long, Self>> : FullyTypedNumberSet<Long, Self>
        sealed interface ShortSet<Self : FullyTypedNumberSet<Short, Self>> : FullyTypedNumberSet<Short, Self>
        sealed interface ByteSet<Self : FullyTypedNumberSet<Byte, Self>> : FullyTypedNumberSet<Byte, Self>
    }

    companion object {
        fun <T : NumberSet<T>> fullRange(indicator: SetIndicator<T>, key: Context.Key): T {
            fun <T : Number, Set : FullyTypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = indicator.newEmptySet()
                set.addRange(indicator.getMinValue(), indicator.getMaxValue(), key)
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> fullPositiveRange(indicator: SetIndicator<T>, key: Context.Key): T {
            fun <T : Number, Set : FullyTypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = indicator.newEmptySet()
                set.addRange(indicator.getZero(), indicator.getMaxValue(), key)
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> fullNegativeRange(indicator: SetIndicator<T>, key: Context.Key): T {
            fun <T : Number, Set : FullyTypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = indicator.newEmptySet()
                set.addRange(indicator.getMinValue(), indicator.getZero(), key)
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> zero(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : FullyTypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = indicator.newEmptySet()
                set.addConstant(indicator.getZero())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> one(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : FullyTypedNumberSet<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = indicator.newEmptySet()
                set.addConstant(indicator.getOne())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> singleValue(value: T): Self {
            val set: Self = SetIndicator.newEmptySetFromValue(value)
            set.addConstant(value)
            return set
        }

        fun <T : Number, Self : FullyTypedNumberSet<T, Self>> fromRange(start: T, end: T, key: Context.Key): Self {
            val set: Self = SetIndicator.newEmptySetFromValue(start)
            set.addRange(start, end, key)
            return set
        }
    }
}
