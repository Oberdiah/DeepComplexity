package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.UnknownKey
import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder.rewriteInTree
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToBoolean
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToNumbers
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion

enum class IfTraversal {
    ConditionAndBranches,
    BranchesOnly,
    ConditionOnly;

    fun doCondition(): Boolean = this == ConditionAndBranches || this == ConditionOnly
    fun doBranches(): Boolean = this == ConditionAndBranches || this == BranchesOnly
}

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
     * Variant of [rewriteInTree] that enforces indicator preservation.
     *
     * For every expression passed to [replacer], the returned expression must have the same indicator
     * as its input. This constraint is checked at runtime and guarantees that the returned tree has
     * the same static type as the receiver.
     *
     * All traversal and caching behaviour is identical to [rewriteInTree].
     */
    fun <T : Any> Expr<T>.rewriteInTreeSameType(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        replacer: (Expr<*>) -> Expr<*>,
    ): Expr<T> =
        this.rewriteInTree(ifTraversal) { e -> replacer(e).castOrThrow(e.ind) }.castOrThrow(this.ind)

    /**
     * Rebuilds the expression tree using a post-order (leaves-first) traversal.
     *
     * Each visited expression may be replaced with another expression that is valid in the same slot.
     * The replacement is not required to preserve the original expression’s indicator.
     *
     * Rewrite results may be cached. As a consequence:
     *  - [replacer] may be invoked fewer times than there are occurrences in the tree.
     *  - [replacer] must act as a pure function, with no behavioural change based on external state.
     *  - You must not rely on occurrence counts or traversal-order side effects.
     *
     * Return the same expression to indicate “no change”.
     *
     * [ifTraversal] controls how IfExpr nodes are traversed.
     */
    fun <T : Any> Expr<T>.rewriteInTree(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        replacer: (Expr<*>) -> Expr<*>,
    ): Expr<*> {
        // OldExpr -> NewExpr
        val replacerCache = mutableMapOf<Expr<*>, Expr<*>>()
        // NewExpr -> Count
        val replacementCounts = mutableMapOf<Expr<*>, Int>()

        iterateTree(ifTraversal).forEach { oldExpr ->
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

        val replacedExpr = internalRewriteInTree(ifTraversal) { oldExpr ->
            val newExpr = replacerCache[oldExpr] ?: return@internalRewriteInTree oldExpr
            chainsToGenerate[newExpr]?.let {
                ExpressionChainPointer.new(it, newExpr.ind)
            } ?: newExpr
        }

        return chainsToGenerate.entries.fold(replacedExpr) { acc, (expr, key) ->
            ExpressionChain.new(key, expr, acc)
        }
    }

    private fun <T : Any> Expr<T>.internalRewriteInTree(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        replacer: (Expr<*>) -> Expr<*>,
    ): Expr<*> = rebuildTreeInner(this, false) { e, isInCondition ->
        if ((isInCondition && ifTraversal.doCondition()) || (!isInCondition && ifTraversal.doBranches())) {
            replacer(e)
        } else {
            e
        }
    }

    /**
     * [isInCondition]: Whether we're currently inside an if-condition. Once set, this remains true for any
     * nested if-conditions. Just used for our recursion, it isn't part of the public API.
     */
    private fun rebuildTreeInner(
        expr: Expr<*>,
        isInCondition: Boolean,
        replacer: (Expr<*>, Boolean) -> Expr<*>
    ): Expr<*> {
        // Note to self: If you're adding to this, remember that you want to call `rebuildTree` recursively,
        // and not `replacer` as that gets called automatically at the end of the method. This should be
        // obvious, but I've made the mistake before so thought it would be good to note.
        val replacedExpr: Expr<*> = when (expr) {
            is BooleanInvertExpr -> BooleanInvertExpr.new(
                rebuildTreeInner(expr.expr, isInCondition, replacer).castToBoolean()
            )

            is VarsExpr -> expr
            is LeafExpr<*> -> expr

            is NegateExpr<*> -> NegateExpr.new(
                rebuildTreeInner(expr.expr, isInCondition, replacer).castToNumbers()
            )

            is TypeCastExpr<*, *> -> TypeCastExpr.new(
                rebuildTreeInner(expr.expr, isInCondition, replacer),
                expr.ind,
                expr.explicit
            )

            is ExpressionChain<*> -> {
                // A map from whether we're in a condition to the support expression for that condition.
                val supports = mutableMapOf<Boolean, Pair<SupportKey, Expr<*>>>()

                // So this is a nightmare because we may be in a situation where we request to rebuild
                // the branches of an if, but not its condition. In such a case, it's possible that a chain's
                // pointers span both. In that case we need to split the expression chain in half — one to
                // support the pointers in the conditions and one to support the pointers in the branches.
                var newExpr = rebuildTreeInner(expr.expr, isInCondition) { e, innerInCondition ->
                    // If it's our pointer...
                    if (e is ExpressionChainPointer && e.supportKey == expr.supportKey) {
                        // ... check if we've encountered a pointer in this situation before...
                        var support = supports[innerInCondition]

                        if (support == null) {
                            // ... if not, we perform its rebuild, which we need to do.
                            // Note to self: This is really inefficient, we end up traversing the support at
                            // every support location worst-case.
                            val newExpr = rebuildTreeInner(expr.support, innerInCondition, replacer)
                            val otherSupport = supports[!innerInCondition]
                            if (otherSupport != null && otherSupport.second == newExpr) {
                                support = otherSupport
                            } else {
                                support = Pair(SupportKey.new("Chain $innerInCondition"), newExpr)
                                supports[innerInCondition] = support
                            }
                        }

                        ExpressionChainPointer.new(support.first, support.second.ind)
                    } else {
                        replacer(e, innerInCondition)
                    }
                }

                for ((_, pair) in supports) {
                    val (supportKey, support) = pair
                    newExpr = ExpressionChain.new(supportKey, support, newExpr)
                }

                newExpr
            }

            is ExpressionChainPointer<*> -> expr

            is IfExpr -> {
                ConversionsAndPromotion.castAToB(
                    rebuildTreeInner(expr.trueExpr, isInCondition, replacer),
                    rebuildTreeInner(expr.falseExpr, isInCondition, replacer),
                    Behaviour.Throw
                ).map { trueE, falseE ->
                    IfExpr.newRaw(
                        trueE,
                        falseE,
                        rebuildTreeInner(expr.thisCondition, true, replacer).castToBoolean()
                    )
                }
            }

            is AnyBinaryExpr<*> -> {
                ConversionsAndPromotion.castAToB(
                    rebuildTreeInner(expr.lhs, isInCondition, replacer),
                    rebuildTreeInner(expr.rhs, isInCondition, replacer),
                    Behaviour.Throw
                ).map { lhs, rhs ->
                    when (expr) {
                        is ComparisonExpr<*> -> ComparisonExpr.new(lhs, rhs, expr.comp)
                        is UnionExpr<*> -> UnionExpr.new(lhs, rhs)
                        is ArithmeticExpr<*> ->
                            ConversionsAndPromotion.castAToB(lhs, rhs.castToNumbers(), Behaviour.Throw).map { l, r ->
                                ArithmeticExpr.new(l, r, expr.op)
                            }

                        is BooleanExpr ->
                            ConversionsAndPromotion.castAToB(lhs, rhs.castToBoolean(), Behaviour.Throw).map { l, r ->
                                BooleanExpr.newRaw(l, r, expr.op)
                            }
                    }
                }
            }
        }

        return replacer(replacedExpr, isInCondition)
    }

    fun withChainsAtBottom(expr: Expr<*>): Expr<*> {
        val removed = mutableSetOf<ExpressionChain<*>>()
        val newExpr = expr.internalRewriteInTree { expr ->
            if (expr is ExpressionChain<*>) {
                removed.add(expr)
                expr.expr
            } else {
                expr
            }
        }

        return removed.fold(newExpr) { acc, chain ->
            ExpressionChain.new(chain.supportKey, chain.support, acc)
        }
    }
}