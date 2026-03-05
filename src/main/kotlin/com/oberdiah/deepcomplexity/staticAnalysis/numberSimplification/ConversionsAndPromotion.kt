package com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle

/**
 * What to do when casting actually has to do something.
 */
enum class Behaviour {
    /** Just throw an exception, don't allow any non-trivial casts. */
    Throw,

    /**
     * Attempts to perform a hard cast, throwing an exception if that isn't possible.
     *
     * A hard cast is a cast that physically changes the object in some way.
     *
     * In the case of expressions, this wraps us in a type-cast expression.
     * In the case of bundles, variances, sets, etc. it attempts a Java-style cast with the same success/failure
     * conditions. (e.g. you can cast a `NumberSet<Int>` to a `NumberSet<Double>`, but not to a `NumberSet<String>`).
     */
    PerformHardCast
}

object ConversionsAndPromotion {
    class TypedBundlePair<T : Any>(val first: Bundle<T>, val second: Bundle<T>) {
        fun <R> map(operation: (Bundle<T>, Bundle<T>) -> R): R = operation(first, second)
    }

    class TypedExprPair<T : Any>(val first: Expr<T>, val second: Expr<T>) {
        fun <R> map(operation: (Expr<T>, Expr<T>) -> R): R = operation(first, second)
    }

    fun <T : Any> castAToB(exprA: Expr<*>, exprB: Expr<T>, nonTrivial: Behaviour): TypedExprPair<T> {
        val castExprA: Expr<T> = exprA.castTo(exprB.ind, nonTrivial)
        return TypedExprPair(castExprA, exprB)
    }

    fun <T : Number> castNumbersAToB(
        exprA: Expr<out Number>,
        exprB: Expr<T>,
        nonTrivial: Behaviour
    ): TypedExprPair<T> {
        val castExprA: Expr<T> = exprA.castTo(exprB.ind, nonTrivial)
        return TypedExprPair(castExprA, exprB)
    }

    fun <T : Number> castBothNumbersTo(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        indicator: NumberIndicator<T>,
        nonTrivial: Behaviour
    ): TypedExprPair<T> {
        val castExprA: Expr<T> = exprA.castTo(indicator, nonTrivial)
        val castExprB: Expr<T> = exprB.castTo(indicator, nonTrivial)
        return TypedExprPair(castExprA, castExprB)
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
            else -> expr.castTo(IntIndicator, Behaviour.PerformHardCast)
        }
    }

    fun binaryNumericPromotion(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
    ): TypedExprPair<out Number> {
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

        return castBothNumbersTo(exprA, exprB, targetIndicator, Behaviour.PerformHardCast)
    }
}