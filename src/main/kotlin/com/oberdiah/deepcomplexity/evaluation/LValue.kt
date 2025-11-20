package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.context.QualifiedFieldKey
import com.oberdiah.deepcomplexity.context.UnknownKey
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator

sealed class LValue<T : Any> {
    abstract val ind: Indicator<T>
}

/**
 * Represents an expression on the left-hand side in an assignment.
 *
 * If you've got a key you want to assign to, you can use this. It doesn't matter if it's
 * a [QualifiedFieldKey] or not.
 */
data class LValueKey<T : Any>(val key: UnknownKey, override val ind: Indicator<T>) : LValue<T>() {
    companion object {
        fun new(key: UnknownKey): LValueKey<*> = LValueKey(key, key.ind)
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
    override val ind: Indicator<T>
) : LValue<T>() {
    companion object {
        fun new(field: QualifiedFieldKey.Field, qualifier: Expr<HeapMarker>): LValueField<*> =
            LValueField(field, qualifier, field.ind)
    }
}
