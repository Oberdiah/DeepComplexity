package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.Context.KeyBackreference
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty
import com.intellij.psi.*

/**
 * Not all keys fall into this category, for example [ExpressionKey]s do not.
 */
sealed class UnknownKey : Key() {
    open val temporary: Boolean = false

    /**
     * Most keys don't need to worry about this.
     */
    open fun withAddedContextId(id: Context.ContextId): UnknownKey = this

    open fun isPlaceholder(): Boolean = false
}

sealed class VariableKey(val variable: PsiVariable) : UnknownKey() {
    override val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(variable.type)
    override fun toString(): String = variable.toStringPretty()
    override fun equals(other: Any?): Boolean = other is VariableKey && this.variable == other.variable
    override fun hashCode(): Int = variable.hashCode()
}

class LocalVariableKey(variable: PsiLocalVariable) : VariableKey(variable)
class ParameterKey(variable: PsiParameter, override val temporary: Boolean = false) : VariableKey(variable)

data class ThisKey(val type: PsiType) : UnknownKey() {
    override val temporary: Boolean = true
    override val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(type)
    override fun toString(): String = "this"
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other is ThisKey
}

data class ReturnKey(override val ind: SetIndicator<*>) : UnknownKey() {
    override fun toString(): String = "Return value"
}

/**
 * Solely used to store any placeholder expression a type may have picked up
 * due to aliasing. Thrown away after stacking.
 *
 * This is needed to allow relatively simple cases to work correctly, e.g.
 * ```
 * a.x = 5;
 * if (b.x == 5) {
 * 	// foo
 * }
 * ```
 *
 * In that example, the context would look as follows after a.x was assigned:
 * ```
 * {
 *     a.x -> 5
 *     Placeholder(T).x -> if (a == Placeholder(T)) ? 5 : Placeholder(T).x
 * }
 * ```
 */
data class PlaceholderKey(override val ind: ObjectSetIndicator) : UnknownKey() {
    override val temporary: Boolean = true
    override fun toString(): String = "PH(${ind.type.toStringPretty()})"
    override fun isPlaceholder(): Boolean = true
}

/**
 * Things that can be qualifiers in a [QualifiedKey]. This is really just [HeapMarker]s and [Context.KeyBackreference]s.
 */
sealed interface Qualifier {
    val ind: SetIndicator<*>
    fun withAddedContextId(newId: Context.ContextId): Qualifier

    /**
     * Turns this [Qualifier] into an expression by trying to resolve it against the given context.
     */
    fun safelyResolveUsing(context: Context): Expr<*>

    /**
     * Turns this [Qualifier] directly into a leaf expression, so either it'll be a [ConstExpr] or a [VariableExpr]
     */
    fun toLeafExpr(): LeafExpr<*>
}

data class QualifiedKey(val field: Field, val qualifier: Qualifier) : UnknownKey() {
    override val ind: SetIndicator<*> = this.field.ind

    val qualifierInd: ObjectSetIndicator = qualifier.ind as ObjectSetIndicator

    override fun toString(): String = "$qualifier.$field"
    override fun withAddedContextId(id: Context.ContextId): QualifiedKey =
        QualifiedKey(field, qualifier.withAddedContextId(id))

    // This is a bit ugly.
    // This whole situation is, really, with the recursive qualified key situation being so confusing.
    override fun isPlaceholder(): Boolean {
        return when (qualifier) {
            is KeyBackreference -> qualifier.isPlaceholder()
            is HeapMarker -> false
        }
    }

    /**
     * This isn't a full key by itself, you'll need a [Qualifier] as well and then will want to make a [QualifiedKey].
     */
    data class Field(private val field: PsiField) {
        override fun toString(): String = field.toStringPretty()
        fun getElement(): PsiElement = field
        val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(field.type)
    }
}
