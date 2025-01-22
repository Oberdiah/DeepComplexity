package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.evaluation.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ByteSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp.*
import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetImpl.ByteSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetImpl.DoubleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetImpl.FloatSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetImpl.IntSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetImpl.LongSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetImpl.ShortSet
import kotlin.reflect.KClass

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

    companion object {
        fun <T : NumberSet<T>> newFromIndicator(ind: SetIndicator<T>): T {
            return when (ind) {
                is ByteSetIndicator -> ind.sillyCast(ind, ByteSet())
                is ShortSetIndicator -> ind.sillyCast(ind, ShortSet())
                is IntSetIndicator -> ind.sillyCast(ind, IntSet())
                is LongSetIndicator -> ind.sillyCast(ind, LongSet())
                is FloatSetIndicator -> ind.sillyCast(ind, FloatSet())
                is DoubleSetIndicator -> ind.sillyCast(ind, DoubleSet())
                is BooleanSetIndicator, GenericSetIndicator ->
                    throw IllegalArgumentException("Cannot create number set from boolean or generic indicator")
            }
        }

        fun <T : NumberSet<T>> fullRange(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : NumberSetImpl<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = newFromIndicator(indicator)
                set.addRange(indicator.getMinValue(), indicator.getMaxValue())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> fullPositiveRange(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : NumberSetImpl<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = newFromIndicator(indicator)
                set.addRange(indicator.getZero(), indicator.getMaxValue())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> fullNegativeRange(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : NumberSetImpl<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = newFromIndicator(indicator)
                set.addRange(indicator.getMinValue(), indicator.getZero())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> emptyRange(indicator: SetIndicator<T>): T {
            return newFromIndicator(indicator)
        }

        fun <T : NumberSet<T>> zero(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : NumberSetImpl<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = newFromIndicator(indicator)
                set.addRange(indicator.getZero(), indicator.getZero())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : NumberSet<T>> one(indicator: SetIndicator<T>): T {
            fun <T : Number, Set : NumberSetImpl<T, Set>> extra(indicator: NumberSetIndicator<T, Set>): Set {
                val set = newFromIndicator(indicator)
                set.addRange(indicator.getOne(), indicator.getOne())
                return set
            }
            @Suppress("UNCHECKED_CAST")
            return extra(indicator as NumberSetIndicator<*, *>) as T
        }

        fun <T : Number, Self : NumberSetImpl<T, Self>> singleValue(value: T): Self {
            val set: Self = newFromIndicator(SetIndicator.fromValue(value))
            set.addRange(value, value)
            return set
        }
    }
}
