package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import com.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty

/**
 * [ResolvesTo]s must be very carefully resolved; they cannot be resolved in any context
 * they were created in. Only [Context]s can create and resolve them.
 */
interface ResolvesTo<T : Any> {
    val lifetime: UnknownKey.Lifetime
        get() = UnknownKey.Lifetime.FOREVER
    val ind: Indicator<T>

    fun toLeafExpr(): Expr<T>
    fun safelyResolveUsing(vars: Vars): Expr<*> = toLeafExpr()
    fun withAddedContextId(newId: ContextId): ResolvesTo<T> = this
    fun isPlaceholder(): Boolean = false

    /**
     * Solely used to store any placeholder expression a type may have picked up
     * due to aliasing. Thrown away after stacking.
     *
     * This is needed to allow relatively simple cases to work correctly, e.g.
     * ```
     * a.x = 5;
     * if (b.x == 5) {
     * 	// foo
     * }
     * ```
     *
     * In that example, the context would look as follows after a.x was assigned:
     * ```
     * {
     *     a.x -> 5
     *     Placeholder(T).x -> if (a == Placeholder(T)) ? 5 : Placeholder(T).x
     * }
     * ```
     */
    data class PlaceholderResolvesTo(override val ind: ObjectIndicator) : ResolvesTo<HeapMarker> {
        override fun toLeafExpr(): VariableExpr<HeapMarker> = VariableExpr.new(this)
        override fun isPlaceholder(): Boolean = true
        override fun toString(): String = "P(${ind.type.toStringPretty()})"
    }
}