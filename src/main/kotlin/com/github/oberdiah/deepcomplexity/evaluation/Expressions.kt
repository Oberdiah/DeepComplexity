package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

sealed class Expression<out T : MoldableSet>(val clazz: KClass<*>) {
    abstract fun evaluate(): T

    inline fun <reified T : MoldableSet> attemptCastTo(): Expression<T>? {
        if (clazz == T::class) {
            return this as Expression<T>
        }
        return null
    }
}

enum class BinaryNumberOperation {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION,
}

enum class ComparisonOperation {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
}

class ArithmeticExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val operation: BinaryNumberOperation
) : Expression<NumberSet>(NumberSet::class) {
    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.arithmeticOperation(rhs, operation)
    }
}

class ComparisonExpression(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>,
    val comparison: ComparisonOperation
) : Expression<BooleanSet>(BooleanSet::class) {
    override fun evaluate(): BooleanSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.comparisonOperation(rhs, comparison)
    }
}