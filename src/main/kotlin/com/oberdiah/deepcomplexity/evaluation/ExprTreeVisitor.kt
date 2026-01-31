package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.utilities.Utilities.decrementCount
import com.oberdiah.deepcomplexity.utilities.Utilities.incrementCount

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
        for (parent in reachable) {
            for (child in parent.directSubExprs) {
                indegree.incrementCount(child)
            }
        }

        val stillToProcess = ArrayDeque(indegree.filterValues { it == 0 }.keys)

        val resultingList = buildList(reachable.size) {
            while (true) {
                val expr = stillToProcess.removeFirstOrNull() ?: break
                add(expr)
                for (child in expr.directSubExprs) {
                    if (indegree.decrementCount(child) == 0) {
                        stillToProcess.addLast(child)
                    }
                }
            }
        }

        require(resultingList.size == reachable.size) {
            "Graph contains a cycle; topological ordering does not exist."
        }

        return resultingList
    }
}