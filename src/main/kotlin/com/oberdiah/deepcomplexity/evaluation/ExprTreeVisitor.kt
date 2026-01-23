package com.oberdiah.deepcomplexity.evaluation

object ExprTreeVisitor {
    fun iterateTree(expr: Expr<*>, ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches): Sequence<Expr<*>> {
        return object : Iterator<Expr<*>> {
            private val stack = mutableListOf(expr)

            override fun hasNext(): Boolean {
                return stack.isNotEmpty()
            }

            override fun next(): Expr<*> {
                val current = stack.removeAt(stack.size - 1)
                visitTree(current, ifTraversal) {
                    stack.add(it)
                }
                return current
            }
        }.asSequence()
    }

    fun visitTree(
        expr: Expr<*>,
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        visitor: (Expr<*>) -> Unit
    ) {
        when (expr) {
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
                if (ifTraversal.doCondition()) {
                    visitor(expr.thisCondition)
                }
                if (ifTraversal.doBranches()) {
                    visitor(expr.trueExpr)
                    visitor(expr.falseExpr)
                }
            }

            is UnionExpr -> {
                visitor(expr.lhs)
                visitor(expr.rhs)
            }

            is TypeCastExpr<*, *> -> visitor(expr.expr)
            is VarsExpr -> {}
            is LeafExpr<*> -> {}
        }
    }
}