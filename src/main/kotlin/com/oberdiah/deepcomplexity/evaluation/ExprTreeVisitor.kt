package com.oberdiah.deepcomplexity.evaluation

object ExprTreeVisitor {
    fun <OUTPUT> reduce(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        initial: Expr<*>,
        producer: (Expr<*>) -> OUTPUT,
        combiner: (OUTPUT, OUTPUT) -> OUTPUT,
    ): OUTPUT {
        data class Frame(val e: Expr<*>, val isEntering: Boolean)

        val cache = mutableMapOf<Expr<*>, OUTPUT>()
        val stack = ArrayDeque<Frame>().apply { addLast(Frame(initial, true)) }

        while (stack.isNotEmpty()) {
            val (expr, isEntering) = stack.removeLast()
            if (expr in cache) continue

            val children = expr.subExprs(ifTraversal)
            if (isEntering) {
                stack.addLast(Frame(expr, false))
                stack.addAll(children.filter { it !in cache }.map { Frame(it, true) })
            } else {
                cache[expr] = children.fold(producer(expr)) { acc, c ->
                    combiner(acc, cache.getValue(c))
                }
            }
        }

        return cache.getValue(initial)
    }
}