package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour

object ExpressionExtensions {
    fun Expr<Boolean>.inverted(constraints: ConstraintsOrPile): Expr<Boolean> =
        ExprConstrain.invert(this, constraints)

    fun Expr<*>.tryCastToNumbers(): Expr<out Number>? {
        return if (this.ind is NumberIndicator<*>) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<out Number>
        } else {
            null
        }
    }

    /**
     * Only supports [nonTrivial] = [Behaviour.Throw].
     */
    fun Expr<*>.castToNumbers(nonTrivial: Behaviour = Behaviour.Throw): Expr<out Number> {
        require(nonTrivial == Behaviour.Throw)
        return tryCastToNumbers() ?: throw IllegalStateException("Failed to cast to a number expr: $this ($ind)")
    }

    fun tryCastToObject(expr: Expr<*>): Expr<HeapMarker>? {
        return if (expr.ind is ObjectIndicator) {
            @Suppress("UNCHECKED_CAST")
            expr as Expr<HeapMarker>
        } else {
            null
        }
    }

    /**
     * Only supports [nonTrivial] = [Behaviour.Throw].
     */
    fun Expr<*>.castToObject(nonTrivial: Behaviour = Behaviour.Throw): Expr<HeapMarker> {
        require(nonTrivial == Behaviour.Throw)
        return tryCastToObject(this) ?: throw IllegalStateException("Failed to cast to an object expr: $this ($ind)")
    }

    fun Expr<*>.castToBoolean(nonTrivial: Behaviour = Behaviour.Throw): Expr<Boolean> =
        castTo(BooleanIndicator, nonTrivial)

    fun Expr<*>.castToContext(nonTrivial: Behaviour = Behaviour.Throw): Expr<VarsMarker> =
        castTo(VarsIndicator, nonTrivial)

    fun <T : Any> Expr<*>.castOrThrow(indicator: Indicator<T>): Expr<T> = castTo(indicator, Behaviour.Throw)

    fun <T : Any> Expr<*>.castTo(indicator: Indicator<T>, nonTrivial: Behaviour): Expr<T> {
        tryCastTo(indicator)?.let { return it }

        return when (nonTrivial) {
            Behaviour.Throw -> {
                throw IllegalStateException("Failed to cast '$this' to $indicator; (${this.ind} != $indicator)")
            }

            Behaviour.WrapWithTypeCastExplicit -> TypeCastExpr.new(this, indicator, explicit = true)
            Behaviour.WrapWithTypeCastImplicit -> TypeCastExpr.new(this, indicator, explicit = false)
        }
    }

    fun <T : Any> Expr<*>.tryCastTo(indicator: Indicator<T>): Expr<T>? {
        return if (this.ind == indicator) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }

    @Suppress("unused")
    inline fun <reified T : Any> Expr<*>.tryCastToReifiedExprType(): Expr<T>? {
        return if (this.ind.clazz == T::class) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }

    @Suppress("unused")
    inline fun <reified T : Any> Expr<*>.tryCastToReifiedIndicatorType(): Expr<T>? {
        return if (this.ind is T) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }
}