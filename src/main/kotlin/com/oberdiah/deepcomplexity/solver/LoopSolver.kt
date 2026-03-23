package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.context.LoopKey
import com.oberdiah.deepcomplexity.evaluation.EvaluatorAssistant
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LoopExpr
import com.oberdiah.deepcomplexity.evaluation.LoopExpr.ConstEvaluatedLeaf
import com.oberdiah.deepcomplexity.staticAnalysis.IntIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.LongIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.oberdiah.deepcomplexity.utilities.into

object LoopSolver {
    private fun noIdea(): Bundle<Int> =
        Bundle.unconstrained(IntIndicator.newFullSet().toConstVariance())

    /**
     * Returns the bundle representing the number of times that a loop with a given condition could execute,
     * given the variable updates in [variables].
     * Assumes that the loop condition is checked before each iteration, so returning zero is possible.
     */
    fun calculateNumLoops(
        exprKey: EvaluationKey.ExpressionKey,
        condition: Expr<Boolean>,
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<Long> {
        val withinLoopConstraints = ExprConstrain.getConstraints(condition, constraints, assistant.enteredCondition())

        val solves = calculateChangePerStep(variables, withinLoopConstraints, assistant)

        var numLoopsOverall = Long.MAX_VALUE
        for (constraints in withinLoopConstraints.pile) {
            var maxNumLoopsHere = 0L
            for ((exprKey, constrainedIn) in constraints.constraints) {
                val solve = solves[exprKey] ?: continue

                val changePerStep = solve.changePerStep ?: continue
                val initialCondition = solve.initialState

                val longBundle =
                    ConversionsAndPromotion.coerceAToB(changePerStep, initialCondition).map { change, initial ->
                        change.binaryMap(
                            LongIndicator,
                            initial,
                            exprKey
                        ) { changeVariances, initialVariances, constraints ->
                            val change = changeVariances.collapse(constraints)
                            val initial = initialVariances.collapse(constraints)

                            val numLoops = calculateNumLoops(
                                change as NumberSet<*>,
                                initial as NumberSet<*>,
                                constrainedIn as NumberSet<*>
                            )
                            NumberVariances.newFromConstant(numLoops)
                        }
                    }


//                maxNumLoopsHere = maxOf(maxNumLoopsHere, numLoopsHere)
            }
            numLoopsOverall = minOf(numLoopsOverall, maxNumLoopsHere)
        }


        TODO()
    }

    private fun <T : Number> calculateNumLoops(
        change: NumberSet<T>,
        initial: NumberSet<*>,
        constrainedIn: NumberSet<*>
    ): NumberSet<Long> {
        val initial = initial.coerceTo(change.ind)
        val constrainedIn = constrainedIn.coerceTo(change.ind)

        val startingPositions = initial.intersect(constrainedIn)

        TODO()
    }

    fun <T : Any> evaluateTarget(
        target: LoopKey<T>,
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        numberOfTimesLooped: Bundle<Long>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        TODO()
    }

    data class Solve(
        val updateExpression: Expr<*>,
        var initialState: Bundle<*>,
        var changePerStep: Bundle<*>?
    )

    private fun calculateChangePerStep(
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Map<LoopKey<*>, Solve> {
        val solves = variables.mapValues {
            Solve(
                it.value.update,
                it.value.initialState.evaluate(
                    constraints,
                    assistant.keyedPath("initial").keyedPath("${it.key}")
                ),
                null
            )
        }

        myWhile@ while (true) {
            var madeProgress = false
            for ((key, solve) in solves) {
                if (solve.changePerStep != null) continue

                val stepsExpr = solve.updateExpression.rewriteTypeInTreeSameType<LoopExpr.LoopLeaf<*>> { loopLeaf ->
                    solves[loopLeaf.key]!!.changePerStep?.let { ConstEvaluatedLeaf.new(it) } ?: loopLeaf
                }

                val initialStatesExpr =
                    solve.updateExpression.rewriteTypeInTreeSameType<LoopExpr.LoopLeaf<*>> { loopLeaf ->
                        ConstEvaluatedLeaf.new(solves[loopLeaf.key]!!.initialState)
                    }

                val changePerStep = calculateChangePerStep(
                    key,
                    stepsExpr.coerceToNumbers(),
                    constraints,
                    assistant
                )

                if (changePerStep.isEmpty()) {
                    solve.changePerStep = noIdea()
                } else {
                    solve.changePerStep = changePerStep
                    madeProgress = true
                }
            }
            if (!madeProgress) break
        }

        return solves
    }

    /**
     * Figure out, in an expression `x = expr`, how much x changes per step of the loop, assuming the rest of the
     * expression is a constant.
     * For `x = y`, this would return 0.
     * For `x = x + 1`, this would return 1.
     * For `x = (y > 6) ? x + 1 : x`, this would return 1 constrained by the condition `y > 6`,
     * and 0 constrained by the condition `y <= 6`.
     * For `x = (y > 6) ? x + 1 : x * 2;`, this would return 1 constrained by the condition `y > 6`, and
     * `ind.fullSet()` constrained by y <= 6, as that branch is too complex to evaluate.
     */
    private fun <T : Number> calculateChangePerStep(
        loopKey: LoopKey<*>,
        rhs: Expr<T>,
        constraintsPile: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<*> {
        // todo: We need to remember to remove all loop keys from constraintsPile here, otherwise
        //       we'll end up with unrealistically tight constraints on the loop variables, which
        //       cannot use the constraints they had when they entered the loop (which is what these are)
        val evaluated = rhs.evaluate(constraintsPile, assistant.keyedPath("changePerStep").keyedPath("$loopKey"))

        val changePerStep = evaluated.unaryMapSameType { variances, constraints ->
            variances.into().calculateChangePerStep(loopKey, constraints)
                ?: throw UnsupportedOperationException("The expression $rhs is too complex to solve for a change per step for $loopKey.")
        }

        return changePerStep
    }
}
