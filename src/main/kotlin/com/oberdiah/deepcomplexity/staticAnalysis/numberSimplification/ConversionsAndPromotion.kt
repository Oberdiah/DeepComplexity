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

    fun castAToB(exprA: Expr<*>, exprB: Expr<*>, nonTrivialBehaviour: Behaviour): TypedPair<*> {
        fun <T : Any> castTo(exprB: Expr<T>): TypedPair<*> {
            val castExprA: Expr<T> = exprA.castTo(exprB.ind, nonTrivialBehaviour)
            return TypedPair(castExprA, exprB)
        }
        return castTo(exprB)
    }

    fun castNumbersAToB(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        nonTrivialBehaviour: Behaviour
    ): TypedPair<out Number> {
        fun <T : Number> castTo(exprB: Expr<T>): TypedPair<out Number> {
            val castExprA: Expr<T> = exprA.castTo(exprB.ind, nonTrivialBehaviour)
            return TypedPair(castExprA, exprB)
        }
        return castTo(exprB)
    }

    fun <T : Number> castBothNumbersTo(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        indicator: NumberIndicator<T>,
        nonTrivialBehaviour: Behaviour
    ): TypedPair<T> {
        val castExprA: Expr<T> = exprA.castTo(indicator, nonTrivialBehaviour)
        val castExprB: Expr<T> = exprB.castTo(indicator, nonTrivialBehaviour)
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