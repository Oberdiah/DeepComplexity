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
            var finalExpr = expr

            val exprReplacements = mutableMapOf<Expr<*>, SupportKey>()

            finalExpr = ExprTreeRebuilder.rebuildTree(finalExpr, ifTraversal) { e ->
                val newExpr = replacer(e)
                if (newExpr != e) {
                    val supportKey = exprReplacements.getOrPut(newExpr) {
                        SupportKey.new("Chained")
                    }
                    ExpressionChainPointer(supportKey, newExpr.ind)
                } else {
                    newExpr
                }
            }

            for (entry in exprReplacements) {
                finalExpr = ExpressionChain(
                    entry.value,
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
