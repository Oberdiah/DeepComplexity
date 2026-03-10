package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.LoopKey
import com.oberdiah.deepcomplexity.evaluation.EvaluatorAssistant
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LoopExpr
import com.oberdiah.deepcomplexity.staticAnalysis.IntIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
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

        val initialStates = variables.mapValues {
            it.value.initial.evaluate(constraints, assistant.keyedPath("initial").keyedPath("${it.key}"))
        }

        val changePerStep = variables.mapValues { (loopKey, loopVar) ->
            val changes = loopVar.update.evaluate(constraints, assistant.keyedPath("update").keyedPath("$loopKey"))

            val changelist = changes.unaryMapToList { variances, constraints ->
                if (variances.ind !is NumberIndicator) {
                    TODO("Not figured this out yet")
                }

                // For next time: We need to perform this iteratively over the 'variables'
                // We'll iterate through, deriving all changes for each variable.
                // If a 'variances'...

                // Wait, no, this won't work at all.
                // Variances is happy to drop variable tracking if it needs to
                // We need to keep it otherwise everything explodes.
                // That's a shame
                // We'll need to rethink.

                loopKey.key

                TODO()
            }

            if (changelist.size > 1) {
                TODO("Interested in what this looks like when it happens")
            }

            TODO()
        }

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
