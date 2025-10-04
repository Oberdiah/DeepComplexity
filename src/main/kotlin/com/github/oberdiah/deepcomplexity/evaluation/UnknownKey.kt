package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty
import com.intellij.psi.*

/**
 * [UnknownKey]s are keys that can be used as placeholders in [VariableExpr]s, and
 * on the left hand side of
 * Not all keys fall into this category, for example `HeapKey`s and `ExpressionKey`s do not.
 */
sealed class UnknownKey(val temporary: Boolean = false) : Key(), QualifierRef

sealed class VariableKey(val variable: PsiVariable, temporary: Boolean = false) : UnknownKey(temporary) {
    override val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(variable.type)
    override fun toString(): String = variable.toStringPretty()
    override fun equals(other: Any?): Boolean = other is VariableKey && this.variable == other.variable
    override fun hashCode(): Int = variable.hashCode()
}

class LocalVariableKey(variable: PsiLocalVariable) : VariableKey(variable)
class ParameterKey(variable: PsiParameter, temporary: Boolean = false) : VariableKey(variable, temporary)

/**
 * This isn't a full key by itself, you'll need a [QualifierRef] as well and then will want to make a [QualifiedKey].
 */
data class FieldRef(private val variable: PsiField) {
    override fun toString(): String = variable.toStringPretty()
    fun getElement(): PsiElement = variable
    val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(variable.type)
}

/**
 * Things that can be qualifiers in a [QualifiedKey]. This is really just [HeapMarker]s and [UnknownKey]s.
 */
sealed interface QualifierRef {
    val ind: SetIndicator<*>
    fun isNew(): Boolean =
        this is HeapMarker
                || (this is QualifiedKey && this.qualifier.isNew())
}

data class QualifiedKey(val field: FieldRef, val qualifier: QualifierRef) : UnknownKey() {
    override val ind: SetIndicator<*> = this.field.ind
    override fun toString(): String = "$qualifier.$field"
}

data class ThisKey(val type: PsiType) : UnknownKey(temporary = true) {
    override val ind: SetIndicator<*> = Utilities.psiTypeToSetIndicator(type)
    override fun toString(): String = "this"
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other is ThisKey
}

data class ReturnKey(override val ind: SetIndicator<*>) : UnknownKey() {
    override fun toString(): String = "Return value"
}