package com.github.oberdiah.deepcomplexity.evaluation

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
    open fun addContextId(id: Context.ContextId): UnknownKey = this
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
 * Things that can be qualifiers in a [QualifiedKey]. This is really just [HeapMarker]s and [Context.KeyBackreference]s.
 */
sealed interface Qualifier {
    val ind: SetIndicator<*>
    fun isNew(): Boolean
    fun addContextId(newId: Context.ContextId): Qualifier

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
    override fun toString(): String = "$qualifier.$field"
    override fun addContextId(id: Context.ContextId): QualifiedKey = QualifiedKey(field, qualifier.addContextId(id))
    override fun isNewlyCreated(): Boolean = qualifier.isNew()

    fun aliasesAgainst(other: ObjectSetIndicator): UnknownKey? {
        return when (qualifier) {
            is Context.KeyBackreference -> qualifier.aliasesAgainst(other)
            is HeapMarker -> TODO("We need to sort this but I couldn't be bothered at the time")// if (other == qualifier.ind) qualifier else null
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
