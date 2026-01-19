package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator

data class SupportKey(private val id: Int, private val displayName: String) {
    override fun toString(): String = "$displayName [$id]"

    companion object {
        private var NEXT_ID = 0
        fun new(displayName: String): SupportKey = SupportKey(NEXT_ID++, displayName)
    }
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

        fun <T : Any> swapInplaceWithChain(
            expr: Expr<T>,
            ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
            replacer: (Expr<*>) -> Expr<*>,
        ): Expr<T> =
            rebuildTreeWithChain(expr, ifTraversal) { e -> replacer(e).castOrThrow(e.ind) }.castOrThrow(expr.ind)

        /**
         * The best way to create a new chain - you provide an expression you're performing substitution on,
         * and how to perform it, and we'll perform the substitution throughout the tree.
         *
         * The reason this is the best way is that ExpressionChain needs to generate itself
         * differently inside if conditions vs outside them, so capturing that nuance manually is challenging.
         */
        fun rebuildTreeWithChain(
            expr: Expr<*>,
            ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
            replacer: (Expr<*>) -> Expr<*>
        ): Expr<*> {
            // OldExpr -> NewExpr
            val replacerCache = mutableMapOf<Expr<*>, Expr<*>>()
            // NewExpr -> Count
            val replacementCounts = mutableMapOf<Expr<*>, Int>()

            expr.iterateTree(ifTraversal).forEach { oldExpr ->
                val newExpr = replacerCache.getOrPut(oldExpr) { replacer(oldExpr) }
                if (newExpr != oldExpr) {
                    replacementCounts[newExpr] = (replacementCounts[newExpr] ?: 0) + 1
                }
            }

            // Only generate chains for expressions that are used more than once
            val chainsToGenerate = replacementCounts
                .filterValues { it > 1 }
                .mapValues { SupportKey.new("Chained") }

            val replacedExpr = ExprTreeRebuilder.rebuildTree(expr, ifTraversal) { oldExpr ->
                val newExpr = replacerCache[oldExpr] ?: return@rebuildTree oldExpr
                chainsToGenerate[newExpr]?.let {
                    ExpressionChainPointer.new(it, newExpr.ind)
                } ?: newExpr
            }

            return chainsToGenerate.entries.fold(replacedExpr) { acc, (expr, key) ->
                new(key, expr, acc)
            }
        }
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
