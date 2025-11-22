package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.ConstExpr
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import javaslang.control.Either

/**
 * [ResolvesTo]s must be very carefully resolved; they cannot be resolved in any context
 * they were created in. Only [Context]s have the ability to create and resolve them.
 */
data class ResolvesTo<T : Any>(
    private val v: Either<UnknownKey, T>,
    private val contextId: ContextId,
    val ind: Indicator<T>
) {
    companion object {
        fun new(key: UnknownKey, contextId: ContextId): ResolvesTo<*> =
            ResolvesTo(Either.left(key), contextId, key.ind)

        fun <T : Any> new(key: UnknownKey, contextId: ContextId, indicator: Indicator<T>): ResolvesTo<T> =
            ResolvesTo(Either.left(key), contextId, indicator)

        fun <T : Any> fromValue(value: T, contextId: ContextId): ResolvesTo<T> =
            ResolvesTo(Either.right(value), contextId, Indicator.fromValue(value))
    }

    fun toLeafExpr(): Expr<T> = v.fold(
        { VariableExpr.new(this) },
        { ConstExpr.fromAny(it) }
    )

    override fun toString(): String = "$v'"
    override fun equals(other: Any?): Boolean = other is ResolvesTo<*> && this.v == other.v
    override fun hashCode(): Int = v.hashCode()

    fun withAddedContextId(newId: ContextId): ResolvesTo<T> =
        ResolvesTo(v.mapLeft { it.withAddedContextId(newId) }, contextId + newId, ind)

    /**
     * This shouldn't be used unless you know for certain you're in the evaluation stage;
     * using this in the method processing stage may lead to you resolving a key using your
     * own context, which is a recipe for disaster.
     */
    fun grabTheKeyYesIKnowWhatImDoingICanGuaranteeImInTheEvaluateStage(): UnknownKey {
        return v.swap().get()
    }

    fun safelyResolveUsing(vars: Vars): Expr<*> {
        assert(!contextId.collidesWith(vars.idx)) {
            "Cannot resolve a KeyBackreference in the context it was created in."
        }
        return v.fold(
            { vars.get(vars.resolveKey(it)) },
            { ConstExpr.fromAny(it) }
        )
    }

    fun isPlaceholder(): Boolean = v.isLeft && v.left.isPlaceholder()
    val lifetime: UnknownKey.Lifetime
        get() = v.fold({ it.lifetime }, { UnknownKey.Lifetime.FOREVER })
}