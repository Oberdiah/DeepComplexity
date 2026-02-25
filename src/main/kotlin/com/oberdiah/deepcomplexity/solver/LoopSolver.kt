package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.evaluation.EvaluatorAssistant
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LoopExpr
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile

object LoopSolver {
    fun calculateNumLoops(
        condition: Expr<Boolean>,
        variables: Map<LoopExpr.LoopKey<*>, LoopExpr.LoopVar<*>>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<Number> {
        TODO()
    }

    fun <T : Any> evaluateTarget(
        target: LoopExpr.LoopKey<T>,
        variables: Map<LoopExpr.LoopKey<*>, LoopExpr.LoopVar<*>>,
        numberOfTimesLooped: Bundle<Number>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        TODO()
    }
}