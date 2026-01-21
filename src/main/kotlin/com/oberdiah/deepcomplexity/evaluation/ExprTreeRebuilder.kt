package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.UnknownKey
import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder.replaceInTree
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
     * Exactly the same as [replaceInTree], but imposes the constraint that each expression's indicator must not change
     * after replacement, which in turn guarantees that the result will be of the same type as the original.
     */
    fun <T : Any> Expr<T>.replaceInTreeMaintainType(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        replacer: (Expr<*>) -> Expr<*>,
    ): Expr<T> =
        this.replaceInTree(ifTraversal) { e -> replacer(e).castOrThrow(e.ind) }.castOrThrow(this.ind)

    /**
     * Iterates over the tree, allowing you to replace any expression with a new one.
     * Verifies that the new expression is valid in whatever slot it goes in to, but it doesn't need to be the same
     * type as the original.
     *
     * This performs a post-order traversal (leaves-first replacement) of the tree. This means children are
     * always fully replaced before their parents, and parents operate on the results of their children's
     * replacements.
     *
     * This can be helpful for optimizations, e.g. `(1 + 1) * 2` could be resolved to 4 in a single run.
     *
     * **Notes of caution**:
     *  - [replacer] must always return the same expression given the same input within the same call.
     *  - You should not rely on [replaceInTree] calling [replacer] on every expression in the tree, it may cache and
     * re-use results to avoid re-evaluation.
     *
     * [ifTraversal]: What to do when encountering an IfExpr.
     */
    fun <T : Any> Expr<T>.replaceInTree(
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
                    if (e is ExpressionChainPointer && e.supportKey == expr.supportKey) {
                        var support = supports[innerInCondition]

                        if (support == null) {
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
}