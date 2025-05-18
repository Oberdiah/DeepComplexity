package com.github.oberdiah.deepcomplexity.solver

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain
import com.intellij.psi.PsiElement

object LoopSolver {
    /**
     * Given the context for the loop body, and the condition, figure out our new context.
     */
    fun processLoopContext(context: Context, condition: Expr<Boolean>) {
        var numLoops: NumIterationTimesExpression<*>? = null

        val conditionVariables = condition.getVariables()
        for ((key, expr) in context.variables) {
            val variablesMatchingCondition = expr.getVariables()
                .filter { vari -> conditionVariables.any { vari.key == it.key } }

            fun <T : Number> ugly(numExpr: Expr<T>) {
                if (variablesMatchingCondition.isEmpty()) return
                // We now have an expression that is looping stuff.
                val (terms, variable) = collectTerms(numExpr, key.getElement(), variablesMatchingCondition) ?: return

                val constraints = ExprConstrain.getConstraints(condition)
                // Temporary
                assert(constraints.size == 1)
                val constraint = constraints[0].getConstraint(variable)
                TODO()
                // Temporary
//                numLoops = NumIterationTimesExpression.new(constraint, variable, terms)
            }

            val numExpr = expr.castToNumbers()
            ugly(numExpr)
        }

        for ((key, expr) in context.variables) {
            // Unresolved expressions not able to be resolved by this context are of no interest to
            // us as they can't affect this loop.
            val allUnresolved = expr.getVariables().filter { context.canResolve(it) }
            if (allUnresolved.isEmpty()) continue

            val numExpr = expr.castToNumbers()
            val newExpr = repeatExpression(numLoops, numExpr, key.getElement(), allUnresolved)
            context.putVar(key, newExpr)
        }
    }

    private fun <T : Number> repeatExpression(
        numLoops: NumIterationTimesExpression<out Number>?,
        expr: Expr<T>,
        psiElement: PsiElement,
        allUnresolved: List<VariableExpression<*>>
    ): Expr<T> {
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
        if (!linearTerm.isOne()) {
            // We can deal with this if the constant term is 0, but we're not bothering
            // with that for now.
            return gaveUp
        }

        if (numLoops.ind != constantTerm.ind) {
            throw IllegalStateException(
                "Mismatched set indicators: ${numLoops.ind} != ${constantTerm.ind}"
            )
        }
        @Suppress("UNCHECKED_CAST")
//        return ArithmeticExpression(constantTerm, numLoops as Expr<T>, BinaryNumberOp.MULTIPLICATION)
        TODO()
    }

    private fun <T : Number> collectTerms(
        expr: Expr<T>,
        psiElement: PsiElement,
        variables: List<VariableExpression<*>>
    ): Pair<ConstraintSolver.CollectedTerms<T>, VariableExpression<T>>? {
        // We can't deal with this in general, things start getting too complicated when
        // something in the loop relies on two other things.
        // Some edge cases might be doable in certain situations, for now I'm not going to bother.
        if (variables.size != 1) return null

        val unresolved = variables.first().tryCastExact<T, VariableExpression<T>>(expr.ind) ?: return null

        // This happens when we rely only on one thing, but it's not us.
        // We might be able to deal with this with a bit more work, but
        // I'm not going to bother for now.
        if (!unresolved.key.matchesElement(psiElement)) return null

        val terms = ConstraintSolver.expandTerms(expr, unresolved, ConstantExpression.TRUE)
        if (terms == null) return null
        return terms to unresolved
    }
}