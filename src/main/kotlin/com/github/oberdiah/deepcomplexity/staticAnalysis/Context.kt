package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.Expression
import com.github.oberdiah.deepcomplexity.evaluation.UnresolvedVariable
import com.intellij.psi.*


/**
 * For the moment, this is entirely within the context of a single method.
 *
 * The context represents the state of the variables at this point in time.
 * If you're passed a Context, you can safely assume all variables in there
 * have the states specified
 */
class Context {
    // Psi Element is where the variable is defined —
    // either PsiLocalVariable, PsiParameter, or PsiField
    private val variables = mutableMapOf<PsiElement, VariableContext<MoldableSet>>()

    override fun toString(): String {
        return variables.toString()
    }

    fun getVar(element: PsiElement): VariableContext<MoldableSet> {
        return variables[element] ?: VariableContext(UnresolvedVariable(element))
    }

    fun assignVar(element: PsiElement, expression: Expression<MoldableSet>) {
        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                variables[element] = VariableContext(expression)
            }

            is PsiReferenceExpression -> {
                // If we're assigning to a variable that's already been declared, overwrite it
                // with the new value. Otherwise, create a new variable.
                val variable = element.resolve() ?: TODO(
                    "Variable couldn't be resolved (${element.text})"
                )

                variables[variable] = VariableContext(expression)
            }

            else -> {
                TODO(
                    "As-yet unsupported PsiElement type (${element::class}) for variable declaration"
                )
            }
        }
    }

    /**
     * Expression is a converter from the values coming in with what's going out.
     * Expression is equal to whatever we've built up so far for
     * this variable up to this point.
     */
    class VariableContext<out T : MoldableSet>(val expression: Expression<T>) {
        override fun toString(): String {
            return expression.toString()
        }
    }
}