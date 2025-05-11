package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.IExpr
import com.github.oberdiah.deepcomplexity.evaluation.performACastTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.*

object ConversionsAndPromotion {
    class TypedPair<T : Any>(val first: IExpr<T>, val second: IExpr<T>) {
        fun <R> map(operation: (IExpr<T>, IExpr<T>) -> R): R = operation(first, second)
    }

    fun castAToB(exprA: IExpr<*>, exprB: IExpr<*>, explicit: Boolean): TypedPair<*> {
        fun <T : Any> castTo(exprB: IExpr<T>): TypedPair<*> {
            val castExprA: IExpr<T> = exprA.performACastTo(exprB.getSetIndicator(), explicit)
            return TypedPair(exprB, castExprA)
        }
        return castTo(exprB)
    }

    fun castNumbersAToB(
        exprA: IExpr<out Number>,
        exprB: IExpr<out Number>,
        explicit: Boolean
    ): TypedPair<out Number> {
        fun <T : Number> castTo(exprB: IExpr<T>): TypedPair<out Number> {
            val castExprA: IExpr<T> = exprA.performACastTo(exprB.getSetIndicator(), explicit)
            return TypedPair(exprB, castExprA)
        }
        return castTo(exprB)
    }

    fun <T : Number> castBothNumbersTo(
        exprA: IExpr<out Number>,
        exprB: IExpr<out Number>,
        indicator: NumberSetIndicator<T>,
        explicit: Boolean
    ): TypedPair<T> {
        val castExprA: IExpr<T> = exprA.performACastTo(indicator, explicit)
        val castExprB: IExpr<T> = exprB.performACastTo(indicator, explicit)
        return TypedPair(castExprA, castExprB)
    }

    // This applies to:
    // - Array dimension declaration
    // - Array indexing
    // - Unary plus/minus
    // - Bitwise complement: ~
    // - >>, >>>, or <<, but only >>> in some cases.
    fun unaryNumericPromotion(expr: IExpr<out Number>): IExpr<out Number> {
        // Java Spec 5.6.1:
        // If the operand is of type byte, short, or char, it is promoted to a value of type int by a widening primitive conversion.
        val indicator = expr.getSetIndicator()

        return when (indicator) {
            DoubleSetIndicator, FloatSetIndicator, LongSetIndicator, IntSetIndicator -> expr
            else -> expr.performACastTo(IntSetIndicator, false)
        }
    }

    fun binaryNumericPromotion(
        exprA: IExpr<out Number>,
        exprB: IExpr<out Number>,
    ): TypedPair<out Number> {
        // Java Spec 5.6.2:
        // If either operand is of type double, the other is converted to double.
        // Otherwise, if either operand is of type float, the other is converted to float.
        // Otherwise, if either operand is of type long, the other is converted to long.
        // Otherwise, both operands are converted to type int.
        val indicatorA = exprA.getSetIndicator()
        val indicatorB = exprB.getSetIndicator()

        val targetIndicator = when {
            indicatorA == DoubleSetIndicator || indicatorB == DoubleSetIndicator -> DoubleSetIndicator
            indicatorA == FloatSetIndicator || indicatorB == FloatSetIndicator -> FloatSetIndicator
            indicatorA == LongSetIndicator || indicatorB == LongSetIndicator -> LongSetIndicator
            else -> IntSetIndicator
        }

        return castBothNumbersTo(exprA, exprB, targetIndicator, false)
    }
}