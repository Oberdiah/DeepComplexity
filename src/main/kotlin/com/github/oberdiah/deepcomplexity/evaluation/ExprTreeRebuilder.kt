package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator

object ExprTreeRebuilder {
    interface Replacer {
        fun <T : IExpr<*>> replace(expr: T): T
    }

    fun <T : IExpr<*>> rebuildTree(
        expr: T,
        replacer: Replacer
    ): T {
        return replacer.replace(
            @Suppress("UNCHECKED_CAST")
            when (expr.ind) {
                is NumberSetIndicator<*> -> rebuildTreeNums(expr.castToNumbers(), replacer) as T
                is GenericSetIndicator -> rebuildTreeGenerics(expr as IExpr<*>, replacer) as T
                BooleanSetIndicator -> rebuildTreeBooleans(expr as IExpr<Boolean>, replacer) as T
            }
        )
    }

    fun <T : Number> rebuildTreeNums(
        expr: IExpr<T>,
        replacer: Replacer
    ): IExpr<T> {
        return when (expr) {
            is ArithmeticExpression -> ArithmeticExpression(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
                expr.op
            )

            is NegateExpression -> NegateExpression(
                rebuildTree(expr.expr, replacer)
            )

            is NumIterationTimesExpression -> NumIterationTimesExpression(
                expr.constraint,
                rebuildTree(expr.variable, replacer),
                expr.terms
            )

            else -> rebuildTreeAnythings(expr, replacer)
        }
    }

    fun rebuildTreeBooleans(
        expr: IExpr<Boolean>,
        replacer: Replacer
    ): IExpr<Boolean> {
        return when (expr) {
            is BooleanExpression -> BooleanExpression(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer),
                expr.op
            )

            is BooleanInvertExpression -> BooleanInvertExpression(
                rebuildTree(expr.expr, replacer)
            )

            is ComparisonExpression<*> -> {
                fun <T : Number> extra(expr: ComparisonExpression<T>): ComparisonExpression<T> = ComparisonExpression(
                    rebuildTree(expr.lhs, replacer),
                    rebuildTree(expr.rhs, replacer),
                    expr.comp
                )
                extra(expr)
            }

            else -> rebuildTreeAnythings(expr, replacer)
        }
    }

    fun <T : Any> rebuildTreeGenerics(
        expr: IExpr<T>,
        replacer: Replacer
    ): IExpr<T> {
        return rebuildTreeAnythings(expr, replacer)
    }

    fun <T : Any> rebuildTreeAnythings(
        expr: IExpr<T>,
        replacer: Replacer
    ): IExpr<T> {
        return when (expr) {
            is UnionExpression -> UnionExpression(
                rebuildTree(expr.lhs, replacer),
                rebuildTree(expr.rhs, replacer)
            )

            is IfExpression -> IfExpression(
                rebuildTree(expr.trueExpr, replacer),
                rebuildTree(expr.falseExpr, replacer),
                rebuildTree(expr.thisCondition, replacer)
            )

            is TypeCastExpression<*, *> -> {
                fun <T : Any, Q : Any> extra(
                    expr: TypeCastExpression<T, Q>
                ): TypeCastExpression<T, Q> = TypeCastExpression(
                    rebuildTree(expr.expr, replacer),
                    expr.setInd,
                    expr.explicit
                )

                @Suppress("UNCHECKED_CAST") // Safety: We put in the same type we get out.
                extra(expr) as IExpr<T>
            }

            is ConstExpr<*> -> expr
            is VariableExpression<*> -> expr

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
    }
}