package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.CastSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.BooleanBundle.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.into

object ExprEvaluate {
    fun <T : Any> evaluate(expr: IExpr<T>, condition: IExpr<Boolean>): BundleSet<T> {
        @Suppress("UNCHECKED_CAST")
        return when (expr.ind) {
            is NumberSetIndicator<*> -> evaluateNums(expr.castToNumbers(), condition) as BundleSet<T>
            is GenericSetIndicator -> evaluateGenerics(expr as IExpr<*>, condition) as BundleSet<T>
            BooleanSetIndicator -> evaluateBools(expr as IExpr<Boolean>, condition) as BundleSet<T>
        }
    }

    private fun <T : Number> evaluateNums(expr: IExpr<T>, condition: IExpr<Boolean>): BundleSet<T> {
        val toReturn = when (expr) {
            is ArithmeticExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                lhs.arithmeticOperation(rhs, expr.op)
            }

            is NegateExpression -> evaluate(expr.expr, condition).negate()
            is NumIterationTimesExpression -> {
                val terms = expr.terms
                // The plan here is to figure out, based on the set of numbers we are allowed to have,
                // the maximum number of times this could occur.
                // This could get pretty complex, but we'll keep it relatively simple for now.

                val startingValue = expr.variable.evaluate(condition)
                startingValue.evaluateLoopingRange(
                    terms.evaluate(condition),
                    expr.constraint
                )
            }

            else -> evaluateAnythings(expr, condition)
        }
        return toReturn
    }

    private fun evaluateBools(expr: IExpr<Boolean>, condition: IExpr<Boolean>): BundleSet<Boolean> {
        val toReturn = when (expr) {
            is BooleanExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                lhs.booleanOperation(rhs, expr.op)
            }

            is ComparisonExpression<*> -> {
                fun <T : Number> evalC(expr: ComparisonExpression<T>, condition: IExpr<Boolean>): BundleSet<Boolean> {
                    val lhs = evaluate(expr.lhs, condition)
                    val rhs = evaluate(expr.rhs, condition)

                    return lhs.comparisonOperation(rhs, expr.comp)
                }
                evalC(expr, condition)
            }

            is BooleanInvertExpression -> evaluate(expr, condition).invert()
            else -> evaluateAnythings(expr, condition)
        }
        return toReturn
    }

    private fun <T : Any> evaluateGenerics(expr: IExpr<T>, condition: IExpr<Boolean>): BundleSet<T> {
        return evaluateAnythings(expr, condition)
    }

    private fun <T : Any> evaluateAnythings(expr: IExpr<T>, condition: IExpr<Boolean>): BundleSet<T> {
        val toReturn: BundleSet<T> = when (expr) {
            is UnionExpression -> evaluate(expr.lhs, condition).union(evaluate(expr.rhs, condition))
            is IfExpression -> {
                val ifCondition = expr.thisCondition
                val evaluatedCond = evaluate(ifCondition, condition)
                val trueCondition = BooleanExpression(ifCondition, condition, BooleanOp.AND)
                val falseCondition =
                    BooleanExpression(BooleanInvertExpression(ifCondition), condition, BooleanOp.AND)

                evaluatedCond.unaryMapAndUnion(expr.trueExpr.ind) { bundle, constraints ->
                    when (bundle.collapse(constraints).into()) {
                        TRUE -> evaluate(expr.trueExpr, trueCondition)
                        FALSE -> evaluate(expr.falseExpr, falseCondition)
                        BOTH -> {
                            val trueValue = evaluate(expr.trueExpr, trueCondition)
                            val falseValue = evaluate(expr.falseExpr, falseCondition)
                            trueValue.union(falseValue)
                        }

                        NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
                    }
                }
            }

            is TypeCastExpression<*, *> -> CastSolver.castFrom(
                evaluate(expr.expr, condition),
                expr.ind,
                expr.explicit
            )

            is ConstExpr -> expr.constSet.constrainWith(condition)
            is VariableExpression -> {
                val constraintsList = ExprConstrain.getConstraints(condition)

                var bundleSet = BundleSet.empty(expr.ind)
                for (constraints in constraintsList) {
                    val newVariance = expr.ind.newVariance(expr.key)
                    // Constrain with the entire set of constraints.
                    bundleSet = bundleSet.union(BundleSet.constrained(newVariance, constraints))
                }

                bundleSet
            }

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
        return toReturn
    }
}