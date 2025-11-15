package com.oberdiah.deepcomplexity.context

import com.intellij.psi.*
import com.oberdiah.deepcomplexity.context.Context.KeyBackreference
import com.oberdiah.deepcomplexity.evaluation.ConstExpr
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LeafExpr
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import com.oberdiah.deepcomplexity.utilities.Utilities
import com.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty

/**
 * Not all keys fall into this category, for example [ExpressionKey]s do not.
 */
sealed class UnknownKey : Key() {
    open val lifetime: Lifetime = Lifetime.FOREVER

    fun shouldBeStripped(lifetimeToStrip: Lifetime): Boolean = this.lifetime.ordinal <= lifetimeToStrip.ordinal

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

    fun withAddedContextId(id: Context.ContextId): UnknownKey {
        return when (this) {
            is QualifiedFieldKey if qualifier is KeyBackreference ->
                QualifiedFieldKey(qualifier.withAddedContextId(id), field)

            else -> this
        }
    }

    fun isPlaceholder(): Boolean {
        return when (this) {
            is PlaceholderKey -> true
            is QualifiedFieldKey if qualifier is KeyBackreference -> qualifier.isPlaceholder()
            else -> false
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
data class PlaceholderKey(
    override val ind: ObjectIndicator,
    override val lifetime: Lifetime = Lifetime.BLOCK
) : UnknownKey() {
    override fun toString(): String = "PK(${ind.type.toStringPretty()})"
}

/**
 * Things that can be qualifiers in a [QualifiedFieldKey]. This is really just [HeapMarker]s and [Context.KeyBackreference]s.
 */
sealed interface Qualifier {
    val ind: Indicator<*>

    val lifetime: UnknownKey.Lifetime
        get() = UnknownKey.Lifetime.FOREVER

    /**
     * Turns this [Qualifier] into an expression by trying to resolve it against the given context.
     */
    fun safelyResolveUsing(context: MetaContext): Expr<*>

    /**
     * Turns this [Qualifier] directly into a leaf expression, so either it'll be a
     * [ConstExpr] or a
     * [VariableExpr]
     */
    fun toLeafExpr(): LeafExpr<*> = when (this) {
        is HeapMarker -> ConstExpr.fromHeapMarker(this)
        is KeyBackreference -> VariableExpr.new(this)
    }
}

data class QualifiedFieldKey(val qualifier: Qualifier, val field: Field) : UnknownKey() {
    override val ind: Indicator<*> = field.ind
    val qualifierInd: ObjectIndicator = qualifier.ind as ObjectIndicator
    override val lifetime: Lifetime = qualifier.lifetime

    override fun toString(): String = "$qualifier.$field"

    /**
     * This isn't a full key by itself, you'll need a [Qualifier] as well and then will want to make a [QualifiedFieldKey].
     */
    data class Field(private val field: PsiField) {
        override fun toString(): String = field.toStringPretty()
        fun getElement(): PsiElement = field
        val ind: Indicator<*> = Utilities.psiTypeToIndicator(field.type)
    }
}
