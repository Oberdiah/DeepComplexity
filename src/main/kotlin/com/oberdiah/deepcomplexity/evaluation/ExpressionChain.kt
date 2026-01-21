package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.staticAnalysis.Indicator

data class SupportKey(private val id: Int, private val displayName: String) {
    override fun toString(): String = "$displayName [$id]"

    companion object {
        private var NEXT_ID = 0
        fun new(displayName: String): SupportKey = SupportKey(NEXT_ID++, displayName)
    }

    fun newIdCopy(): SupportKey = new(displayName)
    fun branchOff(): SupportKey = new("$displayName^")
}

/**
 * Prevent a massive combinatorial explosion by creating a support expression that can be referenced
 * multiple times in the primary expression.
 */
class ExpressionChain<T : Any> private constructor(
    val supportKey: SupportKey,
    val support: Expr<*>,
    val expr: Expr<T>,
) : Expr<T>() {
    companion object {
        fun <T : Any> new(supportKey: SupportKey, support: Expr<*>, expr: Expr<T>): ExpressionChain<T> =
            ExprPool.create(supportKey, support, expr)
    }

    override fun parts(): List<Any> = listOf(supportKey, support, expr)

    override val ind: Indicator<T> = expr.ind
}

class ExpressionChainPointer<T : Any> private constructor(
    val supportKey: SupportKey,
    override val ind: Indicator<T>
) : Expr<T>() {
    companion object {
        fun <T : Any> new(supportKey: SupportKey, ind: Indicator<T>): ExpressionChainPointer<T> =
            ExprPool.create(supportKey, ind)
    }

    override fun parts(): List<Any> = listOf(supportKey)
}
