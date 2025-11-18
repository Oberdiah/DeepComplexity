package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator


sealed class LValue<T : Any> {
    abstract val ind: Indicator<T>
    abstract val contextId: ContextId?

    open val lifetime: UnknownKey.Lifetime
        get() = UnknownKey.Lifetime.FOREVER
}

data class LValueObject(val marker: HeapMarker, override val contextId: ContextId?) : LValue<HeapMarker>() {
    override val ind: Indicator<HeapMarker> = ObjectIndicator(marker.type)
}

/**
 * Represents an expression on the left-hand side in an assignment.
 *
 * If you've got a key you want to assign to, you can use this. It doesn't matter if it's
 * a [QualifiedFieldKey] or not.
 */
data class LValueKey<T : Any>(
    val key: UnknownKey,
    override val ind: Indicator<T>,
    override val contextId: ContextId?
) : LValue<T>(), Qualifier {
    companion object {
        fun new(key: UnknownKey, contextId: ContextId? = null): LValueKey<*> = LValueKey(key, key.ind, contextId)
    }

    override fun toString(): String = "$key'"
    override fun equals(other: Any?): Boolean = other is LValueKey<*> && this.key == other.key
    override fun hashCode(): Int = key.hashCode()

    override val lifetime: UnknownKey.Lifetime = key.lifetime

    override fun safelyResolveUsing(vars: Vars): Expr<*> {
        assert(!(contextId?.collidesWith(vars.idx) ?: false)) {
            "Cannot get an LValue on the same context it was created in."
        }

        return vars.get(vars.resolveKey(key))
    }

    fun withAddedContextId(id: ContextId): LValueKey<T> {
        return LValueKey(key.withAddedContextId(id), ind, contextId?.plus(id) ?: id)
    }
}

/**
 * For situations in which a simple key just isn't sufficient to describe the LValue.
 *
 * For example, the LValue `((x > 2) ? a : b).y`
 */
data class LValueField<T : Any>(
    val field: QualifiedFieldKey.Field,
    val qualifier: Expr<HeapMarker>,
    override val ind: Indicator<T>,
    override val contextId: ContextId?
) : LValue<T>() {
    companion object {
        fun new(
            field: QualifiedFieldKey.Field,
            qualifier: Expr<HeapMarker>,
            contextId: ContextId? = null
        ): LValueField<*> =
            LValueField(field, qualifier, field.ind, contextId)
    }
}