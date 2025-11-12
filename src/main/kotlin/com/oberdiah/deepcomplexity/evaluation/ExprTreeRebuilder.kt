package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToNumbers
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator

object ExprTreeRebuilder {
    /**
     * `ind` is the indicator of the type that the expression will be converted to.
     */
    class LeafReplacer<T : Any>(val ind: SetIndicator<T>, val replacer: (Expr<*>) -> Expr<T>)

    /**
     * There is a reason to have both this and `rebuildTree`.
     *
     * This is to be used when you want to swap the type of the whole expression by changing its leaves.
     * If you don't want to change the type, use `rebuildTree` instead as it's more flexible (it can replace
     * leaves too, just can't change their type).
     *
     * This replacement only works on a subset of all expressions (only expressions where every node can
     * take any type, so no `ArithmeticExpression`, `ComparisonExpression`, etc.).
     */
    fun <T : Any> replaceTreeLeaves(
        expr: Expr<*>,
        replacer: LeafReplacer<T>
    ): Expr<T> {
        return when (expr) {
            is UnionExpr -> UnionExpr(
                replaceTreeLeaves(expr.lhs, replacer),
                replaceTreeLeaves(expr.rhs, replacer),
            )

            is IfExpr -> IfExpr.newRaw(
                replaceTreeLeaves(expr.trueExpr, replacer),
                replaceTreeLeaves(expr.falseExpr, replacer),
                expr.thisCondition,
            )

            is TypeCastExpr<*, *> -> {
                fun <T : Any, Q : Any> extra(
                    expr: TypeCastExpr<T, Q>
                ): TypeCastExpr<T, *> {
                    return TypeCastExpr(
                        replaceTreeLeaves(expr.expr, replacer),
                        expr.ind,
                        expr.explicit,
                    )
                }

                @Suppress("UNCHECKED_CAST") // Safety: We put in the same type we get out.
                extra(expr) as Expr<T>
            }

            is LeafExpr<*> -> replacer.replacer(expr)
            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
    }

    interface Replacer {
        fun <T : Any> replace(expr: Expr<T>): Expr<T>
    }

    /**
     * Rebuilds the expression tree, replacing nodes using the given [Replacer].
     *
     * This performs a post-order traversal (bottom-up replacement) of the tree. This means children are
     * always fully replaced before their parents, and parents operate on the results of their children's
     * replacements.
     *
     * This can be very helpful for optimisations, e.g. `(1 + 1) * 2` could be resolved to 4 in a single run.
     *
     * [includeIfCondition]: Whether to explore the condition of [IfExpr]s in the rebuild.
     */
    fun <T : Any> rebuildTree(
        expr: Expr<T>,
        replacer: Replacer,
        includeIfCondition: Boolean = true
    ): Expr<T> {
        @Suppress("UNCHECKED_CAST")
        val rebuiltExpr = when (expr.ind) {
            is NumberSetIndicator<*> -> rebuildTreeNums(expr.castToNumbers(), replacer, includeIfCondition) as Expr<T>
            is ObjectSetIndicator -> rebuildTreeGenerics(expr as Expr<*>, replacer, includeIfCondition) as Expr<T>
            BooleanSetIndicator -> rebuildTreeBooleans(expr as Expr<Boolean>, replacer, includeIfCondition) as Expr<T>
        }

        return replacer.replace(rebuiltExpr)
    }

    private fun <T : Number> rebuildTreeNums(
        expr: Expr<T>,
        replacer: Replacer,
        includeIfCondition: Boolean
    ): Expr<T> {
        return when (expr) {
            is ArithmeticExpr -> ArithmeticExpr(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
                expr.op,
            )

            is NegateExpr -> NegateExpr(
                rebuildTree(expr.expr, replacer),
            )

            is NumIterationTimesExpr -> NumIterationTimesExpr(
                expr.constraint,
                rebuildTree(expr.variable, replacer) as VariableExpr<T>,
                expr.terms,
            )

            else -> rebuildTreeAnythings(expr, replacer, includeIfCondition)
        }
    }

    private fun rebuildTreeBooleans(
        expr: Expr<Boolean>,
        replacer: Replacer,
        includeIfCondition: Boolean
    ): Expr<Boolean> {
        return when (expr) {
            is BooleanExpr -> BooleanExpr.newRaw(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
                expr.op,
            )

            is BooleanInvertExpr -> BooleanInvertExpr(
                rebuildTree(expr.expr, replacer),
            )

            is ComparisonExpr<*> -> {
                fun <T : Any> extra(expr: ComparisonExpr<T>): Expr<Boolean> = ComparisonExpr.newRaw(
                    rebuildTree(expr.lhs, replacer),
                    rebuildTree(expr.rhs, replacer),
                    expr.comp,
                )
                extra(expr)
            }

            else -> rebuildTreeAnythings(expr, replacer, includeIfCondition)
        }
    }

    private fun <T : Any> rebuildTreeGenerics(
        expr: Expr<T>,
        replacer: Replacer,
        includeIfCondition: Boolean
    ): Expr<T> {
        return rebuildTreeAnythings(expr, replacer, includeIfCondition)
    }

    private fun <T : Any> rebuildTreeAnythings(
        expr: Expr<T>,
        replacer: Replacer,
        includeIfCondition: Boolean
    ): Expr<T> {
        return when (expr) {
            is UnionExpr -> UnionExpr(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
            )

            is IfExpr -> IfExpr.newRaw(
                rebuildTree(expr.trueExpr, replacer),
                rebuildTree(expr.falseExpr, replacer),
                if (includeIfCondition) rebuildTree(
                    expr.thisCondition,
                    replacer
                ) else expr.thisCondition,
            )

            is TypeCastExpr<*, *> -> TypeCastExpr(
                rebuildTree(expr.expr, replacer),
                expr.ind,
                expr.explicit,
            )

            is LValueFieldExpr<*> -> {
                val newFieldExpr = LValueFieldExpr.new(
                    expr.field,
                    rebuildTree(expr.qualifier, replacer),
                )
                // Safety: RebuildTree doesn't change types.
                @Suppress("UNCHECKED_CAST")
                newFieldExpr as Expr<T>
            }

            is ContextExpr -> expr
            is LeafExpr -> expr
            is LValueExpr -> expr

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
    }
}