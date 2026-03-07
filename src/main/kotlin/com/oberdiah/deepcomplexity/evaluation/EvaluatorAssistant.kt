package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate.CacheKey
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile


class EvaluatorAssistant(
    private val tagsMap: TagsMap,
    private val path: List<Direction>,
    // Mutable, and the one instance is shared between tracers, which can be confusing.
    private val evaluatedStrings: MutableMap<List<Direction>, String>,
    private val expressionCache: MutableMap<CacheKey, Bundle<*>>,
    private val isInsideCondition: Boolean,
) {
    companion object {
        fun createInitial(tagsMap: TagsMap): EvaluatorAssistant =
            EvaluatorAssistant(
                tagsMap,
                emptyList(),
                mutableMapOf(),
                mutableMapOf(),
                isInsideCondition = false
            )
    }

    interface Direction

    data class DirectionInt(val int: Int) : Direction
    enum class DirectionEnum : Direction {
        Only,
        Left,
        Right,
        True,
        False
    }

    /**
     * Call when the evaluator has just entered a condition.
     * This will turn off tracing, which isn't surfaced for conditions anyway.
     */
    fun enteredCondition(): EvaluatorAssistant =
        EvaluatorAssistant(tagsMap, path, evaluatedStrings, expressionCache, true)

    fun leftPath(): EvaluatorAssistant = direction(DirectionEnum.Left)
    fun rightPath(): EvaluatorAssistant = direction(DirectionEnum.Right)
    fun falsePath(): EvaluatorAssistant = direction(DirectionEnum.False)
    fun truePath(): EvaluatorAssistant = direction(DirectionEnum.True)
    fun onlyPath(): EvaluatorAssistant = direction(DirectionEnum.Only)

    private fun direction(direction: Direction): EvaluatorAssistant =
        EvaluatorAssistant(tagsMap, path + direction, evaluatedStrings, expressionCache, isInsideCondition)

    fun <T : Any> getOrPut(
        expr: Expr<T>,
        constraints: ConstraintsOrPile,
        evalFunc: () -> Bundle<*>
    ): Bundle<T> {
        // Note:
        // It is not correct to trivially remove any of these constraints before creating this [CacheKey]
        // if the constraint's key in question does not appear in [expr] at all. This is because
        // we attach constraints to values even when they appear irrelevant at the time, in the chance
        // that they'll appear later somewhere else.
        // e.g.
        // ```
        // int i = 0;
        // if (x > 5) {
        //      i = 3;
        // }
        // ```
        // the '3' here is imbued with [x > 5]'ness even though it has nothing to do with it.
        // This may be possible to solve, but you'll need to be more thorough about it, probably
        // ending up with a system more similar to how the Scope ExprKey filter used to work
        // before we removed it.
        val cacheKey = CacheKey(expr, constraints)

        return expressionCache.getOrPut(cacheKey) {
            val bundle = evalFunc()
            trace(expr, bundle)
            bundle
        }.coerceTo(expr.ind)
    }

    fun getCacheReadout(): String {
        val cacheHitRate = String.format(
            "%.2f",
            100.0 * (1.0 - expressionCache.size.toDouble() / Double.NaN)
        )

        return "Expressions evaluated: ${expressionCache.size}" +
                " out of ${Double.NaN} total," +
                " cache hit rate: $cacheHitRate%"
    }

    fun getTrace(): String {
        if (isInsideCondition) return "<| IS INSIDE CONDITION |>"

        val mainStr = "${ExpressionTagger.tagsToString(tagsMap)}\n${evaluatedStrings[emptyList()]!!}"

        return if (likelyCompromised) {
            mainStr + "\nThe tracer's sanity checks were disabled because the TEST_FILTER" +
                    " environment variable was set to 'go',\nso this trace will likely be inaccurate " +
                    "if you've performed any instruction-pointer-moving debugging."
        } else {
            mainStr
        }
    }

    private val likelyCompromised: Boolean get() = System.getenv("TEST_FILTER") == "go"

    fun trace(expr: Expr<*>, bundle: Bundle<*>) {
        if (isInsideCondition) return

        fun getStr(direction: DirectionEnum, fallback: Expr<*>): String {
            return evaluatedStrings.getOrElse(path + direction) {
//                "<| NOT EVALUATED |>"
                "${ExprToString.toStringWithTags(fallback, tagsMap)} = <| NOT EVALUATED |>"
            }
        }

        val myResult = "<| ${bundle.toDebugString()} |>"

        if (!likelyCompromised) {
            require(path !in evaluatedStrings) {
                "Path already evaluated: $path"
            }
        }

        evaluatedStrings[path] = tagsMap[expr] ?: when (expr) {
            is ArithmeticExpr -> {
                val lhsStr = getStr(DirectionEnum.Left, expr.lhs)
                val rhsStr = getStr(DirectionEnum.Right, expr.rhs)

                if (!lhsStr.contains("|>") && !rhsStr.contains("|>")) {
                    "(${lhsStr} ${expr.op} ${rhsStr}) = $myResult"
                } else {
                    "${lhsStr.prependIndent("| ")}\n" +
                            "|-> ${expr.op}\n" +
                            "${rhsStr.prependIndent("| ")}\n" +
                            "| = $myResult"
                }
            }

            is NegateExpr -> "-${getStr(DirectionEnum.Only, expr.expr)}"

            is ComparisonExpr<*> -> "(${
                getStr(DirectionEnum.Left, expr.lhs)
            } ${expr.comp} ${
                getStr(DirectionEnum.Right, expr.rhs)
            }) = $myResult"

            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                "if ${ExprToString.toStringWithTags(expr.thisCondition, tagsMap)} {\n${
                    getStr(DirectionEnum.True, expr.trueExpr).prependIndent()
                }\n} else {\n${
                    getStr(DirectionEnum.False, expr.falseExpr).prependIndent()
                }\n} = $myResult"
            }

            is BooleanInvertExpr -> "!${getStr(DirectionEnum.Only, expr.expr)} = $myResult"

            is BooleanOpExpr -> "(${
                getStr(DirectionEnum.Left, expr.lhs)
            } ${expr.op} ${
                getStr(DirectionEnum.Right, expr.rhs)
            })"

            is VariableExpr -> expr.key.toString()
            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${getStr(DirectionEnum.Only, expr.expr)}"
                } else {
                    getStr(DirectionEnum.Only, expr.expr)
                }
            }

            is VarsExpr -> "CtxExpr"
            is LoopExpr<*> -> {
                // todo loops
                val target = expr.target
                // Obviously, not implemented with evaluation values in mind at the moment, this
                // is just to get us off the ground.
                val condition = ExprToString.toStringWithTags(expr.condition, tagsMap)
                val variables = expr.variables.entries.joinToString("\n") { (key, value) ->
                    "$key: { initial: ${ExprToString.toStringWithTags(value.initial, tagsMap)}, next: ${
                        ExprToString.toStringWithTags(value.update, tagsMap)
                    } }"
                }

                "Loop(\n  target: $target\n  condition: $condition\n  variables: {\n${
                    variables.prependIndent("    ")
                }\n  }\n)"
            }

            is LoopExpr.LoopLeaf<*> -> "${expr.key}"
        }
    }
}