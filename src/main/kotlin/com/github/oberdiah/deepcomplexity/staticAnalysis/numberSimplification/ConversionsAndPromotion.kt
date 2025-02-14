package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.evaluation.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.FloatSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.IExpr
import com.github.oberdiah.deepcomplexity.evaluation.IntSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.performACastTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ConversionsAndPromotion {
    class TypedPair<T : NumberSet<T>>(val first: IExpr<T>, val second: IExpr<T>) {
        fun <R> map(operation: (IExpr<T>, IExpr<T>) -> R): R = operation(first, second)
    }

    fun binaryNumericPromotion(
        exprA: IExpr<out NumberSet<*>>,
        exprB: IExpr<out NumberSet<*>>,
    ): TypedPair<*> {
        // Java Spec 5.6.2:
        // If either operand is of type double, the other is converted to double.
        // Otherwise, if either operand is of type float, the other is converted to float.
        // Otherwise, if either operand is of type long, the other is converted to long.
        // Otherwise, both operands are converted to type int.
        val indicatorA = exprA.getSetIndicator()
        val indicatorB = exprB.getSetIndicator()


        // It would be nice to be able to simplify this to calculate the indicator first and then create the typed pair,
        // but Kotlin doesn't allow for this yet.
        return when {
            indicatorA == DoubleSetIndicator || indicatorB == DoubleSetIndicator -> {
                TypedPair(
                    exprA.performACastTo(DoubleSetIndicator, false),
                    exprB.performACastTo(DoubleSetIndicator, false)
                )
            }

            indicatorA == FloatSetIndicator || indicatorB == FloatSetIndicator -> {
                TypedPair(
                    exprA.performACastTo(FloatSetIndicator, false),
                    exprB.performACastTo(FloatSetIndicator, false)
                )
            }

            indicatorA == LongSetIndicator || indicatorB == LongSetIndicator -> {
                TypedPair(
                    exprA.performACastTo(LongSetIndicator, false),
                    exprB.performACastTo(LongSetIndicator, false)
                )
            }

            else -> {
                TypedPair(
                    exprA.performACastTo(IntSetIndicator, false),
                    exprB.performACastTo(IntSetIndicator, false)
                )
            }
        }
    }
}