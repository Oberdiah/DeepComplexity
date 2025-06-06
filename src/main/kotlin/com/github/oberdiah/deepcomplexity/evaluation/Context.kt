package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

class Context private constructor(val variables: MutableMap<Key, Expr<*>> = mutableMapOf()) {
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
        data class MethodKey(val method: PsiElement) : Key() {
            override fun toString(): String = "Rest of method"
        }

        /**
         * Used to allow us to equate expressions.
         */
        data class ExpressionKey(val expr: Expr<*>) : Key() {
            override fun toString(): String = ExprToString.toExprKey(expr)
        }

        /**
         * Primarily for testing, doesn't have a specific element.
         */
        data class EphemeralKey(val key: Any) : Key() {
            override fun toString(): String = "#$key"

            companion object {
                var KEY_INDEX = 0
                fun new(): EphemeralKey {
                    return EphemeralKey(KEY_INDEX++)
                }
            }
        }

        val ind: SetIndicator<*>
            get() {
                if (this is ExpressionKey) {
                    return this.expr.ind
                }

                val type: PsiType = getType()
                val clazz = Utilities.psiTypeToKClass(type)
                    ?: throw IllegalArgumentException("Unsupported type for variable expression")
                return SetIndicator.fromClass(clazz)
            }

        fun isAutogenerated(): Boolean = isEphemeral() || isExpr()
        fun isEphemeral(): Boolean = this is EphemeralKey
        fun isMethod(): Boolean = this is MethodKey
        fun isExpr(): Boolean = this is ExpressionKey

        /**
         * When multiplying, we need to decide which one gets to live on.
         */
        fun importance(): Int {
            return when (this) {
                is VariableKey -> 3
                is MethodKey -> 2
                is ExpressionKey -> 1
                is EphemeralKey -> 0
            }
        }

        fun getType(): PsiType {
            return when (this) {
                is VariableKey -> variable.type
                is MethodKey -> {
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
                is MethodKey -> method
                is EphemeralKey -> throw IllegalArgumentException("Cannot get element of arbitrary key")
                is ExpressionKey -> throw IllegalArgumentException("Cannot get element of expression key")
            }
        }

        fun matchesElement(element: PsiElement): Boolean {
            return getElement() == element
        }
    }

    companion object {
        fun new(): Context = Context()

        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and can not resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         *
         * `a` and `b` (The two contexts) must not be used again after this operation.
         */
        fun combine(a: Context, b: Context, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): Context {
            val newMap = mutableMapOf<Key, Expr<*>>()

            val allKeys = a.variables.keys + b.variables.keys

            for (key in allKeys) {
                val aVal = a.variables[key] ?: a.getVar(key)
                val bVal = b.variables[key] ?: b.getVar(key)
                newMap[key] = how(aVal, bVal)
            }

            return Context(newMap)
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

    fun evaluateKey(key: Key): Bundle<*> {
        val expr = variables[key] ?: throw IllegalArgumentException("Key $key not found in context")
        return expr.evaluate(ExprEvaluate.Scope())
    }

    fun getVar(element: PsiElement): Expr<*> {
        return getVar(element.toKey())
    }

    fun getVar(element: Key): Expr<*> {
        return variables[element] ?: VariableExpression<Any>(element)
    }

    /**
     * Performs a cast if necessary.
     */
    fun putVar(key: Key, expr: Expr<*>) {
        // We're just going to always perform this cast for now.
        // If the code compiles, it's reasonable to do so.
        variables[key] = expr.performACastTo(key.ind, false)
    }

    /**
     * Performs a cast if necessary.
     */
    fun putVar(element: PsiElement, expr: Expr<*>) {
        putVar(element.toKey(), expr)
    }

    /**
     * Performs a cast if necessary.
     */
    fun resolveVar(element: PsiElement, expr: Expr<*>) {
        return resolveVar(element.toKey(), expr)
    }

    /**
     * Performs a cast if necessary.
     */
    fun resolveVar(key: Key, expr: Expr<*>) {
        val castExpr = expr.performACastTo(key.ind, false)

        variables.replaceAll { _, oldExpr ->
            oldExpr.rebuildTree(variableExpressionReplacer {
                if (it.key == key) castExpr else null
            })
        }
    }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, prioritise the later context and fall back to this one if the key doesn't exist.
     *
     * Conversely, for `Method` keys, this context is prioritised over the later context.
     */
    fun stack(later: Context) {
        val laterResolvedWithMe = later.variables.mapValues { (_, expr) ->
            expr.rebuildTree(variableExpressionReplacer { variables[it.key] })
        }

        val meResolvedWithLater = variables.mapValues { (_, expr) ->
            expr.rebuildTree(variableExpressionReplacer { later.variables[it.key] })
        }

        variables.clear()

        // For normal keys, later takes priority and gets to override.
        variables.putAll(meResolvedWithLater.filter { !it.key.isMethod() })
        variables.putAll(laterResolvedWithMe.filter { !it.key.isMethod() })

        // For method keys, this context takes priority.
        variables.putAll(laterResolvedWithMe.filter { it.key.isMethod() })
        variables.putAll(meResolvedWithLater.filter { it.key.isMethod() })
    }

    private fun variableExpressionReplacer(
        replacement: (VariableExpression<*>) -> Expr<*>?
    ): ExprTreeRebuilder.Replacer {
        return object : ExprTreeRebuilder.Replacer {
            override fun <T : Any> replace(expr: Expr<T>): Expr<T> {
                if (expr is VariableExpression) {
                    val resolved = replacement(expr)
                    if (resolved != null) {
                        assert(resolved.ind == expr.ind) {
                            "(${resolved.ind} != ${expr.ind}) ${resolved.dStr()} does not match ${expr.dStr()}"
                        }

                        @Suppress("UNCHECKED_CAST") // Safety: Verified indicators match.
                        return resolved as Expr<T>
                    }
                }

                return expr
            }
        }
    }
}