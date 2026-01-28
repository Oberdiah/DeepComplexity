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
    
    fun getTopologicalOrdering(root: Expr<*>): List<Expr<*>> {
        val reachable = root.recursiveSubExprs
        val indegree = reachable.associateWith { 0 }.toMutableMap()
        reachable.forEach { parent ->
            parent.directSubExprs
                .forEach { child ->
                    indegree.merge(child, 1, Int::plus)
                }
        }

        val q = ArrayDeque<Expr<*>>().apply {
            indegree
                .filter { (_, deg) -> deg == 0 }
                .map { (n, _) -> n }
                .forEach(::addLast)
        }

        val topoParent = buildList(reachable.size) {
            while (q.isNotEmpty()) {
                val n = q.removeFirst()
                add(n)

                n.directSubExprs
                    .forEach { child ->
                        val newDeg = indegree.getValue(child) - 1
                        indegree[child] = newDeg
                        if (newDeg == 0) q.addLast(child)
                    }
            }
        }

        require(topoParent.size == reachable.size) {
            "Graph contains a cycle; topological ordering does not exist."
        }

        return topoParent
    }
}