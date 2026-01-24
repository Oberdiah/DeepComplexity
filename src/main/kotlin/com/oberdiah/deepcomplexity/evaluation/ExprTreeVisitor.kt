package com.oberdiah.deepcomplexity.evaluation

object ExprTreeVisitor {
    fun <OUTPUT> reduce(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        initial: Expr<*>,
        producer: (Expr<*>) -> OUTPUT,
        combiner: (OUTPUT, OUTPUT) -> OUTPUT,
    ): OUTPUT {
        val cache = mutableMapOf<Expr<*>, OUTPUT>()
        val stack = ArrayDeque(listOf(initial))

        return generateSequence { stack.removeLastOrNull() }
            .map { e ->
                cache.getOrPut(e) {
                    visitTree(e, ifTraversal) { c -> stack.addLast(c) }
                    producer(e)
                }
            }
            .reduce(combiner)
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
            is TagsExpr<*> -> visitor(expr.expr)
        }
    }
}