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
        val replacerCache = mutableMapOf<Expr<*>, Expr<*>>()

        /**
         * [isInCondition]: Whether we're currently inside an if-condition. Once set, this remains true for any
         * nested if-conditions. Just used for our recursion.
         */
        fun inner(
            expr: Expr<*>,
            isInCondition: Boolean,
            replacer: (Expr<*>) -> Expr<*>
        ): Expr<*> {
            return replacerCache.getOrPut(expr) {
                val replacedExpr: Expr<*> = when (expr) {
                    is BooleanInvertExpr -> BooleanInvertExpr.new(
                        inner(expr.expr, isInCondition, replacer).castToBoolean()
                    )

                    is VarsExpr -> expr
                    is LeafExpr<*> -> expr

                    is NegateExpr<*> -> NegateExpr.new(
                        inner(expr.expr, isInCondition, replacer).castToNumbers()
                    )

                    is TypeCastExpr<*, *> -> TypeCastExpr.new(
                        inner(expr.expr, isInCondition, replacer),
                        expr.ind,
                        expr.explicit
                    )

                    is IfExpr -> ConversionsAndPromotion.castAToB(
                        inner(expr.trueExpr, isInCondition, replacer),
                        inner(expr.falseExpr, isInCondition, replacer),
                        Behaviour.Throw
                    ).map { trueE, falseE ->
                        IfExpr.newRaw(
                            trueE,
                            falseE,
                            inner(expr.thisCondition, true, replacer).castToBoolean()
                        )
                    }

                    is TagsExpr<*> -> TagsExpr.new(
                        expr.tags.mapKeys { inner(it.key, isInCondition, replacer) },
                        inner(expr.expr, isInCondition, replacer)
                    )

                    is AnyBinaryExpr<*> -> {
                        ConversionsAndPromotion.castAToB(
                            inner(expr.lhs, isInCondition, replacer),
                            inner(expr.rhs, isInCondition, replacer),
                            Behaviour.Throw
                        ).map { lhs, rhs ->
                            when (expr) {
                                is ComparisonExpr<*> -> ComparisonExpr.new(lhs, rhs, expr.comp)
                                is UnionExpr<*> -> UnionExpr.new(lhs, rhs)
                                is ArithmeticExpr<*> ->
                                    ConversionsAndPromotion.castAToB(lhs, rhs.castToNumbers(), Behaviour.Throw)
                                        .map { l, r ->
                                            ArithmeticExpr.new(l, r, expr.op)
                                        }

                                is BooleanExpr ->
                                    ConversionsAndPromotion.castAToB(lhs, rhs.castToBoolean(), Behaviour.Throw)
                                        .map { l, r ->
                                            BooleanExpr.newRaw(l, r, expr.op)
                                        }
                            }
                        }
                    }
                }

                if ((isInCondition && ifTraversal.doCondition()) || (!isInCondition && ifTraversal.doBranches())) {
                    replacer(replacedExpr)
                } else {
                    replacedExpr
                }
            }
        }

        return inner(this, false, replacer)
    }

}