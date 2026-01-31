package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate.CacheKey
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain


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

    enum class Direction {
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

    fun leftPath(): EvaluatorAssistant = direction(Direction.Left)
    fun rightPath(): EvaluatorAssistant = direction(Direction.Right)
    fun falsePath(): EvaluatorAssistant = direction(Direction.False)
    fun truePath(): EvaluatorAssistant = direction(Direction.True)
    fun onlyPath(): EvaluatorAssistant = direction(Direction.Only)

    private fun direction(direction: Direction): EvaluatorAssistant =
        EvaluatorAssistant(tagsMap, path + direction, evaluatedStrings, expressionCache, isInsideCondition)

    fun <T : Any> getOrPut(
        expr: Expr<T>,
        constraints: ExprConstrain.ConstraintsOrPile,
        evalFunc: () -> Bundle<*>
    ): Bundle<T> {
        val cacheKey = CacheKey(expr, constraints)

        return expressionCache.getOrPut(cacheKey) {
            val bundle = evalFunc()
            trace(expr, bundle)
            bundle
        }.castOrThrow(expr.ind)
    }

    fun getCacheReadout(): String {
        val cacheHitRate = String.format(
            "%.2f",
            100.0 * (1.0 - expressionCache.size.toDouble() / Double.NaN)
        );

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

        fun getStr(direction: Direction, fallback: Expr<*>): String {
            return evaluatedStrings.getOrElse(path + direction) {
                "<| NOT EVALUATED |>"
//                "${ExprToString.toStringWithTags(fallback, tagsMap)} = <| NOT EVALUATED |>"
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
                val lhsStr = getStr(Direction.Left, expr.lhs)
                val rhsStr = getStr(Direction.Right, expr.rhs)

                if (!lhsStr.contains("|>") && !rhsStr.contains("|>")) {
                    "(${lhsStr} ${expr.op} ${rhsStr}) = $myResult"
                } else {
                    "${lhsStr.prependIndent("| ")}\n" +
                            "|-> ${expr.op}\n" +
                            "${rhsStr.prependIndent("| ")}\n" +
                            "| = $myResult"
                }
            }

            is NegateExpr -> "-${getStr(Direction.Only, expr.expr)}"

            is ComparisonExpr<*> -> "(${
                getStr(Direction.Left, expr.lhs)
            } ${expr.comp} ${
                getStr(Direction.Right, expr.rhs)
            }) = $myResult"

            is ConstExpr<*> -> expr.value.toString()
            is IfExpr -> {
                "if ${ExprToString.toStringWithTags(expr.thisCondition, tagsMap)} {\n${
                    getStr(Direction.True, expr.trueExpr).prependIndent()
                }\n} else {\n${
                    getStr(Direction.False, expr.falseExpr).prependIndent()
                }\n} = $myResult"
            }

            is BooleanInvertExpr -> "!${getStr(Direction.Only, expr.expr)} = $myResult"
            is UnionExpr -> "(${
                getStr(Direction.Left, expr.lhs)
            } ∪ ${
                getStr(Direction.Right, expr.rhs)
            }) = $myResult"

            is BooleanExpr -> "(${
                getStr(Direction.Left, expr.lhs)
            } ${expr.op} ${
                getStr(Direction.Right, expr.rhs)
            })"

            is VariableExpr -> expr.key.toString()
            is TypeCastExpr<*, *> -> {
                if (expr.explicit) {
                    "(${expr.ind}) ${getStr(Direction.Only, expr.expr)}"
                } else {
                    getStr(Direction.Only, expr.expr)
                }
            }

            is VarsExpr -> "CtxExpr"
        }
    }
}