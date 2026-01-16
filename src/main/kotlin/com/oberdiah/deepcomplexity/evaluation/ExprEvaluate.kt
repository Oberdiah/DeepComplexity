package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToBoolean
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToNumbers
import com.oberdiah.deepcomplexity.solver.CastSolver
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.VarsIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.*
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet.*
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into
import com.oberdiah.deepcomplexity.utilities.Utilities
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT

object ExprEvaluate {
    data class Scope(
        val constraints: Set<Constraints> = setOf(Constraints.completelyUnconstrained()),
        val scopesToKeep: Set<Key.ExpressionKey> = setOf(),
        val supportKeyMap: Map<SupportKey, Expr<*>> = mapOf()
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
            return Scope(constraints, scopesToKeep + newScopes, supportKeyMap)
        }

        fun constrainWith(constraints: Set<Constraints>): Scope {
            return Scope(
                ExprConstrain.combineConstraints(constraints, this.constraints),
                scopesToKeep,
                supportKeyMap
            )
        }

        fun withSupport(key: SupportKey, expr: Expr<*>): Scope {
            return Scope(
                constraints,
                scopesToKeep,
                supportKeyMap + (key to expr)
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
            is NumberIndicator<*> -> {
                // Split into two lines for nicer debugging
                val castExpr = expr.castToNumbers()
                evaluateNums(castExpr, scope)
            }

            is ObjectIndicator -> evaluateGenerics(expr as Expr<*>, scope)
            BooleanIndicator -> {
                val castExpr = expr.castToBoolean()
                evaluateBools(castExpr, scope)
            }

            VarsIndicator -> WONT_IMPLEMENT()
        } as Bundle<T>

        Utilities.TEST_GLOBALS.EXPR_HASH_BUNDLES[expr.completelyUniqueValueForDebugUseOnly] = evaluatedBundle

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

            is BooleanInvertExpr -> evaluate(expr, scope).booleanInvert()
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
                // Useful note for future reference: The scope constraints are for binding variables
                // together. For example, we may be able to know that c is only set to 5 when x is 7.
                // In that case, if we see x != 7 later on, we can be sure c != 5.

                val ifCondition = expr.thisCondition

                val condScope = scope.withScope(expr.trueExpr).withScope(expr.falseExpr)
                var trueScope = scope.withScope(expr.thisCondition).withScope(expr.falseExpr)
                var falseScope = scope.withScope(expr.thisCondition).withScope(expr.trueExpr)

                val evaluatedCond = evaluate(ifCondition, condScope)

                trueScope = trueScope.constrainWith(
                    ExprConstrain.getConstraints(ifCondition, trueScope)
                )
                falseScope = falseScope.constrainWith(
                    ExprConstrain.getConstraints(BooleanInvertExpr.new(ifCondition), falseScope)
                )

                evaluatedCond.unaryMapAndUnion(expr.trueExpr.ind) { bundle, constraints ->
                    when (bundle.collapse(constraints).into()) {
                        TRUE -> evaluate(expr.trueExpr, trueScope)
                        FALSE -> evaluate(expr.falseExpr, falseScope)
                        EITHER -> {
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

            is ConstExpr -> Bundle.unconstrained(expr.ind.newConstantSet(expr.value).toConstVariance())
                .constrainWith(scope)

            is VariableExpr ->
                Bundle.unconstrained(expr.ind.newVariance(expr.key)).constrainWith(scope)

            is ExpressionChain -> {
                // Note that at the moment we're leaving the evaluation of the expression itself to each of the
                // chain pointer locations. This will result in a bit of an explosion in evaluations.
                // We may want to re-think that in the future.
                val newScope = scope.withSupport(expr.supportKey, expr.support)
                evaluate(expr.expr, newScope.withScope(expr.support))
            }

            is ExpressionChainPointer -> {
                val replacementExpr = scope.supportKeyMap[expr.supportKey]
                    ?: throw IllegalStateException("No support key found for ${expr.supportKey}")
                val castResult = evaluate(replacementExpr, scope).cast(expr.ind)
                    ?: throw IllegalStateException("Could not cast $replacementExpr to ${expr.ind}")
                castResult
            }

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }

        return toReturn
    }
}