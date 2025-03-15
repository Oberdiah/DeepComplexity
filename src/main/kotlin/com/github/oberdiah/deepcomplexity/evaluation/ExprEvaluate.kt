package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.CastSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

object ExprEvaluate {
    fun <T : IMoldableSet<T>> evaluate(expr: IExpr<T>, condition: IExpr<BooleanSet>): T {
        @Suppress("UNCHECKED_CAST")
        return when (expr.getSetIndicator()) {
            is NumberSetIndicator<*, *> -> evaluateNums(expr.tryCastToNumbers()!!, condition) as T
            BooleanSetIndicator -> evaluateBools(expr as IExpr<BooleanSet>, condition) as T
            GenericSetIndicator -> evaluateGenerics(expr as IExpr<GenericSet>, condition) as T
        }
    }

    private fun <T : NumberSet<T>> evaluateNums(expr: IExpr<T>, condition: IExpr<BooleanSet>): T {
        return when (expr) {
            is ArithmeticExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                return lhs.arithmeticOperation(rhs, expr.op)
            }

            is NegateExpression -> evaluate(expr.expr, condition).negate()
            is NumberLimitsExpression -> {
                val rhs = expr.limit.evaluate(condition)
                when (expr.shouldFlipCmp.evaluate(condition)) {
                    TRUE -> rhs.getSetSatisfying(expr.cmp.flip())
                    FALSE -> rhs.getSetSatisfying(expr.cmp)
                    BOTH -> rhs.getSetSatisfying(expr.cmp).union(rhs.getSetSatisfying(expr.cmp.flip()))
                    NEITHER -> throw IllegalStateException("Condition is neither true nor false!")
                }
            }

            is NumIterationTimesExpression -> {
                val terms = expr.terms
                // The plan here is to figure out, based on the set of numbers we are allowed to have,
                // the maximum number of times this could occur.
                // This could get pretty complex, but we'll keep it relatively simple for now.
                val constrainingValues = expr.constraint.evaluate(condition)
                val startingValue = expr.variable.evaluate(condition)

                startingValue.evaluateLoopingRange(terms.evaluate(condition), constrainingValues)
            }

            else -> evaluateAnythings(expr, condition)
        }
    }

    private fun evaluateBools(expr: IExpr<BooleanSet>, condition: IExpr<BooleanSet>): BooleanSet {
        return when (expr) {
            is BooleanExpression -> {
                val lhs = evaluate(expr.lhs, condition)
                val rhs = evaluate(expr.rhs, condition)

                return lhs.booleanOperation(rhs, expr.op)
            }

            is ComparisonExpression<*> -> {
                fun <T : NumberSet<T>> evalC(expr: ComparisonExpression<T>, condition: IExpr<BooleanSet>): BooleanSet {
                    val lhs = evaluate(expr.lhs, condition)
                    val rhs = evaluate(expr.rhs, condition)

                    return lhs.comparisonOperation(rhs, expr.comp)
                }
                evalC(expr, condition)
            }

            is BooleanInvertExpression -> evaluate(expr, condition).invert()
            else -> evaluateAnythings(expr, condition)
        }
    }

    private fun evaluateGenerics(expr: IExpr<GenericSet>, condition: IExpr<BooleanSet>): GenericSet {
        return evaluateAnythings(expr, condition)
    }

    private fun <T : IMoldableSet<T>> evaluateAnythings(expr: IExpr<T>, condition: IExpr<BooleanSet>): T {
        return when (expr) {
            is IntersectExpression -> evaluate(expr.lhs, condition).intersect(evaluate(expr.rhs, condition))
            is UnionExpression -> evaluate(expr.lhs, condition).union(evaluate(expr.rhs, condition))
            is InvertExpression -> evaluate(expr.expr, condition).invert()
            is IfExpression -> {
                val ifCondition = expr.thisCondition
                val evaluatedCond = evaluate(ifCondition, condition)
                val trueCondition = BooleanExpression(ifCondition, condition, BooleanOp.AND)
                val falseCondition =
                    BooleanExpression(BooleanInvertExpression(ifCondition), condition, BooleanOp.AND)
                when (evaluatedCond) {
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

            is TypeCastExpression<*, *> -> CastSolver.castFrom(
                evaluate(expr.expr, condition),
                expr.getSetIndicator(),
                expr.explicit
            )

            is ConstExpr -> expr.singleElementSet

            is VariableExpression -> {
                expr.resolvedInto?.let {
                    return evaluate(it, condition)
                }

                val constraints = ExprConstrain.getConstraints(condition, expr)
                val constrainedRange = constraints?.evaluate(condition)

                if (constrainedRange != null) {
                    expr.getSetIndicator().newConstrainedSet(expr.myKey.key, constrainedRange)
                } else {
                    ConstantExpression.fullSetFromExprAndKey(expr, expr.myKey.key)
                }
            }

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
    }
}