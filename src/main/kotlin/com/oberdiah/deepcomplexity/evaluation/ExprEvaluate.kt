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
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT

object ExprEvaluate {
    data class SupportKey(private val id: Int, private val displayName: String) {
        override fun toString(): String = "$displayName$id"

        companion object {
            private var NEXT_ID = 0
            fun new(displayName: String): SupportKey = SupportKey(NEXT_ID++, displayName)
        }

        fun newIdCopy(): SupportKey = new(displayName)
        fun branchOff(): SupportKey = new("$displayName^")
    }

    data class Scope(
        val constraints: ExprConstrain.ConstraintsOrPile = ExprConstrain.ConstraintsOrPile.unconstrained(),
        val toKeep: Set<Key.ExpressionKey> = setOf(),
        val supportKeyMap: Map<SupportKey, Expr<*>> = mapOf(),
    ) {
        override fun toString(): String = constraints.toString()
        fun shouldKeep(key: Key): Boolean = toKeep.contains(key) || !key.isExpr()

        /**
         * Adds the expression's keys to the scopes we want to keep around.
         * You call this on all expressions that are not your own.
         *
         * For example, in the expression (x + y) * y, when you are evaluating the x + y part,
         * any y-specific information you calculate you should keep around as it's going to be used.
         */
        fun withScope(expr: Expr<*>): Scope {
            val newScopes = expr.iterateTree().map { it.exprKey }.toSet()
            return Scope(constraints, toKeep + newScopes, supportKeyMap)
        }

        fun constrainWith(constraints: ExprConstrain.ConstraintsOrPile): Scope {
            return Scope(
                constraints.and(this.constraints),
                toKeep,
                supportKeyMap,
            )
        }

        fun withSupport(key: SupportKey, expr: Expr<*>): Scope {
            return Scope(
                constraints,
                toKeep,
                supportKeyMap + (key to expr),
            )
        }
    }

    fun <T : Any> evaluate(expr: Expr<T>, scope: Scope, tracer: Tracer): Bundle<T> {
        @Suppress("UNCHECKED_CAST")
        val evaluatedBundle = when (expr.ind) {
            is NumberIndicator<*> -> {
                // Split into two lines for nicer debugging
                val castExpr = expr.castToNumbers()
                evaluateNums(castExpr, scope, tracer)
            }

            is ObjectIndicator -> evaluateGenerics(expr as Expr<*>, scope, tracer)
            BooleanIndicator -> {
                val castExpr = expr.castToBoolean()
                evaluateBools(castExpr, scope, tracer)
            }

            VarsIndicator -> WONT_IMPLEMENT()
        } as Bundle<T>

        tracer.trace(expr, evaluatedBundle)

        return evaluatedBundle.reduceAndSimplify(scope)
    }

    private fun <T : Number> evaluateNums(expr: Expr<T>, scope: Scope, tracer: Tracer): Bundle<T> {
        val toReturn = when (expr) {
            is ArithmeticExpr -> {
                val lhs = evaluate(expr.lhs, scope.withScope(expr.rhs), tracer.leftPath())
                val rhs = evaluate(expr.rhs, scope.withScope(expr.lhs), tracer.rightPath())

                lhs.arithmeticOperation(rhs, expr.op, expr.exprKey)
            }

            is NegateExpr -> evaluate(expr.expr, scope, tracer.onlyPath()).negate()

            else -> evaluateAnythings(expr, scope, tracer)
        }
        return toReturn
    }

    private fun evaluateBools(expr: Expr<Boolean>, scope: Scope, tracer: Tracer): Bundle<Boolean> {
        val toReturn = when (expr) {
            is BooleanExpr -> {
                val lhs = evaluate(expr.lhs, scope.withScope(expr.rhs), tracer.leftPath())
                val rhs = evaluate(expr.rhs, scope.withScope(expr.lhs), tracer.rightPath())

                lhs.booleanOperation(rhs, expr.op, expr.exprKey)
            }

            is ComparisonExpr<*> -> {
                fun <T : Any> evalC(expr: ComparisonExpr<T>, scope: Scope): Bundle<Boolean> {
                    val lhs = evaluate(expr.lhs, scope.withScope(expr.rhs), tracer.leftPath())
                    val rhs = evaluate(expr.rhs, scope.withScope(expr.lhs), tracer.rightPath())

                    return lhs.comparisonOperation(rhs, expr.comp, expr.exprKey)
                }
                evalC(expr, scope)
            }

            is BooleanInvertExpr -> evaluate(expr, scope, tracer.onlyPath()).booleanInvert()
            else -> evaluateAnythings(expr, scope, tracer)
        }

        return toReturn
    }

    private fun <T : Any> evaluateGenerics(expr: Expr<T>, scope: Scope, tracer: Tracer): Bundle<T> {
        return evaluateAnythings(expr, scope, tracer)
    }

    private fun <T : Any> evaluateAnythings(expr: Expr<T>, scope: Scope, tracer: Tracer): Bundle<T> {
        val toReturn: Bundle<T> = when (expr) {
            is UnionExpr -> evaluate(expr.lhs, scope, tracer.leftPath()).union(
                evaluate(expr.rhs, scope, tracer.rightPath())
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

                val condScope = scope.withScope(expr.trueExpr).withScope(expr.falseExpr)
                var trueScope = scope.withScope(expr.thisCondition).withScope(expr.falseExpr)
                var falseScope = scope.withScope(expr.thisCondition).withScope(expr.trueExpr)

                val evaluatedCond = evaluate(ifCondition, condScope, tracer.onlyPath())

                trueScope = trueScope.constrainWith(
                    ExprConstrain.getConstraints(ifCondition, trueScope)
                )
                falseScope = falseScope.constrainWith(
                    ExprConstrain.getConstraints(BooleanInvertExpr.new(ifCondition), falseScope)
                )

                if (true) {
                    if (falseScope.constraints.unreachable) {
                        evaluate(expr.trueExpr, trueScope, tracer.truePath())
                    } else if (trueScope.constraints.unreachable) {
                        evaluate(expr.falseExpr, falseScope, tracer.falsePath())
                    } else {
                        val trueValue = evaluate(expr.trueExpr, trueScope, tracer.truePath())

                        ExprConstrain.getConstraints(BooleanInvertExpr.new(ifCondition), falseScope)

                        val falseValue = evaluate(expr.falseExpr, falseScope, tracer.falsePath())
                        trueValue.union(falseValue)
                    }
                } else {
                    val b = evaluatedCond.unaryMapAndUnion(expr.trueExpr.ind) { bundle, constraints ->
                        when (bundle.collapse(constraints).into()) {
                            BooleanSet.TRUE -> evaluate(expr.trueExpr, trueScope, tracer.truePath())
                            BooleanSet.FALSE -> evaluate(expr.falseExpr, falseScope, tracer.falsePath())
                            BooleanSet.EITHER -> {
                                val trueValue = evaluate(expr.trueExpr, trueScope, tracer.truePath())
                                val falseValue = evaluate(expr.falseExpr, falseScope, tracer.falsePath())
                                trueValue.union(falseValue)
                            }

                            BooleanSet.NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
                        }
                    }
                    b
                }

            }

            is TypeCastExpr<*, *> -> {
                val toCast = evaluate(expr.expr, scope, tracer.onlyPath())
                CastSolver.castFrom(toCast, expr.ind, expr.explicit)
            }

            is ConstExpr -> Bundle.unconstrained(expr.ind.newConstantSet(expr.value).toConstVariance())
                .constrainWith(scope.constraints)

            is VariableExpr ->
                Bundle.unconstrained(expr.ind.newVariance(expr.key)).constrainWith(scope.constraints)

            else -> {
                throw IllegalStateException("Unknown expression type: ${expr::class.simpleName}")
            }
        }

        return toReturn
    }
}