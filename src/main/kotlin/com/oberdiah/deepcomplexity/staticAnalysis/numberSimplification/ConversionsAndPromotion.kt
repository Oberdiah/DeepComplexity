package com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castTo
import com.oberdiah.deepcomplexity.staticAnalysis.*

/**
 * What to do when casting an expression actually has to do something.
 */
enum class Behaviour {
    Throw,
    WrapWithTypeCastExplicit,
    WrapWithTypeCastImplicit
}

object ConversionsAndPromotion {
    class TypedPair<T : Any>(val first: Expr<T>, val second: Expr<T>) {
        fun <R> map(operation: (Expr<T>, Expr<T>) -> R): R = operation(first, second)
    }

    fun <T : Any> castAToB(exprA: Expr<*>, exprB: Expr<T>, nonTrivial: Behaviour): TypedPair<T> {
        val castExprA: Expr<T> = exprA.castTo(exprB.ind, nonTrivial)
        return TypedPair(castExprA, exprB)
    }

    fun <T : Number> castNumbersAToB(
        exprA: Expr<out Number>,
        exprB: Expr<T>,
        nonTrivial: Behaviour
    ): TypedPair<T> {
        val castExprA: Expr<T> = exprA.castTo(exprB.ind, nonTrivial)
        return TypedPair(castExprA, exprB)
    }

    fun <T : Number> castBothNumbersTo(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        indicator: NumberIndicator<T>,
        nonTrivial: Behaviour
    ): TypedPair<T> {
        val castExprA: Expr<T> = exprA.castTo(indicator, nonTrivial)
        val castExprB: Expr<T> = exprB.castTo(indicator, nonTrivial)
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
            DoubleIndicator, FloatIndicator, LongIndicator, IntIndicator -> expr
            else -> expr.castTo(IntIndicator, Behaviour.WrapWithTypeCastImplicit)
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
            exprA.ind == DoubleIndicator || exprB.ind == DoubleIndicator -> DoubleIndicator
            exprA.ind == FloatIndicator || exprB.ind == FloatIndicator -> FloatIndicator
            exprA.ind == LongIndicator || exprB.ind == LongIndicator -> LongIndicator
            else -> IntIndicator
        }

        return castBothNumbersTo(exprA, exprB, targetIndicator, Behaviour.WrapWithTypeCastImplicit)
    }
}