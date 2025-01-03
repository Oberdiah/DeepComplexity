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

    /**
     * An unresolved expression represents the state of that element on entering this context.
     * It can be the case that an element is in both unresolved expressions and variables.
     * That does not mean we can resolve the unresolved expression.
     * Unresolved's must be resolved by an earlier context.
     *
     * For example:
     *
     * ```
     * int x = 5;
     *
     * fun foo() {
     *     int y = x;
     *     x = 10;
     * }
     * ```
     *
     * In the above, it would be incorrect to resolve `y` to `10`.
     */
    private val allUnresolvedExpressions = mutableMapOf<PsiElement, UnresolvedExpression.Unresolved>()

    companion object {
        /**
         * Stacks the later context on top of the earlier one.
         *
         * That is, any undefined variables in the later context will be taken from the earlier context
         * when possible.
         */
        fun stack(earlier: Context, later: Context): Context {
            val resultingContext = Context()

            // First, resolve what we can.
            for ((key, value) in later.allUnresolvedExpressions) {
                val resolved = earlier.variables[key]
                if (resolved != null) {
                    value.resolvedExpr = resolved
                } else {
                    resultingContext.allUnresolvedExpressions[key] = value
                }
            }

            // Earlier unresolveds cannot be resolved by a later context.
            resultingContext.allUnresolvedExpressions.putAll(earlier.allUnresolvedExpressions)


            resultingContext.variables.putAll(earlier.variables)
            // Later variables overwrite earlier variables
            resultingContext.variables.putAll(later.variables)

            return resultingContext
        }
    }

    override fun toString(): String {
        val variablesString = variables.entries.joinToString("\n\t") { "${it.key}: ${it.value}" }
        return "Context: {\n\t$variablesString\n}"
    }

    fun applyIf(condition: ExprRetBool, trueCtx: Context, falseCtx: Context) {
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

    private fun getUnresolved(element: PsiElement): Expr {
        return allUnresolvedExpressions.getOrPut(element) {
            UnresolvedExpression.fromElement(element)
        }
    }

    fun getVar(element: PsiElement): Expr {
        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                return variables[element] ?: getUnresolved(element)
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