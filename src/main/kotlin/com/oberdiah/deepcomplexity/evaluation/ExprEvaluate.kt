package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.VariableKey
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToBoolean
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToNumbers
import com.oberdiah.deepcomplexity.solver.CastSolver
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.VarsIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.*
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT

object ExprEvaluate {
    data class CacheKey(val expr: Expr<*>, val constraints: ExprConstrain.ConstraintsOrPile)

    val expressionCache = mutableMapOf<CacheKey, Bundle<*>>()
    var totalEvaluatesAttempted = 0

    fun <T : Any> evaluate(
        expr: Expr<T>,
        constraints: ExprConstrain.ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        totalEvaluatesAttempted++
        val cacheKey = CacheKey(expr, constraints)

        val result = expressionCache.getOrPut(cacheKey) {
            val evaluatedBundle = when (expr.ind) {
                is NumberIndicator<*> -> {
                    // Split into two lines for nicer debugging
                    val castExpr = expr.castToNumbers()
                    evaluateNums(castExpr, constraints, assistant)
                }

                is ObjectIndicator -> evaluateGenerics(expr as Expr<*>, constraints, assistant)
                BooleanIndicator -> {
                    val castExpr = expr.castToBoolean()
                    evaluateBools(castExpr, constraints, assistant)
                }

                VarsIndicator -> WONT_IMPLEMENT()
            }

            assistant.trace(expr, evaluatedBundle)

            evaluatedBundle
        }

        return result.castOrThrow(expr.ind)
    }

    private fun <T : Number> evaluateNums(
        expr: Expr<T>,
        constraints: ExprConstrain.ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        val toReturn = when (expr) {
            is ArithmeticExpr -> {
                val lhs = evaluate(expr.lhs, constraints, assistant.leftPath())
                val rhs = evaluate(expr.rhs, constraints, assistant.rightPath())

                lhs.arithmeticOperation(rhs, expr.op, expr.exprKey)
            }

            is NegateExpr -> evaluate(expr.expr, constraints, assistant.onlyPath()).negate()

            else -> evaluateAnythings(expr, constraints, assistant)
        }
        return toReturn
    }

    private fun evaluateBools(
        expr: Expr<Boolean>,
        constraints: ExprConstrain.ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<Boolean> {
        val toReturn = when (expr) {
            is BooleanExpr -> {
                val lhs = evaluate(expr.lhs, constraints, assistant.leftPath())
                val rhs = evaluate(expr.rhs, constraints, assistant.rightPath())

                lhs.booleanOperation(rhs, expr.op, expr.exprKey)
            }

            is ComparisonExpr<*> -> {
                fun <T : Any> evalC(
                    expr: ComparisonExpr<T>,
                    constraints: ExprConstrain.ConstraintsOrPile
                ): Bundle<Boolean> {
                    val lhs = evaluate(expr.lhs, constraints, assistant.leftPath())
                    val rhs = evaluate(expr.rhs, constraints, assistant.rightPath())

                    return lhs.comparisonOperation(rhs, expr.comp, expr.exprKey)
                }
                evalC(expr, constraints)
            }

            is BooleanInvertExpr -> evaluate(expr, constraints, assistant.onlyPath()).booleanInvert()
            else -> evaluateAnythings(expr, constraints, assistant)
        }

        return toReturn
    }

    private fun <T : Any> evaluateGenerics(
        expr: Expr<T>,
        constraints: ExprConstrain.ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        return evaluateAnythings(expr, constraints, assistant)
    }

    private fun <T : Any> evaluateAnythings(
        expr: Expr<T>,
        constraints: ExprConstrain.ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        val toReturn: Bundle<T> = when (expr) {
            is UnionExpr -> evaluate(expr.lhs, constraints, assistant.leftPath()).union(
                evaluate(expr.rhs, constraints, assistant.rightPath())
            )

            is IfExpr -> {
                // Useful note for future reference:
                // A good way to think of this is constraints in towards the leaves, bundles back out again.
                // So, we create constraints here, and they percolate into the nested expressions, collecting
                // with other expressions on the way. Then, they reach the centre and get applied
                // to constants and variables to create bundles.
                // Those bundles then wander out again, operating on each other as they do so.
                // On the way in, we represent constraints as a ConstraintsOrPile, a set of constraints
                // combined by 'OR'.
                // On the way out, we represent constraints inside a bundle, which is very similar to a
                // ConstraintsOrPile except it has a set of values for the variable attached to each entry in the
                // OR set.

                val ifCondition = expr.thisCondition

                val trueConstraints = constraints.and(
                    ExprConstrain.getConstraints(ifCondition, constraints, assistant.enteredCondition())
                )
                val falseConstraints = constraints.and(
                    ExprConstrain.getConstraints(
                        BooleanInvertExpr.new(ifCondition),
                        constraints,
                        assistant.enteredCondition()
                    )
                )

                if (falseConstraints.unreachable) {
                    evaluate(expr.trueExpr, trueConstraints, assistant.truePath())
                } else if (trueConstraints.unreachable) {
                    evaluate(expr.falseExpr, falseConstraints, assistant.falsePath())
                } else {
                    val trueValue = evaluate(expr.trueExpr, trueConstraints, assistant.truePath())
                    val falseValue = evaluate(expr.falseExpr, falseConstraints, assistant.falsePath())
                    trueValue.union(falseValue)
                }
            }

            is TypeCastExpr<*, *> -> {
                val toCast = evaluate(expr.expr, constraints, assistant.onlyPath())
                CastSolver.castFrom(toCast, expr.ind, expr.explicit)
            }

            is ConstExpr -> Bundle.unconstrained(expr.ind.newConstantSet(expr.value).toConstVariance())
                .constrainWith(constraints)

            is VariableExpr -> {
                // By the time we're at the point of evaluating a variable, all other forms of keys
                // should have been resolved away. The only ones that should be left are Variable Keys.
                // At least, for now?
                // That may not turn out to be the case once we expand beyond our simple test setup.
                val varKey = expr.key as VariableKey
                Bundle.unconstrained(expr.ind.newVariance(varKey)).constrainWith(constraints)
            }

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }

        return toReturn
    }
}