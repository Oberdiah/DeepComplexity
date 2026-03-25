package com.oberdiah.deepcomplexity.solver

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.context.LoopKey
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.LoopExpr.ConstEvaluatedLeaf
import com.oberdiah.deepcomplexity.staticAnalysis.LongIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.NumberIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.arithmeticOperation
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberRange
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.oberdiah.deepcomplexity.utilities.Utilities.max
import com.oberdiah.deepcomplexity.utilities.Utilities.min
import com.oberdiah.deepcomplexity.utilities.Utilities.minus
import com.oberdiah.deepcomplexity.utilities.into

object LoopSolver {
    private fun noIdea(): Bundle<Long> =
        Bundle.unconstrained(LongIndicator.newFullSet().toConstVariance())

    /**
     * Returns the bundle representing the number of times that a loop with a given condition could execute,
     * given the variable updates in [variables].
     * Assumes that the loop condition is checked before each iteration, so returning zero is possible.
     */
    fun <T : Any> evaluateTarget(
        target: LoopKey<T>,
        exprKey: EvaluationKey.ExpressionKey,
        condition: Expr<Boolean>,
        variables: Map<LoopKey<*>, LoopExpr.LoopVar<*>>,
        constraints: ConstraintsOrPile,
        assistant: EvaluatorAssistant
    ): Bundle<T> {
        val withinLoopConstraints = ExprConstrain.getConstraints(condition, constraints, assistant.enteredCondition())
        val solves = calculateChangePerStep(variables, withinLoopConstraints, assistant)

        var numLoopsOverall = Bundle.unconstrainedConstant(Long.MAX_VALUE)
        for (constraints in withinLoopConstraints.pile) {
            var maxNumLoopsHere = Bundle.unconstrainedConstant(0L)
            for ((exprKey, constrainedIn) in constraints.constraints) {
                val solve = solves[exprKey] ?: continue

                val changePerStep = solve.changePerStep ?: continue
                val initialCondition = solve.initialState

                val numLoops =
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


                maxNumLoopsHere = maxNumLoopsHere.arithmeticOperation(
                    numLoops,
                    BinaryNumberOp.MAXIMUM,
                    exprKey
                )
            }
            numLoopsOverall = maxNumLoopsHere.arithmeticOperation(
                maxNumLoopsHere,
                BinaryNumberOp.MINIMUM,
                exprKey
            )
        }

        require(target.ind is NumberIndicator)

        @Suppress("UNCHECKED_CAST")
        // Safe because makeBundle returns the same type as it consumes.
        // This is not great, but will do for now while I'm working on it.
        // It obviously can't stay like this long-term.
        return makeBundle(numLoopsOverall, solves, target as LoopKey<Number>, exprKey) as Bundle<T>
    }

    private fun <T : Number> makeBundle(
        numLoops: Bundle<Long>,
        solves: Map<LoopKey<*>, Solve>,
        target: LoopKey<T>,
        exprKey: EvaluationKey.ExpressionKey
    ): Bundle<T> {
        val solve = solves[target]!!

        require(solve.changePerStep != null)
        require(solve.changePerStep?.ind == target.ind)
        require(solve.initialState.ind == target.ind)

        @Suppress("UNCHECKED_CAST")
        val targetInitial = solve.initialState as Bundle<T>

        @Suppress("UNCHECKED_CAST")
        val targetChangePerStep = solve.changePerStep!! as Bundle<T>

        val totalChangeObserved = targetChangePerStep.castTo(LongIndicator).arithmeticOperation(
            numLoops,
            BinaryNumberOp.MULTIPLICATION,
            exprKey
        )

        return targetInitial.castTo(LongIndicator).arithmeticOperation(
            totalChangeObserved,
            BinaryNumberOp.ADDITION,
            exprKey
        ).castTo(target.ind)
    }

    private fun <T : Number> calculateNumLoops(
        change: NumberSet<T>,
        initial: NumberSet<*>,
        constrainedIn: NumberSet<*>
    ): NumberSet<Long> {
        val ind = change.ind
        val initial = initial.coerceTo(ind).into()
        val constrainedIn = constrainedIn.coerceTo(ind).into()

        val possibleInitials = constrainedIn.intersect(initial)

        if (possibleInitials.isEmpty()) {
            // A loop that starts outside its constrained range always executes zero times.
            return NumberSet.zero(LongIndicator)
        }

        if (change.contains(ind.getZero())) {
            // We're just not going to think about this for now, it's just a pain.
            TODO("For loop with a possible change value of zero ($change), will hopefully handle some day.")
        }

        if (!change.isSingleValue()) {
            TODO("For loop with a change value that's not 1 ($change), will hopefully handle some day.")
        }

        val changeAmount = change.getSingleValue()!!.toLong()

        if (changeAmount < 0) {
            TODO("For loop with a negative change value ($change), gonna handle later.")
        }

        if (constrainedIn.invert().into().ranges.any { it.size().toLong() < changeAmount }) {
            TODO("Constraints with a gap that's too small (< $changeAmount), gonna handle later.")
        }

        val incrementalAmount = getIncrementalAmount(possibleInitials, constrainedIn)

        return incrementalAmount.castTo(LongIndicator).into()
            .divide(NumberSet.newFromConstant(changeAmount))
            .add(NumberSet.newFromConstant(1))
    }

    /**
     * Calculates the range of possible values the loop variable can move in before hitting its cutoff point.
     * If a loop starts at 0 and can be in the range `0..9`, this will return 9.
     */
    private fun <T : Number> getIncrementalAmount(
        initial: NumberSet<T>,
        constrainedWithin: NumberSet<T>
    ): NumberSet<T> {
        require(constrainedWithin.ranges.isNotEmpty())

        var minimum = initial.ind.getMaxValue()
        var maximum = initial.ind.getZero()

        for (range in constrainedWithin.ranges) {
            val possibleStarts = initial.intersect(NumberSet.newFromRange(range))
            val smallestDistance = range.end - possibleStarts.largestValue()
            val largestDistance = range.end - possibleStarts.smallestValue()

            minimum = minimum.min(smallestDistance)
            maximum = maximum.max(largestDistance)
        }

        return NumberSet.newFromRange(NumberRange.new(minimum, maximum))
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

        var progress = true
        while (progress) {
            progress = false
            for ((key, solve) in solves) {
                if (solve.changePerStep != null) continue

                val dependencies = solve.updateExpression.allSubExprsOfType<LoopExpr.LoopLeaf<*>>()
                    .associateWith { solves[it.key]?.changePerStep }

                val nulls = dependencies.filterValues { it == null }.keys.map { it.key }
                val nullsMinusKey = nulls - key

                if (nullsMinusKey.isEmpty()) {
                    progress = true

                    val originalExpr = solve.updateExpression

                    val nextStepExpr =
                        solve.updateExpression.rewriteTypeInTreeSameType<LoopExpr.LoopLeaf<*>> { loopLeaf ->
                            solves[loopLeaf.key]!!.changePerStep?.let {
                                ConversionsAndPromotion.coerceAToB(
                                    ConstEvaluatedLeaf.new(it),
                                    loopLeaf.coerceToNumbers()
                                ).map { l, r ->
                                    ArithmeticExpr.new(l, r, BinaryNumberOp.ADDITION)
                                }
                            } ?: solves[key]!!.updateExpression
                        }

                    val finalExpr = ConversionsAndPromotion.coerceAToB(
                        nextStepExpr,
                        originalExpr.coerceToNumbers()
                    ).map { l, r ->
                        ArithmeticExpr.new(l, r, BinaryNumberOp.SUBTRACTION)
                    }

                    val evaluated = finalExpr.evaluate(constraints, assistant.keyedPath("final").keyedPath("$key"))
                    val evaluatedToConstant = evaluated.unaryMapSameType { variances, constraints ->
                        variances.collapse(constraints).toConstVariance()
                    }

                    solve.changePerStep = evaluatedToConstant
                }
            }
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
    private fun <T : Number> calculateChangePerStep( // not currently used, unsure if we'll need it one day or not.
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
