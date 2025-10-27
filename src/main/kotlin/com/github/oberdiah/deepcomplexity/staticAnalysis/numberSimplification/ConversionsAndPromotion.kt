package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.Expr
import com.github.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import com.github.oberdiah.deepcomplexity.staticAnalysis.*

object ConversionsAndPromotion {
    class TypedPair<T : Any>(val first: Expr<T>, val second: Expr<T>) {
        fun <R> map(operation: (Expr<T>, Expr<T>) -> R): R = operation(first, second)
    }

    fun castAToB(exprA: Expr<*>, exprB: Expr<*>, explicit: Boolean): TypedPair<*> {
        fun <T : Any> castTo(exprB: Expr<T>): TypedPair<*> {
            val castExprA: Expr<T> = exprA.castToUsingTypeCast(exprB.ind, explicit)
            return TypedPair(castExprA, exprB)
        }
        return castTo(exprB)
    }

    fun castNumbersAToB(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        explicit: Boolean
    ): TypedPair<out Number> {
        fun <T : Number> castTo(exprB: Expr<T>): TypedPair<out Number> {
            val castExprA: Expr<T> = exprA.castToUsingTypeCast(exprB.ind, explicit)
            return TypedPair(castExprA, exprB)
        }
        return castTo(exprB)
    }

    fun <T : Number> castBothNumbersTo(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        indicator: NumberSetIndicator<T>,
        explicit: Boolean
    ): TypedPair<T> {
        val castExprA: Expr<T> = exprA.castToUsingTypeCast(indicator, explicit)
        val castExprB: Expr<T> = exprB.castToUsingTypeCast(indicator, explicit)
        return TypedPair(castExprA, castExprB)
    }

    // This applies to:
    // - Array dimension declaration
    // - Array indexing
    // - Unary plus/minus
    // - Bitwise complement: ~
    // - >>, >>>, or <<, but only >>> in some cases.
    fun unaryNumericPromotion(expr: Expr<out Number>): Expr<out Number> {
        // Java Spec 5.6.1:
        // If the operand is of type byte, short, or char, it is promoted to a value of type int by a widening primitive conversion.
        return when (expr.ind) {
            DoubleSetIndicator, FloatSetIndicator, LongSetIndicator, IntSetIndicator -> expr
            else -> expr.castToUsingTypeCast(IntSetIndicator, false)
        }
    }

    fun binaryNumericPromotion(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
    ): TypedPair<out Number> {
        // Java Spec 5.6.2:
        // If either operand is of type double, the other is converted to double.
        // Otherwise, if either operand is of type float, the other is converted to float.
        // Otherwise, if either operand is of type long, the other is converted to long.
        // Otherwise, both operands are converted to type int.
        val targetIndicator = when {
            exprA.ind == DoubleSetIndicator || exprB.ind == DoubleSetIndicator -> DoubleSetIndicator
            exprA.ind == FloatSetIndicator || exprB.ind == FloatSetIndicator -> FloatSetIndicator
            exprA.ind == LongSetIndicator || exprB.ind == LongSetIndicator -> LongSetIndicator
            else -> IntSetIndicator
        }

        return castBothNumbersTo(exprA, exprB, targetIndicator, false)
    }
}