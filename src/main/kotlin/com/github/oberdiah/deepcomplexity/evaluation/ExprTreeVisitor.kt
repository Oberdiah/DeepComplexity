package com.github.oberdiah.deepcomplexity.evaluation

object ExprTreeVisitor {
    fun iterateTree(expr: Expr<*>): Sequence<Expr<*>> {
        return object : Iterator<Expr<*>> {
            private val stack = mutableListOf(expr)

            override fun hasNext(): Boolean {
                return stack.isNotEmpty()
            }

            override fun next(): Expr<*> {
                val current = stack.removeAt(stack.size - 1)
                visitTree(current) {
                    stack.add(it)
                }
                return current
            }
        }.asSequence()
    }

    fun visitTree(expr: Expr<*>, visitor: (Expr<*>) -> Unit) {
        when (expr) {
            is VoidExpression -> {}
            is ConstExpr -> {}
            is VariableExpression -> {}
            is BooleanExpression -> {
                visitor(expr.lhs)
                visitor(expr.rhs)
            }

            is ComparisonExpression<*> -> {
                visitor(expr.lhs)
                visitor(expr.rhs)
            }

            is BooleanInvertExpression -> visitor(expr.expr)
            is NegateExpression -> visitor(expr.expr)
            is ArithmeticExpression -> {
                visitor(expr.lhs)
                visitor(expr.rhs)
            }

            is IfExpression -> {
                visitor(expr.trueExpr)
                visitor(expr.falseExpr)
                visitor(expr.thisCondition)
            }

            is UnionExpression -> {
                visitor(expr.lhs)
                visitor(expr.rhs)
            }

            is NumIterationTimesExpression -> visitor(expr.variable)
            is TypeCastExpression<*, *> -> visitor(expr.expr)
        }
    }
}