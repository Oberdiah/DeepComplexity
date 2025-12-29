package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour

object ExpressionExtensions {
    fun Expr<Boolean>.inverted(): Expr<Boolean> = ExprConstrain.invert(this)

    /**
     * Only supports [nonTrivialBehaviour] = [Behaviour.Throw].
     */
    fun Expr<*>.castToNumbers(nonTrivialBehaviour: Behaviour = Behaviour.Throw): Expr<out Number> {
        require(nonTrivialBehaviour == Behaviour.Throw)
        if (this.ind is NumberIndicator<*>) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<out Number>
        } else {
            throw IllegalStateException("Failed to cast to a number expr: $this ($ind)")
        }
    }

    /**
     * Only supports [nonTrivialBehaviour] = [Behaviour.Throw].
     */
    fun Expr<*>.castToObject(nonTrivialBehaviour: Behaviour = Behaviour.Throw): Expr<HeapMarker> {
        require(nonTrivialBehaviour == Behaviour.Throw)
        if (this.ind is ObjectIndicator) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<HeapMarker>
        } else {
            throw IllegalStateException("Failed to cast to an object expr: $this ($ind)")
        }
    }

    fun Expr<*>.castToBoolean(nonTrivialBehaviour: Behaviour = Behaviour.Throw): Expr<Boolean> =
        castTo(BooleanIndicator, nonTrivialBehaviour)

    fun Expr<*>.castToContext(nonTrivialBehaviour: Behaviour = Behaviour.Throw): Expr<VarsMarker> =
        castTo(VarsIndicator, nonTrivialBehaviour)

    fun <T : Any> Expr<*>.castOrThrow(indicator: Indicator<T>): Expr<T> = castTo(indicator, Behaviour.Throw)

    fun <T : Any> Expr<*>.castTo(indicator: Indicator<T>, nonTrivialBehaviour: Behaviour): Expr<T> {
        tryCastTo(indicator)?.let { return it }

        return when (nonTrivialBehaviour) {
            Behaviour.Throw -> throw IllegalStateException("Failed to cast '$this' to $indicator; (${this.ind} != $indicator)")
            Behaviour.WrapWithTypeCastExplicit -> TypeCastExpr(this, indicator, explicit = true)
            Behaviour.WrapWithTypeCastImplicit -> TypeCastExpr(this, indicator, explicit = false)
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

    inline fun <reified T : Any> Expr<*>.tryCastToReifiedExprType(): Expr<T>? {
        return if (this.ind.clazz == T::class) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }

    inline fun <reified T : Any> Expr<*>.tryCastToReifiedIndicatorType(): Expr<T>? {
        return if (this.ind is T) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }
}