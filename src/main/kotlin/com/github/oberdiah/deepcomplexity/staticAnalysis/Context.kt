package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.intellij.psi.*
import kotlinx.collections.immutable.toImmutableMap


/**
 * For the moment, this is entirely within the context of a single method.
 *
 * The context represents the state of the variables at this point in time.
 * If you're passed a Context, you can safely assume all variables in there
 * have the states specified
 */
class Context {
    /**
     * Set to false when a destructive operation is called on this context.
     */
    private var alive = true

    // Psi Element is where the variable is defined —
    // either PsiLocalVariable, PsiParameter, or PsiField
    private val variables = mutableMapOf<PsiElement, IExpr>()

    companion object {
        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and cannot resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         * It cannot be the case that both a and b are null in the callback.
         *
         * a and b must not be used again after this operation.
         */
        fun combine(a: Context, b: Context, how: (a: IExpr, b: IExpr) -> IExpr): Context {
            assert(a.alive && b.alive)

            val resultingContext = Context()

            val allKeys = a.variables.keys + b.variables.keys

            for (key in allKeys) {
                val aVal = a.variables[key] ?: a.getVar(key)
                val bVal = b.variables[key] ?: b.getVar(key)
                resultingContext.variables[key] = how(aVal, bVal)
            }

            resultingContext.migrateUnresolvedFrom(a)
            resultingContext.migrateUnresolvedFrom(b)

            a.alive = false
            b.alive = false

            return resultingContext
        }
    }

    override fun toString(): String {
        val variablesString =
            variables.entries.joinToString("\n") { entry ->
                val expr = entry.value
                val psi = entry.key

                val evaluated = ExprEvaluate.evaluate(expr, ConstantExpression.TRUE)

                "$psi ($evaluated):\n${expr.toString().prependIndent()}"
            }
        return "Context: {\n${variablesString.prependIndent()}\n}"
    }

    fun getVariables(): Map<PsiElement, IExpr> {
        assert(alive)
        return variables.toImmutableMap()
    }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, any undefined variables in the later context will be taken from this
     * when possible.
     */
    fun stack(later: Context) {
        assert(alive && later.alive)

        // First, resolve what we can.
        for (value in later.variables.values) {
            val allUnresolved = value.getVariables(false)
            for (unresolved in allUnresolved) {
                val resolvedKey = unresolved.getKey()
                if (resolvedKey.context == later) {
                    val resolved = variables[resolvedKey.element]
                    if (resolved != null) {
                        unresolved.setResolvedExpr(resolved)
                    }
                }
            }
        }

        // Later's variables overwrite ours if they exist as they're more recent.
        variables.putAll(later.variables)
        // Any variables that are still unresolved need to be migrated.
        migrateUnresolvedFrom(later)
    }

    fun getVar(element: PsiElement): IExpr {
        assert(alive)
        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                return variables[element] ?: VariableExpression.fromElement(element, this)
            }

            else -> {
                TODO("As-yet unsupported PsiElement type (${element::class}) for variable declaration")
            }
        }
    }

    fun assignVar(element: PsiElement, expr: IExpr) {
        assert(alive)
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

    /**
     * Migrates all unresolved referencing the other context to this one.
     * We need to take other as an argument as migrating over all unresolved vars would
     * likely break stuff.
     *
     * This is needed if you find yourself wanting to move variables from one context to another
     */
    private fun migrateUnresolvedFrom(other: Context) {
        for (value in variables.values) {
            value.getVariables(false).forEach {
                if (it.getKey().context == other) {
                    it.getKey().context = this
                }
            }
        }
    }
}