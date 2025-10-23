package com.github.oberdiah.deepcomplexity.evaluation

object ExprTreeVisitor {
    fun iterateTree(expr: Expr<*>, includeIfCondition: Boolean = true): Sequence<Expr<*>> {
        return object : Iterator<Expr<*>> {
            private val stack = mutableListOf(expr)

            override fun hasNext(): Boolean {
                return stack.isNotEmpty()
            }

            override fun next(): Expr<*> {
                val current = stack.removeAt(stack.size - 1)
                visitTree(current, includeIfCondition) {
                    stack.add(it)
                }
                return current
            }
        }.asSequence()
    }

    fun visitTree(expr: Expr<*>, includeIfCondition: Boolean = true, visitor: (Expr<*>) -> Unit) = when (expr) {
        is BooleanExpr -> {
            visitor(expr.lhs)
            visitor(expr.rhs)
        }

        is ComparisonExpr<*> -> {
            visitor(expr.lhs)
            visitor(expr.rhs)
        }

        is BooleanInvertExpr -> visitor(expr.expr)
        is NegateExpr -> visitor(expr.expr)
        is ArithmeticExpr -> {
            visitor(expr.lhs)
            visitor(expr.rhs)
        }

        is IfExpr -> {
            if (includeIfCondition) {
                visitor(expr.thisCondition)
            }

            visitor(expr.trueExpr)
            visitor(expr.falseExpr)
        }

        is UnionExpr -> {
            visitor(expr.lhs)
            visitor(expr.rhs)
        }

        is NumIterationTimesExpr -> visitor(expr.variable)
        is TypeCastExpr<*, *> -> visitor(expr.expr)
        is LValueFieldExpr<*> -> visitor(expr.qualifier)
        is DynamicExpr -> {}
        is LeafExpr<*> -> {}
        is LValueExpr<*> -> {}
    }
}