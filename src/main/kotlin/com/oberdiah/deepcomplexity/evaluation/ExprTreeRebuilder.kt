package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.UnknownKey
import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder.rebuildTree
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToBoolean
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToNumbers
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion

object ExprTreeRebuilder {
    /**
     * This is just a standard expression replacer that will only be called
     * from the root of an expression tree where the key of the expression is also known.
     */
    interface ExprReplacerWithKey {
        fun <T : Any> replace(key: UnknownKey, expr: Expr<T>): Expr<T>

        companion object {
            operator fun invoke(block: (UnknownKey, Expr<*>) -> Expr<*>): ExprReplacerWithKey {
                return object : ExprReplacerWithKey {
                    override fun <T : Any> replace(key: UnknownKey, expr: Expr<T>): Expr<T> {
                        return block(key, expr).castOrThrow(expr.ind)
                    }
                }
            }
        }
    }

    /**
     * Exactly the same as [rebuildTree], but imposes the constraint that each expression's indicator must not change
     * after replacement, which in turn guarantees that the result will be of the same type as the original.
     */
    fun <T : Any> swapInplaceInTree(
        expr: Expr<T>,
        includeIfCondition: Boolean = true,
        replacer: (Expr<*>) -> Expr<*>,
    ): Expr<T> =
        rebuildTree(expr, includeIfCondition) { e -> replacer(e).castOrThrow(e.ind) }.castOrThrow(expr.ind)

    /**
     * Iterates over the entire tree, allowing you to replace any expression with a new one.
     * Verifies that the new expression is valid in whatever slot it goes in to, but it doesn't need be the same
     * type as the original.
     *
     * This performs a post-order traversal (leaves-first replacement) of the tree. This means children are
     * always fully replaced before their parents, and parents operate on the results of their children's
     * replacements.
     *
     * This can be helpful for optimizations, e.g. `(1 + 1) * 2` could be resolved to 4 in a single run.
     *
     * [includeIfCondition]: Whether to explore the condition of [IfExpr]s in the rebuild.
     */
    fun rebuildTree(expr: Expr<*>, includeIfCondition: Boolean = true, replacer: (Expr<*>) -> Expr<*>): Expr<*> {
        // Note to self: If you're adding to this, remember that you want to call `rebuildTree` recursively,
        // and not `replacer` as that gets called automatically at the end of the method. This should be
        // obvious, but I've made the mistake before so thought it would be good to note.
        val replacedExpr: Expr<*> = when (expr) {
            is BooleanInvertExpr -> BooleanInvertExpr(
                rebuildTree(expr.expr, includeIfCondition, replacer).castToBoolean()
            )

            is VarsExpr -> expr
            is LeafExpr -> expr
            is ExpressionChainPointer<*> -> expr

            is NegateExpr<*> -> NegateExpr(
                rebuildTree(expr.expr, includeIfCondition, replacer).castToNumbers()
            )

            is TypeCastExpr<*, *> -> TypeCastExpr(
                rebuildTree(expr.expr, includeIfCondition, replacer),
                expr.ind,
                expr.explicit
            )

            is ExpressionChain<*> -> {
                val newSupport = rebuildTree(expr.support, includeIfCondition, replacer)
                val newExpr =
                    rebuildTree(expr.expr, includeIfCondition, replacer).replaceTypeInTree<ExpressionChainPointer<*>> {
                        if (it.supportKey == expr.supportKey) {
                            // Change the chain pointer's indicator to match the new support indicator.
                            ExpressionChainPointer(it.supportKey, newSupport.ind)
                        } else {
                            null
                        }
                    }
                ExpressionChain(expr.supportKey, newSupport, newExpr)
            }

            is AnyBinaryExpr<*> -> {
                ConversionsAndPromotion.castAToB(
                    rebuildTree(expr.lhs, includeIfCondition, replacer),
                    rebuildTree(expr.rhs, includeIfCondition, replacer),
                    Behaviour.Throw
                ).map { lhs, rhs ->
                    when (expr) {
                        is ComparisonExpr<*> -> ComparisonExpr.new(lhs, rhs, expr.comp)
                        is UnionExpr<*> -> UnionExpr(lhs, rhs)
                        is ArithmeticExpr<*> ->
                            ConversionsAndPromotion.castAToB(lhs, rhs.castToNumbers(), Behaviour.Throw).map { l, r ->
                                ArithmeticExpr(l, r, expr.op)
                            }

                        is BooleanExpr ->
                            ConversionsAndPromotion.castAToB(lhs, rhs.castToBoolean(), Behaviour.Throw).map { l, r ->
                                BooleanExpr.newRaw(l, r, expr.op)
                            }

                        is IfExpr<*> -> IfExpr.newRaw(
                            lhs,
                            rhs,
                            if (includeIfCondition) {
                                rebuildTree(expr.thisCondition, replacer = replacer).castToBoolean()
                            } else {
                                expr.thisCondition
                            }
                        )
                    }
                }
            }
        }

        return replacer(replacedExpr)
    }
}