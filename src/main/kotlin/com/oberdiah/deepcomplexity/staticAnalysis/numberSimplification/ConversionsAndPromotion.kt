package com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion.binaryPromotion
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion.coerceAToB
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT

object ConversionsAndPromotion {
    class TypedBundlePair<T : Any>(val first: Bundle<T>, val second: Bundle<T>) {
        fun <R> map(operation: (Bundle<T>, Bundle<T>) -> R): R = operation(first, second)
    }

    class TypedExprPair<T : Any>(val first: Expr<T>, val second: Expr<T>) {
        fun <R> map(operation: (Expr<T>, Expr<T>) -> R): R = operation(first, second)
    }

    class TypedSetPair<T : Any>(val first: ISet<T>, val second: ISet<T>) {
        fun <R> map(operation: (ISet<T>, ISet<T>) -> R): R = operation(first, second)
    }

    /**
     * Coerces [exprA] to the same type as [exprB], and returns a pair of the coerced [exprA] and [exprB].
     */
    fun <T : Any> coerceAToB(exprA: Expr<*>, exprB: Expr<T>): TypedExprPair<T> {
        val coercedExprA: Expr<T> = exprA.coerceTo(exprB.ind)
        return TypedExprPair(coercedExprA, exprB)
    }

    fun <T : Any> coerceAToB(bundleA: Bundle<*>, bundleB: Bundle<T>): TypedBundlePair<T> {
        val coercedExprA: Bundle<T> = bundleA.coerceTo(bundleB.ind)
        return TypedBundlePair(coercedExprA, bundleB)
    }

    fun <T : Any> coerceAToB(setA: ISet<*>, setB: ISet<T>): TypedSetPair<T> {
        val coercedExprA: ISet<T> = setA.coerceTo(setB.ind)
        return TypedSetPair(coercedExprA, setB)
    }

    @Suppress("unused")
    fun castAToB(exprA: Expr<*>, exprB: Expr<*>): TypedExprPair<*> {
        /**
         * Whenever you may want to use cast A to B, it's likely you should be using other tools
         * at your disposal instead. An implied cast like this typically comes with strict
         * rules about which gets cast to what and when, and it would be good if we could
         * follow those. Look into [coerceAToB] (for casts that are just type wrangling) or
         * [binaryPromotion] (for casts that cause genuine behaviour change) instead.
         */
        WONT_IMPLEMENT()
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
            else -> expr.castTo(IntIndicator)
        }
    }

    fun binaryPromotion(
        exprA: Expr<*>,
        exprB: Expr<*>,
    ): TypedExprPair<*> {
        if (exprA.ind == exprB.ind) {
            @Suppress("UNCHECKED_CAST")
            return TypedExprPair(exprA as Expr<Any>, exprB as Expr<Any>)
        }

        return when {
            exprA.ind is NumberIndicator<*> && exprB.ind is NumberIndicator<*> -> binaryNumericPromotion(
                exprA.coerceToNumbers(),
                exprB.coerceToNumbers()
            )

            else -> throw IllegalStateException("Unsupported binary promotion for indicators ${exprA.ind} and ${exprB.ind}")
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

        return castBothNumbersTo(exprA, exprB, targetIndicator)
    }

    fun <T : Number> castBothNumbersTo(
        exprA: Expr<out Number>,
        exprB: Expr<out Number>,
        indicator: NumberIndicator<T>
    ): TypedExprPair<T> {
        val castExprA: Expr<T> = exprA.castTo(indicator)
        val castExprB: Expr<T> = exprB.castTo(indicator)
        return TypedExprPair(castExprA, castExprB)
    }
}