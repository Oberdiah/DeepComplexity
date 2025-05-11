package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.intellij.psi.*
import kotlinx.collections.immutable.toImmutableMap

class Context {
    sealed class Key {
        // Variable is where the variable is defined —
        // either PsiLocalVariable, PsiParameter, or PsiField
        data class VariableKey(val variable: PsiVariable) : Key() {
            override fun toString(): String {
                val name = "$variable"
                // If ":" exists, we want to remove it and everything before it
                return if (name.contains(":")) {
                    name.substring(name.indexOf(":") + 1)
                } else {
                    name
                }
            }
        }

        // ReturnMethod is the method that the return statement will return to
        data class ReturnKey(val returnMethod: PsiElement) : Key() {
            override fun toString(): String {
                return "Return from $returnMethod"
            }
        }

        /**
         * Could potentially be used one day to allow us to equate expressions,
         * for now does nothing.
         */
        data class ExpressionKey(val expr: IExpr<*>) : Key() {
            override fun toString(): String {
                return expr.toString()
            }
        }

        /**
         * Primarily for testing, doesn't have a specific element.
         */
        data class EphemeralKey(val key: Any) : Key() {
            override fun toString(): String {
                return "#$key"
            }

            companion object {
                var KEY_INDEX = 0
                fun new(): EphemeralKey {
                    return EphemeralKey(KEY_INDEX++)
                }
            }
        }

        fun isEphemeral(): Boolean = this is EphemeralKey

        fun getType(): PsiType {
            return when (this) {
                is VariableKey -> variable.type
                is ReturnKey -> {
                    val returnType = (returnMethod as? PsiMethod)?.returnType
                    // One day this will have to deal with lambdas too.
                    returnType ?: throw IllegalArgumentException("Return statement is not inside a method")
                }

                is EphemeralKey -> throw IllegalArgumentException("Cannot get type of arbitrary key")
                is ExpressionKey -> throw IllegalArgumentException("Cannot get type of expression key")
            }
        }

        /**
         * If the key is a variable (i.e. a Local Variable, Field, etc.), returns the variable.
         * If the key is a return statement, returns the method we're returning from.
         */
        fun getElement(): PsiElement {
            return when (this) {
                is VariableKey -> variable
                is ReturnKey -> returnMethod
                is EphemeralKey -> throw IllegalArgumentException("Cannot get element of arbitrary key")
                is ExpressionKey -> throw IllegalArgumentException("Cannot get element of expression key")
            }
        }

        fun matchesElement(element: PsiElement): Boolean {
            return getElement() == element
        }
    }

    /**
     * Set to false when a destructive operation is called on this context.
     */
    private var alive = true


    private val variables = mutableMapOf<Key, IExpr<*>>()

    companion object {
        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and can not resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         * It cannot be the case that both a and b are null in the callback.
         *
         * a and b must not be used again after this operation.
         */
        fun combine(a: Context, b: Context, how: (a: IExpr<*>, b: IExpr<*>) -> IExpr<*>): Context {
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
                val key = entry.key

                "$key:\n${expr.toString().prependIndent()}"
            }
        return "Context: {\n${variablesString.prependIndent()}\n}"
    }

    fun debugKey(key: Key): String {
        return variables[key]?.dStr() ?: "Key not found"
    }

    fun canResolve(variable: VariableExpression<*>): Boolean {
        assert(alive)
        return variables.containsKey(variable.getKey().key) && variable.getKey().context == this
    }

    fun evaluateKey(key: Key): BundleSet<*> {
        assert(alive)
        val expr = variables[key] ?: throw IllegalArgumentException("Key $key not found in context")
        return expr.evaluate(ConstantExpression.TRUE)
    }

    fun getVariables(): Map<Key, IExpr<*>> {
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
                    val resolved = variables[resolvedKey.key]
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

    fun getVar(element: PsiElement): IExpr<*> {
        assert(alive)
        return getVar(keyFromElement(element))
    }

    fun getVar(element: Key): IExpr<*> {
        assert(alive)
        return variables[element] ?: VariableExpression.fromKey(element, this)
    }

    /**
     * Performs a cast if necessary.
     */
    fun assignVar(key: Key, expr: IExpr<*>) {
        assert(alive)

        val clazz = Utilities.psiTypeToKClass(key.getType())
            ?: throw IllegalArgumentException("Unsupported type for variable expression")
        // We're just going to always perform this cast for now.
        // If the code compiles it's reasonable to do so.
        variables[key] = expr.performACastTo(SetIndicator.fromClass(clazz), false)
    }

    /**
     * Performs a cast if necessary.
     */
    fun assignVar(element: PsiElement, expr: IExpr<*>) {
        assert(alive)
        assignVar(keyFromElement(element), expr)
    }

    private fun keyFromElement(element: PsiElement): Key {
        assert(alive)

        return when (element) {
            is PsiVariable -> Key.VariableKey(element)
            is PsiReturnStatement -> {
                val returnMethod = findContainingMethodOrLambda(element)
                    ?: throw IllegalArgumentException("Return statement is not inside a method or lambda")

                Key.ReturnKey(returnMethod)
            }

            else -> throw IllegalArgumentException("Unsupported PsiElement type: ${element::class} (${element.text})")
        }
    }

    private fun findContainingMethodOrLambda(returnStatement: PsiReturnStatement): PsiElement? {
        var parent = returnStatement.parent
        while (parent != null) {
            when (parent) {
                is PsiMethod -> return parent // Found the containing method
                is PsiLambdaExpression -> return parent // Found the containing lambda
            }
            parent = parent.parent
        }
        return null // Not inside a method or lambda
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