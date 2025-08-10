package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator

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
     * take any type, so no `ArithmeticExpression`, `ComparisonExpression`, etc.),
     */
    fun <T : Any> replaceTreeLeaves(
        expr: Expr<*>,
        replacer: LeafReplacer<T>
    ): Expr<T> {
        return when (expr) {
            is UnionExpression -> UnionExpression(
                replaceTreeLeaves(expr.lhs, replacer),
                replaceTreeLeaves(expr.rhs, replacer),
            )

            is IfExpression -> IfExpression(
                replaceTreeLeaves(expr.trueExpr, replacer),
                replaceTreeLeaves(expr.falseExpr, replacer),
                expr.thisCondition,
            )

            is TypeCastExpression<*, *> -> {
                fun <T : Any, Q : Any> extra(
                    expr: TypeCastExpression<T, Q>
                ): TypeCastExpression<T, *> {
                    return TypeCastExpression(
                        replaceTreeLeaves(expr.expr, replacer),
                        expr.setInd,
                        expr.explicit,
                    )
                }

                @Suppress("UNCHECKED_CAST") // Safety: We put in the same type we get out.
                extra(expr) as Expr<T>
            }

            is ConstExpr -> replacer.replacer(expr)
            is VariableExpression -> replacer.replacer(expr)
            is ObjectExpression -> replacer.replacer(expr)

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
    }

    interface Replacer {
        fun <T : Any> replace(expr: Expr<T>): Expr<T>
    }

    fun <T : Any> rebuildTree(
        expr: Expr<T>,
        replacer: Replacer
    ): Expr<T> {
        return replacer.replace(
            @Suppress("UNCHECKED_CAST")
            when (expr.ind) {
                is NumberSetIndicator<*> -> rebuildTreeNums(expr.castToNumbers(), replacer) as Expr<T>
                is GenericSetIndicator -> rebuildTreeGenerics(expr as Expr<*>, replacer) as Expr<T>
                BooleanSetIndicator -> rebuildTreeBooleans(expr as Expr<Boolean>, replacer) as Expr<T>
            }
        )
    }

    fun <T : Number> rebuildTreeNums(
        expr: Expr<T>,
        replacer: Replacer
    ): Expr<T> {
        return when (expr) {
            is ArithmeticExpression -> ArithmeticExpression(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
                expr.op,
            )

            is NegateExpression -> NegateExpression(
                rebuildTree(expr.expr, replacer),
            )

            is NumIterationTimesExpression -> NumIterationTimesExpression(
                expr.constraint,
                rebuildTree(expr.variable, replacer) as VariableExpression<T>,
                expr.terms,
            )

            else -> rebuildTreeAnythings(expr, replacer)
        }
    }

    fun rebuildTreeBooleans(
        expr: Expr<Boolean>,
        replacer: Replacer
    ): Expr<Boolean> {
        return when (expr) {
            is BooleanExpression -> BooleanExpression(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
                expr.op,
            )

            is BooleanInvertExpression -> BooleanInvertExpression(
                rebuildTree(expr.expr, replacer),
            )

            is ComparisonExpression<*> -> {
                fun <T : Number> extra(expr: ComparisonExpression<T>): ComparisonExpression<T> = ComparisonExpression(
                    rebuildTree(expr.lhs, replacer),
                    rebuildTree(expr.rhs, replacer),
                    expr.comp,
                )
                extra(expr)
            }

            else -> rebuildTreeAnythings(expr, replacer)
        }
    }

    fun <T : Any> rebuildTreeGenerics(
        expr: Expr<T>,
        replacer: Replacer
    ): Expr<T> {
        return rebuildTreeAnythings(expr, replacer)
    }

    fun <T : Any> rebuildTreeAnythings(
        expr: Expr<T>,
        replacer: Replacer
    ): Expr<T> {
        return when (expr) {
            is UnionExpression -> UnionExpression(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
            )

            is IfExpression -> IfExpression(
                rebuildTree(expr.trueExpr, replacer),
                rebuildTree(expr.falseExpr, replacer),
                rebuildTree(expr.thisCondition, replacer),
            )

            is TypeCastExpression<*, *> -> {
                fun <T : Any, Q : Any> extra(
                    expr: TypeCastExpression<T, Q>
                ): TypeCastExpression<T, Q> = TypeCastExpression(
                    rebuildTree(expr.expr, replacer),
                    expr.setInd,
                    expr.explicit,
                )

                @Suppress("UNCHECKED_CAST") // Safety: We put in the same type we get out.
                extra(expr) as Expr<T>
            }

            is LValueFieldExpr<*> -> LValueFieldExpr(
                expr.field,
                rebuildTree(expr.qualifier, replacer),
            )

            is ConstExpr -> expr
            is VariableExpression -> expr
            is ObjectExpression -> expr
            is LValueExpr -> expr

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
    }
}