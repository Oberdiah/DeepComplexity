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
data class ExpressionChain<T : Any>(
    val supportKey: SupportKey,
    val support: Expr<*>,
    val expr: Expr<T>,
) : Expr<T>() {
    companion object {
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
            data class Replacement(val supportKey: SupportKey, var numReplacements: Int)

            // A map from the original expression to whatever we replaced it with
            val replacementCache = mutableMapOf<Expr<*>, Expr<*>>()
            // A map from the new expression to its replacement info
            val newExprInfo = mutableMapOf<Expr<*>, Replacement>()

            // Before we actually perform the rebuild, we need to figure out what we're dealing with.
            // We don't want to create chains when we don't need to.
            expr.iterateTree(ifTraversal).forEach {
                val newExpr = replacer(it)
                // EXPR_EQUALITY_PERF_ISSUE: Note, this method has lots of issues with perf; see the maps using
                // expressions as keys above - what we really want is to cache those expression hashes.
                if (newExpr != it) {
                    replacementCache[it] = newExpr
                    val rep = newExprInfo.getOrPut(newExpr) {
                        Replacement(SupportKey.new("Chained"), 0)
                    }
                    rep.numReplacements++
                }
            }

            var finalExpr = expr

            finalExpr = ExprTreeRebuilder.rebuildTree(finalExpr, ifTraversal) { e ->
                val newExpr = replacementCache[e] ?: return@rebuildTree e
                val replacement = newExprInfo[newExpr]
                if (replacement != null) {
                    if (replacement.numReplacements == 1) {
                        // No need to create a chain if it's only used once
                        newExpr
                    } else {
                        ExpressionChainPointer(replacement.supportKey, newExpr.ind)
                    }
                } else {
                    e
                }
            }

            for (entry in newExprInfo.filterValues { it.numReplacements > 1 }) {
                finalExpr = ExpressionChain(
                    entry.value.supportKey,
                    entry.key,
                    finalExpr,
                )
            }

            return finalExpr
        }
    }

    override val ind: Indicator<T> = expr.ind
}

data class ExpressionChainPointer<T : Any>(val supportKey: SupportKey, override val ind: Indicator<T>) : Expr<T>()
