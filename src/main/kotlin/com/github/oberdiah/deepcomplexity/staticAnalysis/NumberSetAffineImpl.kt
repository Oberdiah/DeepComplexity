package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.ByteSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet.ByteSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet.DoubleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet.FloatSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet.IntSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet.LongSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet.FullyTypedNumberSet.ShortSet
import java.math.BigInteger
import kotlin.reflect.KClass

sealed class NumberSetAffineImpl<T : Number, Self : NumberSetAffineImpl<T, Self>>(
    private val setIndicator: NumberSetIndicator<T, Self>
) : FullyTypedNumberSet<T, Self> {
    class DoubleSetAffine : NumberSetAffineImpl<Double, DoubleSetAffine>(DoubleSetIndicator), DoubleSet<DoubleSetAffine>
    class FloatSetAffine : NumberSetAffineImpl<Float, FloatSetAffine>(FloatSetIndicator), FloatSet<FloatSetAffine>
    class IntSetAffine : NumberSetAffineImpl<Int, IntSetAffine>(IntSetIndicator), IntSet<IntSetAffine>
    class LongSetAffine : NumberSetAffineImpl<Long, LongSetAffine>(LongSetIndicator), LongSet<LongSetAffine>
    class ShortSetAffine : NumberSetAffineImpl<Short, ShortSetAffine>(ShortSetIndicator), ShortSet<ShortSetAffine>
    class ByteSetAffine : NumberSetAffineImpl<Byte, ByteSetAffine>(ByteSetIndicator), ByteSet<ByteSetAffine>

    fun duplicateMe(): Self {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            is ByteSetAffine -> ByteSetAffine()
            is ShortSetAffine -> ShortSetAffine()
            is IntSetAffine -> IntSetAffine()
            is LongSetAffine -> LongSetAffine()
            is FloatSetAffine -> FloatSetAffine()
            is DoubleSetAffine -> DoubleSetAffine()
        } as Self
    }

    fun verifyIsInteger() {
        if (this is DoubleSetAffine || this is FloatSetAffine) {
            throw IllegalArgumentException("This set is not an integer set")
        }
    }

    override fun addRange(start: T, end: T) {
        verifyIsInteger()

    }

    override fun <T : NumberSet<T>> castToType(clazz: KClass<*>): T {
        TODO("Not yet implemented")
    }

    override fun arithmeticOperation(
        other: Self,
        operation: BinaryNumberOp
    ): Self {
        TODO("Not yet implemented")
    }

    override fun comparisonOperation(
        other: Self,
        operation: ComparisonOp
    ): BooleanSet {
        TODO("Not yet implemented")
    }

    override fun getRange(): Pair<Number, Number>? {
        TODO("Not yet implemented")
    }

    override fun negate(): Self {
        TODO("Not yet implemented")
    }

    override fun isOne(): Boolean {
        TODO("Not yet implemented")
    }

    override fun evaluateLoopingRange(
        changeTerms: ConstraintSolver.EvaluatedCollectedTerms<Self>,
        valid: Self
    ): Self {
        TODO("Not yet implemented")
    }

    override fun getSetSatisfying(comp: ComparisonOp): Self {
        TODO("Not yet implemented")
    }

    override fun getSetIndicator(): SetIndicator<Self> {
        TODO("Not yet implemented")
    }

    override fun union(other: Self): Self {
        TODO("Not yet implemented")
    }

    override fun intersect(other: Self): Self {
        TODO("Not yet implemented")
    }

    override fun invert(): Self {
        TODO("Not yet implemented")
    }

    override fun contains(element: Any): Boolean {
        TODO("Not yet implemented")
    }

}