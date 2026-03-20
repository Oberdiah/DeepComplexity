package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.LoopKey
import com.oberdiah.deepcomplexity.evaluation.EvaluatorAssistant
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LoopExpr
import com.oberdiah.deepcomplexity.evaluation.LoopExpr.ConstEvaluatedLeaf
import com.oberdiah.deepcomplexity.staticAnalysis.IntIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain

object LoopSolver {
    private fun noIdea(): Bundle<Int> =
        Bundle.unconstrained(IntIndicator.newFullSet().toConstVariance())

    /**
     * Returns the bundle representing the number of times that a loop with a given condition could execute,
     * given the variable updates in [variables].
     * Assumes that the loop condition is checked before each iteration, so returning zero is possible.
     */
    fun calculateNumLoops(
        condition: Expr<Boolean>,
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<Int> {
        val withinLoopConstraints = ExprConstrain.getConstraints(condition, constraints, assistant.enteredCondition())

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
    ) {
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

                TODO()
            }
            if (!madeProgress) break
        }
    }

    /*
     * It's been verified at this point that the expression only contains our loop key. Everything else can be
     * considered a 'constant'.
     *
     * Wait, this doesn't work - we'd get here if we had
     * x = x + 1
     * y = y + x // Once x was solved, this would be a valid 1-key calculation, which is obviously wrong.
     *
     * I suspect there are only two valid cases - where the rhs contains our key and no other loop leaves,
     * or cases where the rhs contains only other loop leaves.
     *
     * I guess there is the third case of cancelling out actually
     * e.g.
     * x = x + 1
     * y = y + 1
     * z = x + 1 - (x + y)
     *
     * Who knows though. I may need more powerful machinery.
     */

    /**
     * Figure out, in an expression `x = expr`, how much x changes per step of the loop, assuming the rest of the
     * expression is a constant.
     * For `x = y`, this would return 0.
     * For `x = x + 1`, this would return 1.
     * For `x = (y > 6) ? x + 1 : x`, this would return 1 constrained by the condition `y > 6`,
     * and 0 constrained by the condition `y <= 6`.
     * For `x = (y > 6) ? x + 1 : x * 2;`, this would return 1 constrained by the condition `y > 6`, and
     * `ind.fullSet()` constrained by y <= 6, as that branch is too complex to evaluate.
     * Can handle and return a correct, if perhaps non-useful, answer for every possible expression input.
     */
    private fun calculateChangePerStep(
        loopKey: LoopKey<*>,
        rhs: Expr<*>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<out Number> {
        TODO()
    }

    fun <T : Any> evaluateTarget(
        target: LoopKey<T>,
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        numberOfTimesLooped: Bundle<Int>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        TODO()
    }
}
