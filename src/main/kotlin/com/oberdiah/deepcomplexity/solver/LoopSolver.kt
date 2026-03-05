package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.LoopKey
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.staticAnalysis.IntIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.arithmeticOperation

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
        val returnValue = when (condition) {
            is BooleanOpExpr -> {
                val lhs = calculateNumLoops(condition.lhs, variables, constraints, assistant.leftPath())
                val rhs = calculateNumLoops(condition.rhs, variables, constraints, assistant.rightPath())
                when (condition.op) {
                    BooleanOp.AND -> lhs.arithmeticOperation(
                        rhs,
                        BinaryNumberOp.MAXIMUM,
                        condition.exprKey
                    )

                    BooleanOp.OR -> lhs.arithmeticOperation(
                        rhs,
                        BinaryNumberOp.MINIMUM,
                        condition.exprKey
                    )
                }
            }

            is ComparisonExpr<*> -> calculateNumLoops(condition, variables, constraints, assistant)

            is ConstExpr -> {
                val value = condition.value
                if (value) {
                    noIdea()
                } else {
                    Bundle.unconstrained(IntIndicator.onlyZeroSet().toConstVariance())
                }
            }

            is BooleanInvertExpr -> TODO("Who knows")
            is IfExpr -> TODO("Can probably figure out with some work")
            else -> TODO("Might be able to do one day")
        }

        return returnValue
    }

    private fun calculateNumLoops(
        condition: ComparisonExpr<*>,
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<Int> {
        // Let's only deal with numbers for now.
        val lhs = condition.lhs.tryCastToNumbers() ?: return noIdea()
        val rhs = condition.rhs.tryCastToNumbers() ?: return noIdea()

        val lhsBundle = lhs.evaluate(constraints, assistant.leftPath())
        val rhsBundle = rhs.evaluate(constraints, assistant.rightPath())

        // For now, we're only going to deal with situations where only one side of the comparison contains
        // a LoopVar. Maybe one day we can deal with more, but things are complex enough as it is.

        // The plan here is pretty simple - we literally just evaluate lhs and rhs here, and then run a standard
        // numberVariances comparison on the results. We'll get a Constraints object back that we can query
        // for what is 'within loop'
        // We'll then query elsewhere for the simple loop evaluation algorithm, and get the increment per step.
        // Badda bing badda boom we'll have our answer.
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
