package com.oberdiah.deepcomplexity.evaluation

import com.intellij.psi.PsiTypes
import com.oberdiah.deepcomplexity.context.*
import com.oberdiah.deepcomplexity.context.Key.ExpressionKey
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.tryCastTo
import com.oberdiah.deepcomplexity.evaluation.IfExpr.Companion.new
import com.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet

sealed class Expr<T : Any>() {
    /**
     * This is used as a key in the caching system. If two expressions have the same key,
     * they are considered equal.
     *
     * Doing this works because all expressions are data classes, so equality ends up based on the
     * structure of the expression, not the reference.
     */
    val exprKey = ExpressionKey(this)

    /**
     * The indicator represents what the expression will be once evaluated.
     */
    abstract val ind: Indicator<T>

    final override fun toString(): String {
        return ExprToString.toString(this)
    }

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, it calls the replacer on every expression so you can make any modifications
     * you want.
     */
    fun rebuildTree(replacer: ExprTreeRebuilder.ExprReplacer, includeIfCondition: Boolean = true) =
        ExprTreeRebuilder.rebuildTree(this, replacer, includeIfCondition)

    fun <NewT : Any> replaceLeaves(replacer: LeafReplacer<NewT>): Expr<NewT> =
        ExprTreeRebuilder.replaceTreeLeaves(this, replacer)

    fun iterateTree(includeIfCondition: Boolean = false): Sequence<Expr<*>> =
        ExprTreeVisitor.iterateTree(this, includeIfCondition)

    internal inline fun <reified Q : Expr<*>> iterateTree(includeIfCondition: Boolean = false): Sequence<Q> =
        ExprTreeVisitor.iterateTree(this, includeIfCondition).filterIsInstance<Q>()

    fun iterateLeaves(includeIfCondition: Boolean = false): Sequence<LeafExpr<T>> =
        ExprTreeVisitor.iterateTree(this, includeIfCondition).filterIsInstance<LeafExpr<T>>()

    fun getVariables(): Set<VariableExpr<*>> = iterateTree<VariableExpr<*>>().toSet()

    fun resolveUnknowns(mCtx: Context): Expr<T> =
        mCtx.resolveKnownVariables(this)

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, whenever it encounters an expression of type [Q],
     * it replaces it with the result of calling [replacement] on it. If [replacement] returns null,
     * it will skip the replacement.
     */
    inline fun <reified Q> replaceTypeInTree(crossinline replacement: (Q) -> Expr<*>?): Expr<T> {
        return rebuildTree(ExprTreeRebuilder.ExprReplacer { expr ->
            if (expr is Q) {
                replacement(expr) ?: expr
            } else {
                expr
            }
        })
    }


    /**
     * Swaps out the leaves of the expression. Every leaf of the expression must have type [Q].
     * An ergonomic and slightly constrained version of [com.oberdiah.deepcomplexity.evaluation.Expr.replaceLeaves].
     *
     * Will assume everything you return has type [newInd], and throw an exception if that is not true. This
     * is mainly for ergonomic reasons, so you don't have to do the casting yourself.
     */
    internal inline fun <reified Q : Expr<T>> replaceTypeInLeaves(
        newInd: Indicator<*>,
        crossinline replacement: (Q) -> Expr<*>
    ): Expr<*> {
        return object {
            operator fun <T : Any> invoke(
                newInd: Indicator<T>,
            ): Expr<T> {
                return replaceTypeInLeavesWithInd<Q, T>(newInd, replacement)
            }
        }(newInd)
    }

    private inline fun <reified Q : Expr<T>, B : Any> replaceTypeInLeavesWithInd(
        newInd: Indicator<B>,
        crossinline replacement: (Q) -> Expr<*>
    ): Expr<B> {
        return replaceLeaves { expr ->
            val newExpr = if (expr is Q) {
                replacement(expr)
            } else {
                throw IllegalArgumentException(
                    "Expected ${Q::class.simpleName}, got ${expr::class.simpleName}"
                )
            }

            newExpr.tryCastTo(newInd) ?: throw IllegalStateException(
                "(${newExpr.ind} != $newInd) $newExpr does not match $expr"
            )
        }
    }

    /**
     * Recursively calls [simplify] on every node in the tree, rebuilding the tree as it goes.
     */
    fun optimise(): Expr<T> = rebuildTree(ExprTreeRebuilder.ExprReplacer { it.simplify() })

    /**
     * Simplifies the expression if possible.
     * Does not operate recursively.
     */
    open fun simplify(): Expr<T> = this

    fun evaluate(scope: ExprEvaluate.Scope): Bundle<T> = ExprEvaluate.evaluate(this, scope)
    fun dStr(): String = ExprToString.toDebugString(this)
}

fun <T : Number> Expr<T>.getNumberIndicator() = ind as NumberIndicator<T>

/**
 * Represents a link to an entire context. If it's null it represents the meta context's
 * personal `ctx` value.
 */
data class VarsExpr(val vars: Vars? = null) : Expr<VarsMarker>() {
    companion object {
        const val STRING_PLACEHOLDER = "##VarsExpr##"
    }

    fun map(operation: (Vars) -> Vars): VarsExpr = VarsExpr(vars?.let { operation(it) })

    override val ind: VarsIndicator = VarsIndicator
}

data class ArithmeticExpr<T : Number>(
    val lhs: Expr<T>,
    val rhs: Expr<T>,
    val op: BinaryNumberOp,
) : Expr<T>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Adding expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override val ind: Indicator<T>
        get() = lhs.ind
}


@ConsistentCopyVisibility
data class ComparisonExpr<T : Any> private constructor(
    val lhs: Expr<T>,
    val rhs: Expr<T>,
    val comp: ComparisonOp,
) : Expr<Boolean>() {
    companion object {
        fun <T : Any> newRaw(lhs: Expr<T>, rhs: Expr<T>, comp: ComparisonOp): Expr<Boolean> =
            ComparisonExpr(lhs, rhs, comp)

        /**
         * Compile-time casts [rhs] for you so you don't have to worry about it. If you provide
         * a wrongly typed [rhs] you'll get a runtime exception.
         */
        fun <A : Any> new(lhs: Expr<A>, rhs: Expr<*>, comp: ComparisonOp): Expr<Boolean> {
            val rhs = rhs.castOrThrow(lhs.ind)
            return StaticExpressionAnalysis.attemptToSimplifyComparison(lhs, rhs, comp)
        }
    }

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, comp)

    init {
        assert(lhs.ind == rhs.ind) {
            "Comparing expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override val ind: Indicator<Boolean>
        get() = BooleanIndicator
}

/**
 * Tries to cast the expression to the given set indicator.
 * Does nothing if the expression is already of the given type.
 *
 * Given that there's an assumption baked into all of this that we're working on a compilable program,
 * explicit isn't strictly necessary, but it's nice debugging and printing purposes.
 */
data class TypeCastExpr<T : Any, Q : Any>(
    val expr: Expr<Q>,
    override val ind: Indicator<T>,
    val explicit: Boolean,
) : Expr<T>()

@ConsistentCopyVisibility
data class IfExpr<T : Any> private constructor(
    val trueExpr: Expr<T>,
    val falseExpr: Expr<T>,
    val thisCondition: Expr<Boolean>,
) : Expr<T>() {
    init {
        assert(trueExpr.ind == falseExpr.ind) {
            "Incompatible types in if statement: ${trueExpr.ind} and ${falseExpr.ind}"
        }
    }

    override val ind: Indicator<T>
        get() = trueExpr.ind

    override fun simplify(): Expr<T> = new(trueExpr, falseExpr, thisCondition)

    companion object {
        /**
         * Like [new], but perform on-the-fly optimisations. Should only really be used during tree traversal.
         *
         * A tree replacement traversal relies on the only changes coming from the replacer itself -
         * if we did our own optimisations, we could end up in a situation where a replacer sees the same
         * expression more than once.
         *
         * For example,
         * ```kotlin
         * if (x > 6) {
         *      if (x == x) {
         *          a
         *      } else {
         *          b
         *      }
         * }
         * ```
         * If we ran the above through an identity replacer it would end up seeing `a` twice. Once when it parsed
         * the original leaf node, and then again once the inner if was optimised away. This would obviously
         * be incorrect.
         */
        fun <T : Any> newRaw(trueExpr: Expr<T>, falseExpr: Expr<T>, condition: Expr<Boolean>): IfExpr<T> =
            IfExpr(trueExpr, falseExpr, condition)

        /**
         * Compile-time casts [falseExpr] for you so you don't have to worry about it. If you provide
         * a wrongly typed [falseExpr] you'll get a runtime exception.
         */
        fun <A : Any> new(trueExpr: Expr<A>, falseExpr: Expr<*>, condition: Expr<Boolean>): Expr<A> {
            val falseExpr = falseExpr.castOrThrow(trueExpr.ind)
            return StaticExpressionAnalysis.attemptToSimplifyIfExpr(trueExpr, falseExpr, condition)
        }
    }
}

data class UnionExpr<T : Any>(val lhs: Expr<T>, val rhs: Expr<T>) : Expr<T>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Unioning expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override val ind: Indicator<T>
        get() = lhs.ind
}

@ConsistentCopyVisibility
data class BooleanExpr private constructor(val lhs: Expr<Boolean>, val rhs: Expr<Boolean>, val op: BooleanOp) :
    Expr<Boolean>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Boolean expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override val ind: Indicator<Boolean>
        get() = BooleanIndicator

    companion object {
        fun newRaw(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): BooleanExpr =
            BooleanExpr(lhs, rhs, op)

        fun new(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
            return StaticExpressionAnalysis.attemptToSimplifyBooleanExpr(lhs, rhs, op)
        }
    }

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, op)
}

data class BooleanInvertExpr(val expr: Expr<Boolean>) : Expr<Boolean>() {
    override val ind: Indicator<Boolean>
        get() = BooleanIndicator
}

data class NegateExpr<T : Number>(val expr: Expr<T>) : Expr<T>() {
    override val ind: Indicator<T>
        get() = expr.ind
}

data class NumIterationTimesExpr<T : Number>(
    // How the variable is constrained; if the variable changes such that this returns false,
    // the loop will end.
    val constraint: NumberSet<T>,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpr<T>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms<T>,
) : Expr<T>() {
    override val ind: Indicator<T>
        get() = TODO("Not yet implemented")

    companion object {
        fun <T : Number> new(
            constraint: NumberSet<T>,
            variable: VariableExpr<out Number>,
            terms: ConstraintSolver.CollectedTerms<out Number>
        ): NumIterationTimesExpr<T> {
            val indicator = constraint.ind

            assert(indicator == variable.ind) {
                "Variable and constraint have different set indicators: ${variable.ind} and $indicator"
            }
            assert(indicator == terms.ind) {
                "Variable and terms have different set indicators: ${variable.ind} and ${terms.ind}"
            }

            @Suppress("UNCHECKED_CAST")
            return NumIterationTimesExpr(
                constraint,
                variable as VariableExpr<T>,
                terms as ConstraintSolver.CollectedTerms<T>
            )
        }
    }
}

sealed class LeafExpr<T : Any> : Expr<T>() {
    abstract val resolvesTo: ResolvesTo<T>
    override val ind: Indicator<T>
        get() = resolvesTo.ind
}

/**
 * This represents a standard primitive which we do not know the value of yet.
 *
 * Related to a specific context (The context that created it).
 * This context is only used for ensuring proper usage, it's never used within the logic.
 */
@ConsistentCopyVisibility
data class VariableExpr<T : Any> private constructor(
    override val resolvesTo: ResolvesTo<T>
) : LeafExpr<T>() {
    companion object {
        /**
         * This should only ever be called from a [Context]. Only contexts are allowed
         * to create [VariableExpr]s. Only contexts really can, anyway, because they've got control
         * of the [KeyBackreference]s.
         */
        fun <T : Any> new(resolvesTo: ResolvesTo<T>): VariableExpr<T> = VariableExpr(resolvesTo)
        fun new(key: UnknownKey, contextId: ContextId): VariableExpr<*> =
            VariableExpr(KeyBackreference.new(key, contextId))
    }

    data class KeyBackreference<T : Any>(
        private val key: UnknownKey,
        private val contextId: ContextId,
        override val ind: Indicator<T>
    ) : ResolvesTo<T> {
        companion object {
            fun new(key: UnknownKey, contextId: ContextId): KeyBackreference<*> =
                KeyBackreference(key, contextId, key.ind)

            fun <T : Any> new(key: UnknownKey, contextId: ContextId, indicator: Indicator<T>): KeyBackreference<T> =
                KeyBackreference(key, contextId, indicator)
        }

        override fun toLeafExpr(): Expr<T> = new(this)
        override fun toString(): String = "$key'"
        override fun equals(other: Any?): Boolean = other is KeyBackreference<*> && this.key == other.key
        override fun hashCode(): Int = key.hashCode()
        override fun isPlaceholder() = key.isPlaceholder()
        override val lifetime = key.lifetime

        override fun withAddedContextId(newId: ContextId): KeyBackreference<T> =
            KeyBackreference(key.withAddedContextId(newId), contextId + newId, ind)

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
            return vars.get(vars.resolveKey(key))
        }
    }
}

/**
 * Objects are represented as a [ConstExpr] with an underlying [com.oberdiah.deepcomplexity.context.HeapMarker].
 */
@ConsistentCopyVisibility
data class ConstExpr<T : Any> private constructor(override val resolvesTo: DataContainer<T>) : LeafExpr<T>() {
    data class DataContainer<T : Any>(val v: T, override val ind: Indicator<T>) : ResolvesTo<T> {
        override fun toString(): String = v.toString()
        override fun toLeafExpr(): Expr<T> = ConstExpr(this)
    }

    val value = resolvesTo.v

    companion object {
        fun <T : Any> new(value: T, indicator: Indicator<T>): ConstExpr<T> = ConstExpr(DataContainer(value, indicator))

        val TRUE = new(true, BooleanIndicator)
        val FALSE = new(false, BooleanIndicator)
        val VOID = fromHeapMarker(HeapMarker.new(PsiTypes.voidType()))

        fun <T : Number> zero(ind: NumberIndicator<T>): ConstExpr<T> = new(ind.getZero(), ind)
        fun <T : Number> one(ind: NumberIndicator<T>): ConstExpr<T> = new(ind.getOne(), ind)
        fun <T : Any> fromAny(value: T): ConstExpr<T> = new(value, Indicator.fromValue(value))
        fun fromHeapMarker(marker: HeapMarker): ConstExpr<HeapMarker> = fromAny(marker)
    }
}