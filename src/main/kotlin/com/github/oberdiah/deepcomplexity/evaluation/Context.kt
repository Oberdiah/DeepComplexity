package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.BundleSet
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

class Context private constructor(val variables: Map<Key, IExpr<*>> = mapOf()) {
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
        fun combine(a: Context, b: Context, how: (a: IExpr<*>, b: IExpr<*>) -> IExpr<*>): Context {
            val newMap = mutableMapOf<Key, IExpr<*>>()

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

    fun evaluateKey(key: Key): BundleSet<*> {
        val expr = variables[key] ?: throw IllegalArgumentException("Key $key not found in context")
        return expr.evaluate(ConstantExpression.TRUE)
    }

    fun getVar(element: PsiElement): IExpr<*> {
        return getVar(element.toKey())
    }

    fun getVar(element: Key): IExpr<*> {
        return variables[element] ?: VariableExpression<Any>(element)
    }

    /**
     * Performs a cast if necessary.
     */
    fun withVar(key: Key, expr: IExpr<*>): Context {
        val newMap = variables.toMutableMap()
        // We're just going to always perform this cast for now.
        // If the code compiles, it's reasonable to do so.
        newMap[key] = expr.performACastTo(key.ind, false)

        return Context(newMap)
    }

    /**
     * Performs a cast if necessary.
     */
    fun withVar(element: PsiElement, expr: IExpr<*>): Context {
        return withVar(element.toKey(), expr)
    }

    /**
     * Performs a cast if necessary.
     */
    fun resolveVar(element: PsiElement, expr: IExpr<*>): Context {
        return resolveVar(element.toKey(), expr)
    }

    /**
     * Performs a cast if necessary.
     */
    fun resolveVar(key: Key, expr: IExpr<*>): Context {
        val castExpr = expr.performACastTo(key.ind, false)
        return Context(variables.mapValues { (key, expr) ->
            expr.rebuildTree(variableExpressionReplacer {
                if (it.key == key) {
                    castExpr
                } else {
                    null
                }
            })
        })
    }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, any undefined variables in the later context will be taken from this
     * when possible.
     */
    fun stack(later: Context): Context {
        // To make matters even more confusing, return keys need to be resolved back-to-front
        // that is, for return statements we should be iterating over ourselves and resolving with `later`'s variables.

        // First, resolve what we can (with all the normal keys).
        val newLater = Context(later.variables.mapValues { (key, expr) ->
            expr.rebuildTree(variableExpressionReplacer {
                if (it.key.isMethod()) return@variableExpressionReplacer null
                variables[it.key]
            })
        })

        // Now, we need to resolve the method keys backward.
        val newMe = Context(variables.mapValues { (key, expr) ->
            expr.rebuildTree(variableExpressionReplacer {
                if (!it.key.isMethod()) return@variableExpressionReplacer null
                later.variables[it.key]
            })
        })

        val newMap = newMe.variables.toMutableMap()

        // Later's variables overwrite ours if they exist as they're more recent.
        // Filter out return keys
        newMap.putAll(newLater.variables.filter { !it.key.isMethod() })

        // Now put all return keys in, but only if they don't exist in this context.
        newMap.putAll(
            newLater.variables.filter { it.key.isMethod() && !newMe.variables.containsKey(it.key) }
        )

        return Context(newMap)
    }

    private fun variableExpressionReplacer(
        replacement: (VariableExpression<*>) -> IExpr<*>?
    ): ExprTreeRebuilder.Replacer {
        return object : ExprTreeRebuilder.Replacer {
            override fun <T : Any> replace(expr: IExpr<T>): IExpr<T> {
                if (expr is VariableExpression) {
                    val resolved = replacement(expr)
                    if (resolved != null) {
                        assert(resolved.ind == expr.ind) {
                            "Resolved expression ${resolved.dStr()} does not match ${expr.dStr()}"
                        }

                        @Suppress("UNCHECKED_CAST") // Safety: Verified indicators match.
                        return resolved as IExpr<T>
                    }
                }

                return expr
            }
        }
    }
}