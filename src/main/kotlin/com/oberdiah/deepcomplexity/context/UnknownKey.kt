package com.oberdiah.deepcomplexity.context

import com.intellij.psi.*
import com.oberdiah.deepcomplexity.evaluation.ConstExpr
import com.oberdiah.deepcomplexity.evaluation.LeafExpr
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.into
import com.oberdiah.deepcomplexity.utilities.Utilities
import com.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty

/**
 * Not all keys fall into this category, for example [ExpressionKey]s do not.
 */
sealed class UnknownKey : Key() {
    open val lifetime: Lifetime = Lifetime.FOREVER

    fun shouldBeStripped(lifetimeToStrip: Lifetime): Boolean =
        isPlaceholder() || this.lifetime.ordinal <= lifetimeToStrip.ordinal

    enum class Lifetime {
        /**
         * The key will be removed as soon as it moves out of the block it was created in.
         */
        BLOCK,

        /**
         * The key will be removed as soon as it moves out of the method it was created in.
         */
        METHOD,

        /**
         * The key will never be removed.
         */
        FOREVER
    }

    fun isPlaceholder(): Boolean {
        return when (this) {
            is QualifiedFieldKey -> this.qualifier is ConstExpr && this.qualifier.isPlaceholder
            is VariableKey -> false
            is ThisKey -> false
            is ReturnKey -> false
        }
    }
}

sealed class VariableKey(val variable: PsiVariable) : UnknownKey() {
    override val ind: Indicator<*> = Utilities.psiTypeToIndicator(variable.type)
    override fun toString(): String = variable.toStringPretty()
    override fun equals(other: Any?): Boolean = other is VariableKey && this.variable == other.variable
    override fun hashCode(): Int = variable.hashCode()
}

class LocalVariableKey(variable: PsiLocalVariable) : VariableKey(variable)
class ParameterKey(variable: PsiParameter, override val lifetime: Lifetime = Lifetime.FOREVER) : VariableKey(variable)

data class ThisKey(val type: PsiType) : UnknownKey() {
    override val lifetime: Lifetime = Lifetime.METHOD
    override val ind: Indicator<*> = Utilities.psiTypeToIndicator(type)
    override fun toString(): String = "this"
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = other is ThisKey
}

data class ReturnKey(override val ind: Indicator<*>) : UnknownKey() {
    override fun toString(): String = "Return value"
}

data class QualifiedFieldKey(val qualifier: LeafExpr<HeapMarker>, val field: Field) : UnknownKey() {
    init {
        require(qualifier.ind is ObjectIndicator) {
            "Cannot create a qualified field key with a qualifier of indicator ${qualifier.ind}"
        }
    }

    override val ind: Indicator<*> = field.ind
    override val lifetime: Lifetime
        get() {
            return when (this.qualifier) {
                is VariableExpr<*> -> qualifier.key.lifetime
                else -> Lifetime.FOREVER
            }
        }

    override fun toString(): String = "$qualifier.$field"

    fun toPlaceholderKey(): QualifiedFieldKey =
        QualifiedFieldKey(ConstExpr.placeholderOf(qualifier.ind.into()), field)

    /**
     * This isn't a full key by itself, you'll need a qualifier as well and then will want to make a [QualifiedFieldKey].
     */
    data class Field(private val field: PsiField) {
        override fun toString(): String = field.toStringPretty()
        val ind: Indicator<*> = Utilities.psiTypeToIndicator(field.type)
    }
}
