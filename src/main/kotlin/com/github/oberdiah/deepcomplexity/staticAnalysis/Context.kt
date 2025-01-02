package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.Expr
import com.github.oberdiah.deepcomplexity.evaluation.ExprRetBool
import com.github.oberdiah.deepcomplexity.evaluation.IfExpression
import com.github.oberdiah.deepcomplexity.evaluation.UnresolvedExpression
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
    private val variables = mutableMapOf<PsiElement, Expr>()

    override fun toString(): String {
        val variablesString = variables.entries.joinToString("\n\t") { "${it.key}: ${it.value}" }
        return "Context: {\n\t$variablesString\n}"
    }

    fun applyContextUnder(condition: ExprRetBool, trueCtx: Context, falseCtx: Context) {
        val currentKeys = variables.keys
        val trueKeys = trueCtx.variables.keys
        val falseKeys = falseCtx.variables.keys

        val allKeys = currentKeys.union(trueKeys).union(falseKeys)

        for (key in allKeys) {
            val trueVar = trueCtx.variables[key] ?: getVar(key)
            val falseVar = falseCtx.variables[key] ?: getVar(key)

            val trueModified = trueVar != variables[key]
            val falseModified = falseVar != variables[key]

            if (!trueModified && !falseModified) {
                // No need to do anything
                continue
            }

            variables[key] = IfExpression(trueVar, falseVar, condition)
        }
    }

    fun shallowClone(): Context {
        val newContext = Context()
        newContext.variables.putAll(variables)
        return newContext
    }

    fun getVar(element: PsiElement): Expr {
        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                return variables[element] ?: UnresolvedExpression.fromElement(element)
            }

            else -> {
                TODO("As-yet unsupported PsiElement type (${element::class}) for variable declaration")
            }
        }
    }

    fun assignVar(element: PsiElement, expr: Expr) {
        assert(element is PsiLocalVariable || element is PsiParameter || element is PsiField)

        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                variables[element] = expr
            }

            is PsiReferenceExpression -> {
                // If we're assigning to a variable that's already been declared, overwrite it
                // with the new value. Otherwise, create a new variable.
                val variable = element.resolve() ?: TODO(
                    "Variable couldn't be resolved (${element.text})"
                )

                variables[variable] = expr
            }

            else -> {
                TODO(
                    "As-yet unsupported PsiElement type (${element::class}) for variable declaration"
                )
            }
        }
    }
}