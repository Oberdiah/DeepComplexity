package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.Context
import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.context.QualifiedFieldKey
import com.oberdiah.deepcomplexity.context.Qualifier
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain

object ExpressionExtensions {
    fun Expr<Boolean>.inverted(): Expr<Boolean> = ExprConstrain.invert(this)

    fun Expr<*>.castToNumbers(): Expr<out Number> {
        if (this.ind is NumberSetIndicator<*>) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<out Number>
        } else {
            throw IllegalStateException("Failed to cast to a number: $this ($ind)")
        }
    }

    fun Expr<*>.castToBoolean(): Expr<Boolean> {
        return this.tryCastTo(BooleanSetIndicator)
            ?: throw IllegalStateException("Failed to cast to a boolean: $this ($ind)")
    }

    fun Expr<*>.castToObject(): Expr<HeapMarker> {
        if (this.ind is ObjectSetIndicator) {
            @Suppress("UNCHECKED_CAST")
            return this as Expr<HeapMarker>
        } else {
            throw IllegalStateException("Failed to cast to an object: $this ($ind)")
        }
    }

    inline fun <Set : Any, reified T : Expr<Set>> Expr<*>.tryCastToReified(indicator: SetIndicator<Set>): T? {
        return if (this::class == T::class && indicator == this.ind) {
            @Suppress("UNCHECKED_CAST")
            this as T
        } else {
            null
        }
    }

    /**
     * Basically a nicer way of doing `this as Expr<T>`, but with type checking :)
     */
    fun <T : Any> Expr<*>.tryCastTo(indicator: SetIndicator<T>): Expr<T>? {
        return if (this.ind == indicator) {
            @Suppress("UNCHECKED_CAST")
            this as Expr<T>
        } else {
            null
        }
    }

    fun <T : Any> Expr<*>.castOrThrow(indicator: SetIndicator<T>): Expr<T> {
        return this.tryCastTo(indicator)
            ?: throw IllegalStateException("Failed to cast to $indicator: $this (${this.ind})")
    }

    /**
     * Wrap the expression in a type cast to the given indicator.
     */
    fun <T : Any> Expr<*>.castToUsingTypeCast(indicator: SetIndicator<T>, explicit: Boolean): Expr<T> {
        return if (this.ind == indicator) {
            this.castOrThrow(indicator)
        } else {
            TypeCastExpr(this, indicator, explicit)
        }
    }

    fun Expr<*>.getField(context: Context, field: QualifiedFieldKey.Field): Expr<*> {
        return replaceTypeInLeaves<LeafExpr<*>>(field.ind) {
            context.getVar(QualifiedFieldKey(it.underlying as Qualifier, field))
        }
    }

    /**
     * Swaps out the leaves of the expression. Every leaf of the expression must have type [Q].
     * An ergonomic and slightly constrained version of [replaceLeaves].
     *
     * Will assume everything you return has type [newInd], and throw an exception if that is not true. This
     * is mainly for ergonomic reasons, so you don't have to do the casting yourself.
     */
    inline fun <reified Q> Expr<*>.replaceTypeInLeaves(
        newInd: SetIndicator<*>,
        crossinline replacement: (Q) -> Expr<*>
    ): Expr<*> {
        return object {
            inline operator fun <T : Any> invoke(
                newInd: SetIndicator<T>,
                crossinline replacement: (Q) -> Expr<*>
            ): Expr<*> {
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
        }(newInd, replacement)
    }
}