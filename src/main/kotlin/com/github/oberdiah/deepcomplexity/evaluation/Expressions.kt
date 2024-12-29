package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import org.apache.commons.numbers.core.DD
import kotlin.reflect.KClass

interface Expression<T : MoldableSet<*>> {
    fun evaluate(): T
}

enum class BinaryNumberOperation {
    ADDITION,
    MULTIPLICATION,
}

enum class ComparisonOperation {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
}

class BinaryNumberExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val operation: BinaryNumberOperation
) : Expression<NumberSet> {
    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.binaryOperation(rhs, operation)
    }
}

class ConstantNumber(val value: DD, val clazz: KClass<*>) : Expression<NumberSet> {
    override fun evaluate(): NumberSet {
        return NumberSet.singleValue(value, clazz)
    }
}

class ComparisonExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val comparison: ComparisonOperation
) : Expression<MoldableSet<Boolean>> {
    override fun evaluate(): MoldableSet<Boolean> {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.comparisonOperation(rhs, comparison)
    }
}