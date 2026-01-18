package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle


class Tracer(
    private val path: List<Direction> = emptyList(),
    // Mutable, and the one instance is shared between tracers, which can be confusing.
    private val evaluatedStrings: MutableMap<List<Direction>, String> = mutableMapOf()
) {
    enum class Direction {
        Only,
        Left,
        Right,
        True,
        False
    }

    fun leftPath(): Tracer = direction(Direction.Left)
    fun rightPath(): Tracer = direction(Direction.Right)
    fun falsePath(): Tracer = direction(Direction.False)
    fun truePath(): Tracer = direction(Direction.True)
    fun onlyPath(): Tracer = direction(Direction.Only)

    private fun direction(direction: Direction): Tracer =
        Tracer(path + direction, evaluatedStrings)

    fun getTrace(): String {
        return if (likelyCompromised) {
            evaluatedStrings[emptyList()]!! + "\nThis trace will likely be inaccurate " +
                    "if you've performed any instruction-pointer-moving debugging. Checks-disabled tracing was enabled " +
                    "because the TEST_FILTER environment variable was set to 'go'."
        } else {
            evaluatedStrings[emptyList()]!!
        }
    }

    private val likelyCompromised: Boolean get() = System.getenv("TEST_FILTER") == "go"

    fun trace(expr: Expr<*>, bundle: Bundle<*>) {
        fun getStr(direction: Direction, fallback: Expr<*>): String {
            return evaluatedStrings.getOrElse(path + direction) {
                "$fallback = <| NOT EVALUATED |>"
            }
        }

        val myResult = "<| ${bundle.toDebugString()} |>"

        if (!likelyCompromised) {
            require(path !in evaluatedStrings) {
                "Path already evaluated: $path"
            }
        }

        evaluatedStrings[path] = when (expr) {
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
                "if ${expr.thisCondition} {\n${
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
            is ExpressionChain<*> -> {
                "${expr.supportKey} = ${expr.support}\n${getStr(Direction.Only, expr.expr)}"
            }

            is ExpressionChainPointer<*> -> expr.supportKey.toString()
        }
    }
}