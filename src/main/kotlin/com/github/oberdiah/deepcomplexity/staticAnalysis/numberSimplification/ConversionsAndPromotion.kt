package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IExpr
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.performACastTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.TypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.ConstrainedSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConversionsAndPromotion {
    class TypedPair<T : ConstrainedSet<T>>(val first: IExpr<T>, val second: IExpr<T>) {
        fun <R> map(operation: (IExpr<T>, IExpr<T>) -> R): R = operation(first, second)
    }

    fun castAToB(exprA: IExpr<*>, exprB: IExpr<*>, explicit: Boolean): TypedPair<*> {
        fun <T : ConstrainedSet<T>> castTo(exprB: IExpr<T>): TypedPair<*> {
            val castExprA: IExpr<T> = exprA.performACastTo(exprB.getSetIndicator(), explicit)
            return TypedPair(exprB, castExprA)
        }
        return castTo(exprB)
    }

    fun castNumbersAToB(
        exprA: IExpr<out NumberSet<*>>,
        exprB: IExpr<out NumberSet<*>>,
        explicit: Boolean
    ): TypedPair<out NumberSet<*>> {
        fun <T : NumberSet<T>> castTo(exprB: IExpr<T>): TypedPair<out NumberSet<*>> {
            val castExprA: IExpr<T> = exprA.performACastTo(exprB.getSetIndicator(), explicit)
            return TypedPair(exprB, castExprA)
        }
        return castTo(exprB)
    }

    fun <T : Number, Set : TypedNumberSet<T, Set>> castBothNumbersTo(
        exprA: IExpr<out NumberSet<*>>,
        exprB: IExpr<out NumberSet<*>>,
        indicator: NumberSetIndicator<T, Set>,
        explicit: Boolean
    ): TypedPair<Set> {
        val castExprA: IExpr<Set> = exprA.performACastTo(indicator, explicit)
        val castExprB: IExpr<Set> = exprB.performACastTo(indicator, explicit)
        return TypedPair(castExprA, castExprB)
    }

    // This applies to:
    // - Array dimension declaration
    // - Array indexing
    // - Unary plus/minus
    // - Bitwise complement: ~
    // - >>, >>>, or <<, but only >>> in some cases.
    fun unaryNumericPromotion(expr: IExpr<out NumberSet<*>>): IExpr<out NumberSet<*>> {
        // Java Spec 5.6.1:
        // If the operand is of type byte, short, or char, it is promoted to a value of type int by a widening primitive conversion.
        val indicator = expr.getSetIndicator()

        return when (indicator) {
            DoubleSetIndicator, FloatSetIndicator, LongSetIndicator, IntSetIndicator -> expr
            else -> expr.performACastTo(IntSetIndicator, false)
        }
    }

    fun binaryNumericPromotion(
        exprA: IExpr<out NumberSet<*>>,
        exprB: IExpr<out NumberSet<*>>,
    ): TypedPair<out NumberSet<*>> {
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