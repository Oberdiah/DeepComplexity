package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToBoolean
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToNumbers
import com.oberdiah.deepcomplexity.solver.CastSolver
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.*
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet.*
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into

object ExprEvaluate {
    data class Scope(
        val constraints: Set<Constraints> = setOf(Constraints.completelyUnconstrained()),
        val scopesToKeep: Set<Key.ExpressionKey> = mutableSetOf(),
    ) {
        override fun toString(): String = constraints.toString()
        fun shouldKeep(key: Key): Boolean = scopesToKeep.contains(key) || !key.isExpr()

        fun isUnconstrained(): Boolean = constraints.all { it.isUnconstrained() }

        /**
         * Adds the expression's keys to the scopes we want to keep around.
         * You call this on all expressions that are not your own.
         *
         * For example, in the expression (x + y) * y, when you are evaluating the x + y part,
         * any y-specific information you calculate you should keep around as it's going to be used.
         */
        fun withScope(expr: Expr<*>): Scope {
            val newScopes = expr.iterateTree().map { it.exprKey }.toSet()
            return Scope(constraints, scopesToKeep + newScopes)
        }

        fun constrainWith(constraints: Set<Constraints>): Scope {
            return Scope(
                ExprConstrain.combineConstraints(constraints, this.constraints),
                scopesToKeep
            )
        }
    }

    /**
     * We actually process evaluate backwards so that we can keep track of the scope.
     *
     * skipSimplify is just used for debug printing, which likes to see the non-reduced version.
     */
    fun <T : Any> evaluate(expr: Expr<T>, scope: Scope, skipSimplify: Boolean = false): Bundle<T> {
        @Suppress("UNCHECKED_CAST")
        val evaluatedBundle = when (expr.ind) {
            is NumberSetIndicator<*> -> {
                // Split into two lines for nicer debugging
                val castExpr = expr.castToNumbers()
                evaluateNums(castExpr, scope)
            }

            is ObjectSetIndicator -> evaluateGenerics(expr as Expr<*>, scope)
            BooleanSetIndicator -> {
                val castExpr = expr.castToBoolean()
                evaluateBools(castExpr, scope)
            }
        } as Bundle<T>

        return if (skipSimplify) evaluatedBundle else evaluatedBundle.reduceAndSimplify(scope)
    }

    private fun <T : Number> evaluateNums(expr: Expr<T>, scope: Scope): Bundle<T> {
        val toReturn = when (expr) {
            is ArithmeticExpr -> {
                val lhs = evaluate(expr.lhs, scope.withScope(expr.rhs))
                val rhs = evaluate(expr.rhs, scope.withScope(expr.lhs))

                lhs.arithmeticOperation(rhs, expr.op, expr.exprKey)
            }

            is NegateExpr -> evaluate(expr.expr, scope).negate()
            is NumIterationTimesExpr -> {
                val terms = expr.terms
                // The plan here is to figure out, based on the set of numbers we are allowed to have,
                // the maximum number of times this could occur.
                // This could get pretty complex, but we'll keep it relatively simple for now.

                val startingValue = expr.variable.evaluate(scope)
                startingValue.evaluateLoopingRange(terms, expr.constraint)
            }

            else -> evaluateAnythings(expr, scope)
        }
        return toReturn
    }

    private fun evaluateBools(expr: Expr<Boolean>, scope: Scope): Bundle<Boolean> {
        val toReturn = when (expr) {
            is BooleanExpr -> {
                val lhs = evaluate(expr.lhs, scope.withScope(expr.rhs))
                val rhs = evaluate(expr.rhs, scope.withScope(expr.lhs))

                lhs.booleanOperation(rhs, expr.op, expr.exprKey)
            }

            is ComparisonExpr<*> -> {
                fun <T : Any> evalC(expr: ComparisonExpr<T>, scope: Scope): Bundle<Boolean> {
                    val lhs = evaluate(expr.lhs, scope.withScope(expr.rhs))
                    val rhs = evaluate(expr.rhs, scope.withScope(expr.lhs))

                    return lhs.comparisonOperation(rhs, expr.comp, expr.exprKey)
                }
                evalC(expr, scope)
            }

            is BooleanInvertExpr -> evaluate(expr, scope).invert()
            else -> evaluateAnythings(expr, scope)
        }
        return toReturn
    }

    private fun <T : Any> evaluateGenerics(expr: Expr<T>, scope: Scope): Bundle<T> {
        return evaluateAnythings(expr, scope)
    }

    private fun <T : Any> evaluateAnythings(expr: Expr<T>, scope: Scope): Bundle<T> {
        val toReturn: Bundle<T> = when (expr) {
            is UnionExpr -> evaluate(expr.lhs, scope).union(evaluate(expr.rhs, scope))
            is IfExpr -> {
                val ifCondition = expr.thisCondition

                val condScope = scope.withScope(expr.trueExpr).withScope(expr.falseExpr)
                var trueScope = scope.withScope(expr.thisCondition).withScope(expr.falseExpr)
                var falseScope = scope.withScope(expr.thisCondition).withScope(expr.trueExpr)

                val evaluatedCond = evaluate(ifCondition, condScope)

                trueScope = trueScope.constrainWith(
                    ExprConstrain.getConstraints(ifCondition, trueScope)
                )
                falseScope = falseScope.constrainWith(
                    ExprConstrain.getConstraints(BooleanInvertExpr(ifCondition), falseScope)
                )

                evaluatedCond.unaryMapAndUnion(expr.trueExpr.ind) { bundle, constraints ->
                    when (bundle.collapse(constraints).into()) {
                        TRUE -> evaluate(expr.trueExpr, trueScope)
                        FALSE -> evaluate(expr.falseExpr, falseScope)
                        BOTH -> {
                            val trueValue = evaluate(expr.trueExpr, trueScope)
                            val falseValue = evaluate(expr.falseExpr, falseScope)
                            trueValue.union(falseValue)
                        }

                        NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
                    }
                }
            }

            is TypeCastExpr<*, *> -> {
                val toCast = evaluate(expr.expr, scope)
                CastSolver.castFrom(toCast, expr.ind, expr.explicit)
            }

            is ConstExpr -> Bundle.constrained(
                expr.ind.newConstantSet(expr.underlying).toConstVariance(),
                Constraints.completelyUnconstrained()
            ).constrainWith(scope)

            is VariableExpr ->
                Bundle.constrained(
                    expr.ind.newVariance(expr.key.grabTheKeyYesIKnowWhatImDoingICanGuaranteeImInTheEvaluateStage()),
                    Constraints.completelyUnconstrained()
                )
                    .constrainWith(scope)

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }
        return toReturn
    }
}