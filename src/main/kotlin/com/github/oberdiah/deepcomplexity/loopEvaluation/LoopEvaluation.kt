package com.github.oberdiah.deepcomplexity.loopEvaluation

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.intellij.psi.PsiElement

object LoopEvaluation {
    /**
     * Given the context for the loop body, and the condition, figure out our new context.
     */
    fun processLoopContext(context: Context, condition: IExprRetBool) {
        val numLoops = null

        val conditionVariables = condition.getVariables(false).map { it.getKey() }
        for ((psiElement, expr) in context.getVariables()) {
            val matchingCondition = expr.getVariables(false)
                .filter { conditionVariables.contains(it.getKey()) }

            if (matchingCondition.isEmpty()) continue

            // We now have an expression that is looping stuff.
            val terms = collectTerms(expr, psiElement, matchingCondition) ?: continue
            val linearTerm = terms.terms[1] ?: continue
            val constantTerm = terms.terms[0] ?: continue
            if (terms.terms.size > 2) {
                continue
            }
            if (!linearTerm.evaluate(ConstantExpression.TRUE).isOne()) {
                // We can deal with this if the constant term is 0, but not for now.
                continue
            }


        }

        val allElements = context.getVariables().keys
        for ((psiElement, expr) in context.getVariables()) {
            // Unresolved expressions not able to be resolved by this context are of no interest to
            // us as they can't affect this loop.
            val allUnresolved = expr.getVariables(false).filter { context.canResolve(it) }
            if (allUnresolved.isEmpty()) continue

            if (numLoops == null) {
                // If we don't know the number of loops, we've got absolutely no chance.
                // This can definitely be improved from just throwing all info away though.
                context.assignVar(psiElement, ConstantExpression.fullExprFromExpr(expr))
                continue
            }

            val terms = collectTerms(expr, psiElement, allUnresolved)
            println(terms)
        }
    }

    fun collectTerms(
        expr: IExpr,
        psiElement: PsiElement,
        variables: List<VariableExpression>
    ): ConstraintSolver.CollectedTerms? {
        return if (variables.size == 1) {
            val unresolved = variables.first()
            if (unresolved.getKey().element == psiElement) {
                if (expr is IExprRetNum) {
                    ConstraintSolver.expandTerms(expr, unresolved.getKey())
                } else {
                    // Nothing for now.
                    null
                }
            } else {
                // We rely only on one thing, but it's not us.
                // We might be able to deal with this with a bit more work, but
                // I'm not going to bother for now.
                null
            }
        } else {
            // We can't deal with this in general, things start getting too complicated when
            // something in the loop relies on two other things.
            // Some edge cases might be doable in certain situations, for now I'm not going to bother.
            null
        }
    }
}