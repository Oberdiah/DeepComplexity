package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.Expr
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
     * Unresolveds must be resolved by an earlier context.
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
     *
     * It is a list because when we combine contexts both contexts may have independently found
     * the same unresolved expression and we need to be able to update them all when we resolve them.
     */
    private val allUnresolvedExpressions = mutableMapOf<PsiElement, MutableList<UnresolvedExpression.Unresolved>>()

    companion object {
        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and cannot resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         * It cannot be the case that both a and b are null in the callback.
         */
        fun combine(a: Context, b: Context, how: (a: Expr, b: Expr) -> Expr): Context {
            val resultingContext = Context()
            resultingContext.addAllUnresolved(a)
            resultingContext.addAllUnresolved(b)

            val allKeys = a.variables.keys + b.variables.keys

            for (key in allKeys) {
                val aVal = a.variables[key] ?: resultingContext.getVar(key)
                val bVal = b.variables[key] ?: resultingContext.getVar(key)
                resultingContext.variables[key] = how(aVal, bVal)
            }

            return resultingContext
        }
    }

    override fun toString(): String {
        val variablesString = variables.entries.joinToString("\n\t") { "${it.key}: ${it.value}" }
        return "Context: {\n\t$variablesString\n}"
    }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, any undefined variables in the later context will be taken from this
     * when possible.
     */
    fun stack(later: Context) {
        // First, resolve what we can.
        for ((key, values) in later.allUnresolvedExpressions) {
            val resolved = variables[key]
            if (resolved != null) {
                for (value in values) {
                    value.setResolvedExpr(resolved)
                }
            } else {
                allUnresolvedExpressions.getOrPut(key) { mutableListOf() }.addAll(values)
            }
        }
        // Later's variables overwrite ours if they exist as they're more recent.
        variables.putAll(later.variables)
    }

    private fun getUnresolved(element: PsiElement): Expr {
        return allUnresolvedExpressions.getOrPut(element) {
            mutableListOf(UnresolvedExpression.fromElement(element))
        }.first() // It really shouldn't matter which we use.
    }

    private fun addAllUnresolved(other: Context) {
        for ((key, values) in other.allUnresolvedExpressions) {
            allUnresolvedExpressions.getOrPut(key) { mutableListOf() }.addAll(values)
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