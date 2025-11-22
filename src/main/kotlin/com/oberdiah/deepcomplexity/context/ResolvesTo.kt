package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator

/**
 * [ResolvesTo]s must be very carefully resolved; they cannot be resolved in any context
 * they were created in. Only [Context]s have the ability to create and resolve them.
 */
interface ResolvesTo<T : Any> {
    val lifetime: UnknownKey.Lifetime
        get() = UnknownKey.Lifetime.FOREVER
    val ind: Indicator<T>

    fun toLeafExpr(): Expr<T>
    fun safelyResolveUsing(vars: Vars): Expr<*> = toLeafExpr()
    fun withAddedContextId(newId: ContextId): ResolvesTo<T> = this
    fun isPlaceholder(): Boolean = false
}