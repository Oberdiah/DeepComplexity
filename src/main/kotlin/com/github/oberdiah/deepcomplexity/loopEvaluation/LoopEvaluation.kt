package com.github.oberdiah.deepcomplexity.loopEvaluation

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp.*
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.PsiElement

object LoopEvaluation {
    /**
     * Given the context for the loop body, and the condition, figure out our new context.
     */
    fun processLoopContext(context: Context, condition: IExpr<BooleanSet>) {
        var numLoops: NumIterationTimesExpression<*>? = null

        val conditionVariables = condition.getVariables(false)
        for ((key, expr) in context.getVariables()) {
            val variablesMatchingCondition = expr.getVariables(false)
                .filter { vari -> conditionVariables.any { vari.getKey() == it.getKey() } }

            val numExpr = expr.tryCastToNumbers() ?: continue
            if (variablesMatchingCondition.isEmpty()) continue

            // We now have an expression that is looping stuff.
            val (terms, variable) = collectTerms(numExpr, key.getElement(), variablesMatchingCondition) ?: continue

            val constraint = ExprConstrain.getConstraints(condition, variable)?.tryCastToNumbers() ?: continue

            numLoops = NumIterationTimesExpression.new(constraint, variable, terms)
        }

        for ((key, expr) in context.getVariables()) {
            // Unresolved expressions not able to be resolved by this context are of no interest to
            // us as they can't affect this loop.
            val allUnresolved = expr.getVariables(false).filter { context.canResolve(it) }
            if (allUnresolved.isEmpty()) continue

            val numExpr = expr.tryCastToNumbers()
            if (numExpr == null) {
                context.assignVar(key, ConstantExpression.fullExprFromExprAndKey(expr, key))
                continue
            }

            val newExpr = repeatExpression(numLoops, numExpr, key.getElement(), allUnresolved)
            context.assignVar(key, newExpr)
        }
    }

    private fun <T : NumberSet<T>> repeatExpression(
        numLoops: NumIterationTimesExpression<out NumberSet<*>>?,
        expr: IExpr<T>,
        psiElement: PsiElement,
        allUnresolved: List<VariableExpression<*>>
    ): IExpr<T> {
        val gaveUp = ConstantExpression.fullExprFromExprAndKey(expr, Context.Key.EphemeralKey.new())

        if (numLoops == null) {
            // If we don't know the number of loops, we've got absolutely no chance.
            // This can definitely be improved from just throwing all info away though.
            return gaveUp
        }

        val (terms, _) = collectTerms(expr, psiElement, allUnresolved) ?: return gaveUp

        if (terms.terms.size > 2) return gaveUp

        val linearTerm = terms.terms[1] ?: return gaveUp
        val constantTerm = terms.terms[0] ?: return gaveUp
        if (!linearTerm.evaluate(ConstantExpression.TRUE).isOne()) {
            // We can deal with this if the constant term is 0, but we're not bothering
            // with that for now.
            return gaveUp
        }

        if (numLoops.getSetIndicator() != constantTerm.getSetIndicator()) {
            throw IllegalStateException(
                "Mismatched set indicators: ${numLoops.getSetIndicator()} != ${constantTerm.getSetIndicator()}"
            )
        }
        @Suppress("UNCHECKED_CAST")
        return ArithmeticExpression(constantTerm, numLoops as IExpr<T>, MULTIPLICATION)
    }

    private fun <T : NumberSet<T>> collectTerms(
        expr: IExpr<T>,
        psiElement: PsiElement,
        variables: List<VariableExpression<*>>
    ): Pair<ConstraintSolver.CollectedTerms<T>, VariableExpression<T>>? {
        // We can't deal with this in general, things start getting too complicated when
        // something in the loop relies on two other things.
        // Some edge cases might be doable in certain situations, for now I'm not going to bother.
        if (variables.size != 1) return null

        val unresolved = variables.first().tryCastExact<T, VariableExpression<T>>(expr.getSetIndicator()) ?: return null

        // This happens when we rely only on one thing, but it's not us.
        // We might be able to deal with this with a bit more work, but
        // I'm not going to bother for now.
        if (!unresolved.getKey().key.matchesElement(psiElement)) return null

        return ConstraintSolver.expandTerms(expr, unresolved.getKey()) to unresolved
    }
}