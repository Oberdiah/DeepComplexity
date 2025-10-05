package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty
import com.intellij.psi.*

/**
 * [UnknownKey]s are keys that can be used as placeholders in [VariableExpr]s, and
 * as the qualifiers in [QualifiedKey]s.
 * Not all keys fall into this category, for example `HeapKey`s and `ExpressionKey`s do not.
 */
sealed class UnknownKey : Key(), QualifierRef {
    open val temporary: Boolean = false
    abstract val contextId: Context.ContextId
}

sealed class VariableKey(val variable: PsiVariable) : UnknownKey() {
    override val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(variable.type)
    override fun toString(): String = variable.toStringPretty()
    override fun equals(other: Any?): Boolean = other is VariableKey && this.variable == other.variable
    override fun hashCode(): Int = variable.hashCode()
}

class LocalVariableKey(
    variable: PsiLocalVariable,
    override val contextId: Context.ContextId
) : VariableKey(variable)

class ParameterKey(
    variable: PsiParameter,
    override val contextId: Context.ContextId,
    override val temporary: Boolean = false
) : VariableKey(variable)

class ThisKey(val type: PsiType, override val contextId: Context.ContextId) : UnknownKey() {
    override val temporary: Boolean = true
    override val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(type)
    override fun toString(): String = "this"
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other is ThisKey
}

class ReturnKey(override val ind: SetIndicator<*>, override val contextId: Context.ContextId) : UnknownKey() {
    override fun toString(): String = "Return value"
    override fun equals(other: Any?): Boolean = other is ReturnKey && this.ind == other.ind
    override fun hashCode(): Int = ind.hashCode()
}

/**
 * Things that can be qualifiers in a [QualifiedKey]. This is really just [HeapMarker]s and [UnknownKey]s.
 */
sealed interface QualifierRef {
    val ind: SetIndicator<*>
    fun isNew(): Boolean =
        this is HeapMarker || (this is QualifiedKey && this.qualifier.isNew())
}

class QualifiedKey(
    val field: FieldRef,
    val qualifier: QualifierRef,
    override val contextId: Context.ContextId
) : UnknownKey() {
    override val ind: SetIndicator<*> = this.field.ind
    override fun toString(): String = "$qualifier.$field"

    override fun equals(other: Any?): Boolean =
        other is QualifiedKey && this.field == other.field && this.qualifier == other.qualifier

    override fun hashCode(): Int = field.hashCode() * 31 + qualifier.hashCode()

    /**
     * This isn't a full key by itself, you'll need a [QualifierRef] as well and then will want to make a [QualifiedKey].
     */
    data class FieldRef(private val field: PsiField) {
        override fun toString(): String = field.toStringPretty()
        fun getElement(): PsiElement = field
        val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(field.type)
    }
}
