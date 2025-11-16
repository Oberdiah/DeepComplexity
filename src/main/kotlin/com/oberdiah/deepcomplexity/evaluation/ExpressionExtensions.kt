package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.Context
import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.context.QualifiedFieldKey
import com.oberdiah.deepcomplexity.context.Qualifier
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain

object ExpressionExtensions {
    fun Expr<Boolean>.inverted(): Expr<Boolean> = ExprConstrain.invert(this)

    fun Expr<*>.castToNumbers(): Expr<out Number> {
        if (this.ind is NumberIndicator<*>) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<out Number>
        } else {
            throw IllegalStateException("Failed to cast to a number expr: $this ($ind)")
        }
    }

    fun Expr<*>.castToBoolean(): Expr<Boolean> {
        return this.tryCastTo(BooleanIndicator)
            ?: throw IllegalStateException("Failed to cast to a boolean expr: $this ($ind)")
    }

    fun Expr<*>.castToObject(): Expr<HeapMarker> {
        if (this.ind is ObjectIndicator) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<HeapMarker>
        } else {
            throw IllegalStateException("Failed to cast to an object expr: $this ($ind)")
        }
    }

    fun Expr<*>.castToContext(): Expr<VarsMarker> {
        if (this.ind is VarsIndicator) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<VarsMarker>
        } else {
            throw IllegalStateException("Failed to cast to a context expr: $this ($ind)")
        }
    }

    /**
     * Basically a nicer way of doing `this as Expr<T>`, but with type checking :)
     */
    fun <T : Any> Expr<*>.tryCastTo(indicator: Indicator<T>): Expr<T>? {
        return if (this.ind == indicator) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }

    inline fun <reified T : Any> Expr<*>.tryCastTo(): Expr<T>? {
        return if (this.ind.clazz == T::class) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }

    fun <T : Any> Expr<*>.castOrThrow(indicator: Indicator<T>): Expr<T> {
        return this.tryCastTo(indicator)
            ?: throw IllegalStateException("Failed to cast '$this' to $indicator; (${this.ind} != $indicator)")
    }

    inline fun <reified T : Any> Expr<*>.castOrThrow(): Expr<T> {
        return this.tryCastTo<T>()
            ?: throw IllegalStateException("Failed to cast '$this' to ${T::class}; (${this.ind} != ${T::class})")
    }

    /**
     * Wrap the expression in a type cast to the given indicator.
     */
    fun <T : Any> Expr<*>.castToUsingTypeCast(indicator: Indicator<T>, explicit: Boolean): Expr<T> {
        return if (this.ind == indicator) {
            this.castOrThrow(indicator)
        } else {
            TypeCastExpr(this, indicator, explicit)
        }
    }

    // We should be able to de-dupe this one day in the fairly near future
    fun Expr<*>.getField(context: Context, field: QualifiedFieldKey.Field): Expr<*> {
        return replaceTypeInLeaves<LeafExpr<*>>(field.ind) {
            context.getVar(QualifiedFieldKey(it.underlying as Qualifier, field))
        }
    }

    /**
     * Swaps out the leaves of the expression. Every leaf of the expression must have type [Q].
     * An ergonomic and slightly constrained version of [com.oberdiah.deepcomplexity.evaluation.Expr.replaceLeaves].
     *
     * Will assume everything you return has type [newInd], and throw an exception if that is not true. This
     * is mainly for ergonomic reasons, so you don't have to do the casting yourself.
     */
    internal inline fun <reified Q : Any> Expr<*>.replaceTypeInLeaves(
        newInd: Indicator<*>,
        crossinline replacement: (Q) -> Expr<*>
    ): Expr<*> {
        return object {
            operator fun <T : Any> invoke(
                newInd: Indicator<T>,
            ): Expr<T> {
                return this@replaceTypeInLeaves.replaceTypeInLeavesWithInd<Q, T>(newInd, replacement)
            }
        }(newInd)
    }

    internal inline fun <reified Q : Any, reified T : Any> Expr<*>.replaceTypeInLeavesTyped(
        crossinline replacement: (Q) -> Expr<*>
    ): Expr<T> {
        return replaceTypeInLeavesWithInd<Q, T>(Indicator.fromClass(T::class), replacement)
    }

    private inline fun <reified Q : Any, T : Any> Expr<*>.replaceTypeInLeavesWithInd(
        newInd: Indicator<T>,
        crossinline replacement: (Q) -> Expr<*>
    ): Expr<T> {
        return replaceLeaves(ExprTreeRebuilder.LeafReplacer(newInd) { expr ->
            val newExpr = if (expr is Q) {
                replacement(expr)
            } else {
                throw IllegalArgumentException(
                    "Expected ${Q::class.simpleName}, got ${expr::class.simpleName}"
                )
            }

            newExpr.tryCastTo(newInd) ?: throw IllegalStateException(
                "(${newExpr.ind} != $newInd) $newExpr does not match $expr"
            )
        })
    }
}