package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.Context.Key
import com.github.oberdiah.deepcomplexity.evaluation.Context.Key.EphemeralKey
import com.github.oberdiah.deepcomplexity.evaluation.Context.Key.QualifiedKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.DoubleSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.orElse
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty
import com.intellij.psi.*

typealias Vars = Map<Key, Expr<*>>

/**
 * A potentially subtle but important point is that an unknown variable in a context never
 * refers to another variable within that same context.
 *
 * For example, in a context `{ x: y + 1, y: 2}`, the variable `x` is not equal to `2 + 1`, it's equal to `y' + 1`.
 */
class Context(variables: Vars, private val idx: ContextId) {
    /**
     * All the variables in this context.
     *
     * We map all the expressions to our own context id as now that they're here, they must
     * never be resolved with us either, alongside anything they were previously forbidden to resolve.
     */
    val variables: Vars = variables.mapValues { expr ->
        expr.value.replaceTypeInTree<VariableExpression<*>> {
            VariableExpression<Any>(it.key, it.contextId + idx)
        }
    }

    init {
        // We have to allow `This` here to allow us to create a context with 'this' defined
        // to stack on top of and resolve.
        assert(variables.keys.none { it is EphemeralKey || (it is Key.HeapKey && it != Key.HeapKey.This) }) {
            "Ephemeral keys and heap keys shouldn't be used as variable keys."
        }
    }

    @JvmInline
    value class ContextId(val ids: Set<Int>) {
        operator fun plus(other: ContextId): ContextId = ContextId(this.ids + other.ids)
        fun collidesWith(other: ContextId): Boolean = this.ids.any { it in other.ids }

        companion object {
            var ID_INDEX = 0
            fun new(): ContextId = ContextId(setOf(ID_INDEX++))
        }
    }

    /**
     * This isn't a full key by itself, you'll need a [HeapRef] as well and then will want to make a [QualifiedKey].
     */
    data class FieldRef(private val variable: PsiField) {
        override fun toString(): String = variable.toStringPretty()
        fun getElement(): PsiElement = variable
        val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(variable.type)
    }

    sealed class Key {
        abstract class VariableKey() : Key() {
            abstract val variable: PsiVariable
        }

        data class LocalVariableKey(override val variable: PsiLocalVariable) : VariableKey() {
            override fun toString(): String = variable.toStringPretty()
        }

        data class ParameterKey(override val variable: PsiParameter) : VariableKey() {
            override fun toString(): String = variable.toStringPretty()
        }

        data class QualifiedKey(val field: FieldRef, val qualifier: Key = HeapKey.This) : Key() {
            override fun toString(): String = "$qualifier.$field"
        }

        data class HeapKey(
            private val idx: Int,
            // Whether this reference is pointing to an object we watched get created during expression parsing.
            // (We never watch `this` get created, nor unknown objects outside our scope.)
            val newlyCreated: Boolean
        ) : Key() {
            companion object {
                private var KEY_INDEX = 0
                val This = new(false)
                fun new(newlyCreated: Boolean = true): HeapKey = HeapKey(KEY_INDEX++, newlyCreated)
            }

            override fun equals(other: Any?): Boolean = other is HeapKey && this.idx == other.idx
            override fun hashCode(): Int = idx.hashCode()
            override fun toString(): String = if (this == This) "this" else "#${idx}"
        }

        data class ReturnKey(val type: SetIndicator<*>) : Key() {
            companion object {
                val Me = ReturnKey(DoubleSetIndicator)
            }

            override fun toString(): String = "Return value"
            override fun hashCode(): Int = 0
            override fun equals(other: Any?): Boolean = other is ReturnKey
        }

        /**
         * Used to allow us to equate expressions.
         */
        data class ExpressionKey(val expr: Expr<*>) : Key() {
            override fun toString(): String = ExprToString.toExprKeyString(expr)
        }

        /**
         * Primarily for testing, doesn't have a specific element.
         */
        data class EphemeralKey(val key: Any) : Key() {
            override fun toString(): String = "#$key"

            companion object {
                private var KEY_INDEX = 0
                fun new(): EphemeralKey {
                    return EphemeralKey(KEY_INDEX++)
                }
            }
        }

        open val ind: SetIndicator<*>
            get() {
                return when (this) {
                    is ExpressionKey -> this.expr.ind
                    is ReturnKey -> type
                    is VariableKey -> Utilities.psiTypeToSetIndicator(variable.type)
                    is QualifiedKey -> this.field.ind
                    is HeapKey -> GenericSetIndicator(Any::class)
                    else -> throw IllegalArgumentException("Cannot get indicator for $this")
                }
            }

        fun isAutogenerated(): Boolean = isEphemeral() || isExpr()
        fun isEphemeral(): Boolean = this is EphemeralKey
        fun isExpr(): Boolean = this is ExpressionKey
        fun isNewlyCreated(): Boolean =
            this is HeapKey && this.newlyCreated || this is QualifiedKey && this.qualifier.isNewlyCreated()

        /**
         * When multiplying, we need to decide which one gets to live on.
         */
        fun importance(): Int {
            return when (this) {
                is VariableKey -> 5
                is QualifiedKey -> 4
                is HeapKey -> 3
                is ReturnKey -> 2
                is ExpressionKey -> 1
                is EphemeralKey -> 0
            }
        }

        /**
         * If the key is a variable (i.e. a Local Variable, Field, etc.), returns the variable.
         * If the key is a return statement, returns the method we're returning from.
         */
        fun getElement(): PsiElement {
            return when (this) {
                is VariableKey -> variable
                is QualifiedKey -> field.getElement()
                is HeapKey -> throw IllegalArgumentException("Cannot get element of heap key")
                is ReturnKey -> throw IllegalArgumentException("Cannot get element of return key")
                is EphemeralKey -> throw IllegalArgumentException("Cannot get element of arbitrary key")
                is ExpressionKey -> throw IllegalArgumentException("Cannot get element of expression key")
            }
        }

        fun matchesElement(element: PsiElement): Boolean {
            return getElement() == element
        }
    }

    companion object {
        /**
         * You won't want this often, in nearly all cases it makes sense
         * to inherit the existing context via cloning.
         */
        fun brandNew(): Context = Context(emptyMap(), ContextId.new())

        /**
         * Combines two contexts at the same 'point in time' e.g. a branching if statement.
         * This does not and cannot resolve any unresolved expressions as these two statements
         * are independent of each other.
         *
         * You must define how to resolve conflicts.
         */
        fun combine(a: Context, b: Context, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): Context {
            return Context(
                (a.variables.keys + b.variables.keys)
                    .associateWith { key ->
                        val aVal = a.variables[key]
                        val bVal = b.variables[key]

                        // This equality is probably not very cheap.
                        // I'm sure that can be improved in the future.
                        if (aVal == bVal) {
                            // Safety: We know that at least one is not null, so both must be non-null in here.
                            aVal!!
                        } else if (aVal != null && bVal != null) {
                            how(aVal, bVal)
                        } else if (key.isNewlyCreated()) {
                            // Safety: We know at least one is not null.
                            aVal ?: bVal!!
                        } else {
                            how(
                                aVal ?: VariableExpression<Any>(key, a.idx),
                                bVal ?: VariableExpression<Any>(key, b.idx)
                            )
                        }
                    },
                a.idx + b.idx
            )
        }
    }

    val returnValue: Expr<*>?
        get() = variables[Key.ReturnKey.Me]

    override fun toString(): String {
        val variablesString =
            variables.entries.joinToString("\n") { entry ->
                val expr = entry.value
                val key = entry.key

                "$key:\n${expr.toString().prependIndent()}"
            }
        return "Context: {\n${variablesString.prependIndent()}\n}"
    }

    fun getVar(element: Key): Expr<*> {
        return variables[element] ?: VariableExpression<Any>(element, idx)
    }

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): Context {
        assert(rExpr.iterateTree().none { it is LValueExpr }) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolveLValues(context)` on it first."
        }

        if (lExpr is LValueKeyExpr) {
            val castVar = rExpr.castToUsingTypeCast(lExpr.key.ind, false)
            return Context(variables + (lExpr.key to castVar), idx)
        } else if (lExpr !is LValueFieldExpr) {
            throw IllegalArgumentException("This cannot happen")
        }

        /**
         * This does look a bit scary, so I'll try to walk you through it:
         * Essentially, a qualifier may not just be a simple VariableExpression with a HeapKey.
         * In the simplest case it is, and this all becomes a lot easier, but in the general case
         * it may be any complicated expression.
         * Let's go with the following example:
         * ```
         * a = new C(2);
         * b = new C(3);
         * ((x > 0) ? a : b).x = 5
         * ```
         * Now, the only objects we should be touching with our operation are `a` and `b`, so we gather
         * them first into [varKeysInvolved]. That part's simple enough.
         *
         * Then, for the variables we want to modify, we take our qualifier as specified above, and replace
         * `a` and `b` with either:
         *      a) The value already at that object, effectively turning `b` into `b.x`
         *      b) The value that we're setting this field to
         *  depending on whether the object we're modifying is the object being replaced in the expression.
         *
         *  The result of this is that for something like `((x > 0) ? a : b).x = 5`, the variables end up
         *  like so:
         *      `a.x = { (x > 0) ? 5 : 3 }`
         *      `b.x = { (x > 0) ? 2 : 5 }`
         *  which is exactly as desired.
         */

        val qualifier = lExpr.qualifier
        val fieldKey = lExpr.field

        val objectsMentionedInQualifier =
            qualifier.iterateTree()
                .filterIsInstance<VariableExpression<*>>()
                .map { it.key }
                .toSet()

        val newVariables = variables + objectsMentionedInQualifier.map {
            val thisVarKey = QualifiedKey(fieldKey, it)
            val newValue = qualifier.replaceTypeInLeaves<VariableExpression<*>>(fieldKey.ind) { expr ->
                if (expr.key == it) {
                    rExpr
                } else {
                    getVar(thisVarKey)
                }
            }

            (thisVarKey to newValue)
        }

        return Context(newVariables, idx)
    }

    /**
     * Performs a cast if necessary.
     */
    fun withResolvedVar(key: Key, expr: Expr<*>): Context {
        val castExpr = expr.castToUsingTypeCast(key.ind, false)

        return Context(variables.mapValues { (_, oldExpr) ->
            oldExpr.replaceTypeInTree<VariableExpression<*>> {
                if (it.key == key) castExpr else null
            }
        }, idx)
    }

    /**
     * Resolves all variables in the expression that are known of in this context.
     */
    fun resolveKnownVariables(expr: Expr<*>): Expr<*> =
        expr.replaceTypeInTree<VariableExpression<*>> { varExpr ->
            assert(!varExpr.contextId.collidesWith(idx)) {
                "Cannot resolve variables from the same context that created them."
            }
            if (varExpr.key is QualifiedKey) {
                getVar(varExpr.key.qualifier).getField(this, varExpr.key.field)
            } else {
                getVar(varExpr.key)
            }
        }

    fun stackWithReturn(later: Context): Context {
        val later2 = later.returnValue?.let {
            Context(later.variables + (Key.ReturnKey.Me to resolveKnownVariables(it)), later.idx)
        }.orElse {
            later
        }

        val retVal = returnValue?.let {
            mapOf(Key.ReturnKey.Me to later2.resolveKnownVariables(it))
        } ?: mapOf()

        return Context(stack(later).variables + retVal, idx)
    }

    /**
     * Stacks the later context on top of this one.
     *
     * That is, prioritise the later context and fall back to this one if the key doesn't exist.
     */
    fun stack(later: Context): Context {
        var newContext = this

        for ((key, expr) in later.variables) {
            val lValue = if (key is QualifiedKey) {
                // The qualifier is a key itself, so it also needs to try and get resolved.
                LValueFieldExpr<Any>(key.field, getVar(key.qualifier))
            } else {
                // Do nothing, just assign as normal.
                LValueKeyExpr(key)
            }

            newContext = newContext.withVar(lValue, resolveKnownVariables(expr))
        }

        return Context(newContext.variables, idx)
    }

    fun withoutReturnValue(): Context {
        return Context(variables - Key.ReturnKey.Me, idx)
    }
}