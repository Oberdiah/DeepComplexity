package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.BundleSet
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
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

        /**
         * Method is the key representing the method itself, or the remaining bits of it still to be processed.
         * It behaves differently to other keys in that it applies backward when stacking.
         *
         * Normally, if you had two contexts in this order:
         *
         * ```
         * { a = if (x > 5) { 0 } else { a } }
         * { a = a + 5 }
         * ```
         *
         * The latter one would inherit the former's variables, and you'd end up with:
         *
         * ```
         * { a = (if (x > 5) { 0 } else { a }) + 5 }
         * ```
         *
         * However, if `a` was instead a `Method`, you'd end up with:
         *
         * ```
         * { if (x > 5) { 0 } else { a + 5 } }
         * ```
         * e.g. the rest of the method would get inserted into the context.
         *
         * As far as I can tell, this is only really going to ever be useful for return statements.
         * Well that, and grabbing the value returned from a method, which is the same thing really.
         */
        data class Method(val method: PsiElement) : Key() {
            override fun toString(): String {
                return "Rest of method"
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

        val ind: SetIndicator<*>
            get() {
                val type: PsiType = getType()
                val clazz = Utilities.psiTypeToKClass(type)
                    ?: throw IllegalArgumentException("Unsupported type for variable expression")
                return SetIndicator.fromClass(clazz)
            }

        fun isEphemeral(): Boolean = this is EphemeralKey
        fun isMethod(): Boolean = this is Method

        fun getType(): PsiType {
            return when (this) {
                is VariableKey -> variable.type
                is Method -> {
                    val returnType = (method as? PsiMethod)?.returnType
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
                is Method -> method
                is EphemeralKey -> throw IllegalArgumentException("Cannot get element of arbitrary key")
                is ExpressionKey -> throw IllegalArgumentException("Cannot get element of expression key")
            }
        }

        fun matchesElement(element: PsiElement): Boolean {
            return getElement() == element
        }
    }

    private val variables = mutableMapOf<Key, IExpr<*>>()

    companion object {
        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and can not resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         *
         * `a` and `b` (The two contexts) must not be used again after this operation.
         */
        fun combine(a: Context, b: Context, how: (a: IExpr<*>, b: IExpr<*>) -> IExpr<*>): Context {
            val resultingContext = Context()

            val allKeys = a.variables.keys + b.variables.keys

            for (key in allKeys) {
                val aVal = a.variables[key] ?: a.getVar(key)
                val bVal = b.variables[key] ?: b.getVar(key)
                resultingContext.variables[key] = how(aVal, bVal)
            }

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
        return variables.containsKey(variable.key)
    }

    fun evaluateKey(key: Key): BundleSet<*> {
        val expr = variables[key] ?: throw IllegalArgumentException("Key $key not found in context")
        return expr.evaluate(ConstantExpression.TRUE)
    }

    fun getVariables(): Map<Key, IExpr<*>> {
        return variables.toImmutableMap()
    }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, any undefined variables in the later context will be taken from this
     * when possible.
     */
    fun stack(later: Context) {
        // To make matters even more confusing, return keys need to be resolved back-to-front
        // that is, for return statements we should be iterating over ourselves and resolving with `later`'s variables.

        // First, resolve what we can (with all the normal keys).
        for (value in later.variables.values) {
            val allUnresolved = value.getVariables()
            for (unresolved in allUnresolved) {
                if (!unresolved.key.isMethod()) {
                    val resolved = variables[unresolved.key]
                    if (resolved != null) {
                        unresolved.setResolvedExpr(resolved)
                    }
                }
            }
        }

        // Now, we need to resolve the return keys backward.
        for (value in variables.values) {
            val allUnresolved = value.getVariables()
            for (unresolved in allUnresolved) {
                if (unresolved.key.isMethod()) {
                    val resolved = later.variables[unresolved.key]
                    if (resolved != null) {
                        unresolved.setResolvedExpr(resolved)
                    }
                }
            }
        }

        // Later's variables overwrite ours if they exist as they're more recent.
        // Filter out return keys
        variables.putAll(later.variables.filter { !it.key.isMethod() })

        // Now put all return keys in, but only if they don't exist in this context.
        variables.putAll(
            later.variables.filter { it.key.isMethod() && !variables.containsKey(it.key) }
        )
    }

    fun getVar(element: PsiElement): IExpr<*> {
        return getVar(element.toKey())
    }

    fun getVar(element: Key): IExpr<*> {
        return variables[element] ?: VariableExpression.fromKey(element, this)
    }

    /**
     * Performs a cast if necessary.
     */
    fun assignVar(key: Key, expr: IExpr<*>) {
        // We're just going to always perform this cast for now.
        // If the code compiles, it's reasonable to do so.
        variables[key] = expr.performACastTo(key.ind, false)
    }

    /**
     * Performs a cast if necessary.
     */
    fun assignVar(element: PsiElement, expr: IExpr<*>) {
        assignVar(element.toKey(), expr)
    }

    /**
     * Performs a cast if necessary.
     */
    fun resolveVar(element: PsiElement, expr: IExpr<*>) {
        resolveVar(element.toKey(), expr)
    }

    /**
     * Performs a cast if necessary.
     */
    fun resolveVar(key: Key, expr: IExpr<*>) {
        val castExpr = expr.performACastTo(key.ind, false)
        for (variable in variables.values) {
            variable.getVariables().forEach {
                if (it.key == key) {
                    it.setResolvedExpr(castExpr)
                }
            }
        }
    }
}