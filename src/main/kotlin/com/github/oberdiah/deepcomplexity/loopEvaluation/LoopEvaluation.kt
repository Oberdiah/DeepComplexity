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

        val conditionVariables = condition.getVariables(false)
        for ((psiElement, expr) in context.getVariables()) {
            val variablesMatchingCondition = expr.getVariables(false)
                .filter { vari -> conditionVariables.any { vari.getKey() == it.getKey() } }

            if (variablesMatchingCondition.isEmpty()) continue

            // We now have an expression that is looping stuff.
            val (terms, variable) = collectTerms(expr, psiElement, variablesMatchingCondition) ?: continue
            val linearTerm = terms.terms[1] ?: continue
            val constantTerm = terms.terms[0] ?: continue
            if (terms.terms.size > 2) {
                continue
            }
            if (!linearTerm.evaluate(ConstantExpression.TRUE).isOne()) {
                // We can deal with this if the constant term is 0, but not for now.
                continue
            }

//            val solved = ConstraintSolver.getVariableConstraints(
//                condition,
//                variable.getKey()
//            )
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
    ): Pair<ConstraintSolver.CollectedTerms, VariableExpression>? {
        // We can't deal with this in general, things start getting too complicated when
        // something in the loop relies on two other things.
        // Some edge cases might be doable in certain situations, for now I'm not going to bother.
        if (variables.size != 1) return null

        val unresolved = variables.first()

        // This happens when we rely only on one thing, but it's not us.
        // We might be able to deal with this with a bit more work, but
        // I'm not going to bother for now.
        if (unresolved.getKey().element != psiElement) return null

        if (expr !is IExprRetNum) return null

        return ConstraintSolver.expandTerms(expr, unresolved.getKey()) to unresolved
    }
}