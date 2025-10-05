package com.github.oberdiah.deepcomplexity.evaluation

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
 * Things that can be qualifiers in a [QualifiedKey]. This is really just [HeapMarker]s and [UnknownKey]s.
 */
sealed interface QualifierRef {
    val ind: SetIndicator<*>
    fun isNew(): Boolean
    fun addContextId(newId: Context.ContextId): QualifierRef
}

data class QualifiedKey(val field: FieldRef, val qualifier: QualifierRef) : UnknownKey() {
    override val ind: SetIndicator<*> = this.field.ind
    override fun toString(): String = "$qualifier.$field"
    override fun addContextId(id: Context.ContextId): QualifiedKey = QualifiedKey(field, qualifier.addContextId(id))
    override fun isNewlyCreated(): Boolean = qualifier.isNew()

    /**
     * This isn't a full key by itself, you'll need a [QualifierRef] as well and then will want to make a [QualifiedKey].
     */
    data class FieldRef(private val field: PsiField) {
        override fun toString(): String = field.toStringPretty()
        fun getElement(): PsiElement = field
        val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(field.type)
    }
}
