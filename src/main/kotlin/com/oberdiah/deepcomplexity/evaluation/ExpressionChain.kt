package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder.replaceInTree
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

        fun withChainsAtBottom(expr: Expr<*>): Expr<*> {
            val removed = mutableListOf<ExpressionChain<*>>()
            val newExpr = expr.replaceTypeInTree<ExpressionChain<*>> { expr ->
                removed.add(expr)
                expr.expr
            }

            return removed.fold(newExpr) { acc, chain ->
                new(chain.supportKey, chain.support, acc)
            }
        }

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
//            val expr = withChainsAtBottom(expr)

            // The plan for next time:
            // We're going to start pulling chains down to the roots of expressions, in big piles
            // That's going to be a key part of this rebuilding process, we're going to make sure we always
            // pull chains to the bottom.
            // Then we're going to write some code to build a list of everything we've generated chains for
            // thus far
            // Then we're going to use that to potentially add to those existing chains rather than
            // making new ones.

            // OldExpr -> NewExpr
            val replacerCache = mutableMapOf<Expr<*>, Expr<*>>()
            // NewExpr -> Count
            val replacementCounts = mutableMapOf<Expr<*>, Int>()

            expr.iterateTree(ifTraversal).forEach { oldExpr ->
                val newExpr = replacerCache.getOrPut(oldExpr) {
                    replacer(oldExpr)
                }
                if (newExpr != oldExpr) {
                    replacementCounts[newExpr] = (replacementCounts[newExpr] ?: 0) + 1
                }
            }

            // Only generate chains for expressions that are used more than once
            val chainsToGenerate = replacementCounts
                .filterValues { it > 1 }
                .mapValues { SupportKey.new("Chained") }

            val replacedExpr = expr.replaceInTree(ifTraversal) { oldExpr ->
                val newExpr = replacerCache[oldExpr] ?: return@replaceInTree oldExpr
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
