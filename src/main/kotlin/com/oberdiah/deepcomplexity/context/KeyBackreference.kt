package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator

/**
 * [KeyBackreference]s must be very carefully resolved; they cannot be resolved in any context
 * they were created in. Only [Context]s have the ability to create and resolve them.
 */
data class KeyBackreference(private val key: UnknownKey, private val contextId: ContextId) : Qualifier {
    override fun toString(): String = "$key'"
    override fun equals(other: Any?): Boolean = other is KeyBackreference && this.key == other.key
    override fun hashCode(): Int = key.hashCode()

    override val ind: Indicator<*> = key.ind
    fun withAddedContextId(newId: ContextId): KeyBackreference =
        KeyBackreference(key.withAddedContextId(newId), contextId + newId)

    /**
     * This shouldn't be used unless you know for certain you're in the evaluation stage;
     * using this in the method processing stage may lead to you resolving a key using your
     * own context, which is a recipe for disaster.
     */
    fun grabTheKeyYesIKnowWhatImDoingICanGuaranteeImInTheEvaluateStage(): UnknownKey = key

    override fun safelyResolveUsing(vars: Vars): Expr<*> {
        assert(!contextId.collidesWith(vars.idx)) {
            "Cannot resolve a KeyBackreference in the context it was created in."
        }

        return if (key is QualifiedFieldKey && key.qualifier is KeyBackreference) {
            vars.getExprCombiningQualifierAndField(key.qualifier.safelyResolveUsing(vars), key.field)
        } else {
            vars.get(key)
        }
    }

    fun isPlaceholder(): Boolean = key.isPlaceholder()
    override val lifetime: UnknownKey.Lifetime
        get() = key.lifetime
}