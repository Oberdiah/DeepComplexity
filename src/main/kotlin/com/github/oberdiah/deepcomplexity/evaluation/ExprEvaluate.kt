package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.CastSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*

object ExprEvaluate {
    fun <T : Any> evaluate(expr: IExpr<T>, condition: IExpr<Boolean>): BundleSet<T> {
        @Suppress("UNCHECKED_CAST")
        return when (expr.getSetIndicator()) {
            is NumberSetIndicator<*> -> evaluateNums(expr.tryCastToNumbers()!!, condition) as BundleSet<T>
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
            is NumberLimitsExpression -> {
                val rhs = expr.limit.evaluate(condition)
                val bundleSet = expr.shouldFlipCmp.evaluate(condition)

                bundleSet.unaryMapAndUnion(rhs.ind) { bundle ->
                    when (bundle.collapse().into()) {
                        TRUE -> rhs.getSetSatisfying(expr.cmp.flip())
                        FALSE -> rhs.getSetSatisfying(expr.cmp)
                        BOTH -> rhs.getSetSatisfying(expr.cmp)
                            .union(rhs.getSetSatisfying(expr.cmp.flip()))

                        NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                    }
                }
            }

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
            is InvertExpression -> evaluate(expr.expr, condition).invert()
            is IfExpression -> {
                val ifCondition = expr.thisCondition
                val evaluatedCond = evaluate(ifCondition, condition)
                val trueCondition = BooleanExpression(ifCondition, condition, BooleanOp.AND)
                val falseCondition =
                    BooleanExpression(BooleanInvertExpression(ifCondition), condition, BooleanOp.AND)

                evaluatedCond.unaryMapAndUnion(expr.trueExpr.getSetIndicator()) { bundle ->
                    when (bundle.collapse().into()) {
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
                expr.getSetIndicator(),
                expr.explicit
            )

            is ConstExpr -> expr.constSet.constrainWith(condition)
            is VariableExpression -> {
                expr.resolvedInto?.let {
                    return evaluate(it, condition)
                }

                val constraintsList = ExprConstrain.getConstraints(condition)

                var bundleSet = BundleSet.empty(expr.getSetIndicator())
                for (constraints in constraintsList) {
                    // If the variable does not appear in the constraints,
                    // create a new 'full' bundle.

                    val constraint = constraints.getConstraint(expr)
                        ?: expr.getSetIndicator().newFullBundle()

                    val newBundle = constraint.withVariance(expr.myKey.key)
                    // Constrain with the entire set of constraints.
                    bundleSet = bundleSet.union(BundleSet.constrained(newBundle, constraints))
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