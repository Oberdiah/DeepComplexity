package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.IfExpr.Companion.new
import com.github.oberdiah.deepcomplexity.evaluation.Key.ExpressionKey
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.intellij.psi.PsiTypes

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
    abstract val ind: SetIndicator<T>

    final override fun toString(): String {
        return ExprToString.toString(this)
    }

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, it calls the replacer on every expression so you can make any modifications
     * you want.
     */
    fun rebuildTree(replacer: ExprTreeRebuilder.Replacer, includeIfCondition: Boolean = true) =
        ExprTreeRebuilder.rebuildTree(this, replacer, includeIfCondition)

    fun <NewT : Any> replaceLeaves(replacer: ExprTreeRebuilder.LeafReplacer<NewT>): Expr<NewT> =
        ExprTreeRebuilder.replaceTreeLeaves(this, replacer)

    fun iterateTree(includeIfCondition: Boolean = false): Sequence<Expr<*>> =
        ExprTreeVisitor.iterateTree(this, includeIfCondition)

    fun getVariables(): Set<VariableExpr<*>> = iterateTree()
        .filterIsInstance<VariableExpr<*>>()
        .toSet()

    fun resolveUnknowns(context: Context): Expr<T> =
        context.resolveKnownVariables(this)

    fun containsUnknowns(): Boolean = iterateTree().any { it is VariableExpr<*> }

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, whenever it encounters an expression of type [Q],
     * it replaces it with the result of calling [replacement] on it. The replacement expression
     * has a wild generic type for ergonomic reasons because you're likely not going to have
     * a typed replacement on hand, so we do the type check and cast at runtime for you.
     */
    inline fun <reified Q> replaceTypeInTree(crossinline replacement: (Q) -> Expr<*>?): Expr<T> {
        return rebuildTree(object : ExprTreeRebuilder.Replacer {
            override fun <T : Any> replace(expr: Expr<T>): Expr<T> {
                if (expr is Q) {
                    val resolved = replacement(expr)
                    if (resolved != null) {
                        return resolved.tryCastTo(expr.ind)
                            ?: throw IllegalStateException(
                                "(${resolved.ind} != ${expr.ind}) ${resolved.dStr()} does not match ${expr.dStr()}"
                            )
                    }
                }

                return expr
            }
        })
    }

    /**
     * Recursively calls [simplify] on every node in the tree, rebuilding the tree as it goes.
     */
    fun optimise(): Expr<T> {
        return rebuildTree(object : ExprTreeRebuilder.Replacer {
            override fun <T : Any> replace(expr: Expr<T>): Expr<T> {
                return expr.simplify()
            }
        })
    }

    /**
     * Simplifies the expression if possible.
     * Does not operate recursively.
     */
    open fun simplify(): Expr<T> = this

    fun evaluate(scope: ExprEvaluate.Scope): Bundle<T> = ExprEvaluate.evaluate(this, scope)
    fun dStr(): String = ExprToString.toDebugString(this)
}

fun <T : Number> Expr<T>.getNumberSetIndicator() = ind as NumberSetIndicator<T>

fun Expr<*>.castToNumbers(): Expr<out Number> {
    if (this.ind is NumberSetIndicator<*>) {
        @Suppress("UNCHECKED_CAST")
        return this as Expr<out Number>
    } else {
        throw IllegalStateException("Failed to cast to a number: $this ($ind)")
    }
}

fun Expr<*>.castToBoolean(): Expr<Boolean> {
    return this.tryCastTo(BooleanSetIndicator)
        ?: throw IllegalStateException("Failed to cast to a boolean: $this ($ind)")
}

inline fun <Set : Any, reified T : Expr<Set>> Expr<*>.tryCastToReified(indicator: SetIndicator<Set>): T? {
    return if (this::class == T::class && indicator == this.ind) {
        @Suppress("UNCHECKED_CAST")
        this as T
    } else {
        null
    }
}

/**
 * Basically a nicer way of doing `this as Expr<T>`, but with type checking :)
 */
fun <T : Any> Expr<*>.tryCastTo(indicator: SetIndicator<T>): Expr<T>? {
    return if (this.ind == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as Expr<T>
    } else {
        null
    }
}

fun <T : Any> Expr<*>.castOrThrow(indicator: SetIndicator<T>): Expr<T> {
    return this.tryCastTo(indicator)
        ?: throw IllegalStateException("Failed to cast to $indicator: $this (${this.ind})")
}

/**
 * Wrap the expression in a type cast to the given indicator.
 */
fun <T : Any> Expr<*>.castToUsingTypeCast(indicator: SetIndicator<T>, explicit: Boolean): Expr<T> {
    return if (this.ind == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as Expr<T>
    } else {
        TypeCastExpr(this, indicator, explicit)
    }
}

fun Expr<*>.getField(context: Context, field: QualifiedKey.Field): Expr<*> {
    return replaceTypeInLeaves<LeafExpr<*>>(field.ind) {
        context.getVar(QualifiedKey(field, it.underlying as Qualifier))
    }
}

/**
 * Swaps out the leaves of the expression. Every leaf of the expression must have type [Q].
 * An ergonomic and slightly constrained version of [replaceLeaves].
 *
 * Will assume everything you return has type [newInd], and throw an exception if that is not true. This
 * is mainly for ergonomic reasons, so you don't have to do the casting yourself.
 */
inline fun <reified Q> Expr<*>.replaceTypeInLeaves(
    newInd: SetIndicator<*>,
    crossinline replacement: (Q) -> Expr<*>
): Expr<*> {
    return object {
        inline operator fun <T : Any> invoke(
            newInd: SetIndicator<T>,
            crossinline replacement: (Q) -> Expr<*>
        ): Expr<*> {
            return replaceLeaves(ExprTreeRebuilder.LeafReplacer(newInd) { expr ->
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
            })
        }
    }(newInd, replacement)
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

    override val ind: SetIndicator<T>
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

        fun <T : Any> new(lhs: Expr<T>, rhs: Expr<T>, comp: ComparisonOp): Expr<Boolean> {
            StaticExpressionAnalysis.attemptToSimplifyComparison(lhs, rhs, comp)?.let {
                return it
            }
            return ComparisonExpr(lhs, rhs, comp)
        }
    }

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, comp)

    init {
        assert(lhs.ind == rhs.ind) {
            "Comparing expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override val ind: SetIndicator<Boolean>
        get() = BooleanSetIndicator
}

/**
 * Represents where, in an expression, the rest of the method is considered to continue from.
 *
 * For example, we would represent
 * ```
 * if (y > 10) {
 * 	a.x += 1;
 * 	return;
 * }
 * a.x = a.x + 2;
 * ```
 * as
 * ```
 * (y > 10) ? (a.x` + 1) : REM(a.x` + 2)
 * ```
 *
 * A method without any control flow will always just have a single [RestOfMethodExpr] wrapping its entire
 * expression.
 */
data class RestOfMethodExpr<T : Any>(val expr: Expr<T>) : Expr<T>() {
    override val ind: SetIndicator<T> = expr.ind
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
    override val ind: SetIndicator<T>,
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

    override val ind: SetIndicator<T>
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

        fun <A : Any, B : Any> new(
            trueExpr: Expr<A>,
            falseExpr: Expr<B>,
            condition: Expr<Boolean>
        ): Expr<A> {
            val falseExpr = falseExpr.tryCastTo(trueExpr.ind)
                ?: throw IllegalStateException("Incompatible types in if statement: ${trueExpr.ind} and ${falseExpr.ind}")

            if (condition == ConstExpr.TRUE) {
                return trueExpr
            } else if (condition == ConstExpr.FALSE) {
                return falseExpr
            }

            return IfExpr(trueExpr, falseExpr, condition)
        }
    }
}

data class UnionExpr<T : Any>(val lhs: Expr<T>, val rhs: Expr<T>) : Expr<T>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Unioning expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override val ind: SetIndicator<T>
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

    override val ind: SetIndicator<Boolean>
        get() = BooleanSetIndicator

    companion object {
        fun newRaw(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): BooleanExpr =
            BooleanExpr(lhs, rhs, op)

        fun new(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
            StaticExpressionAnalysis.attemptToSimplifyBooleanExpr(lhs, rhs, op)?.let {
                return it
            }
            return BooleanExpr(lhs, rhs, op)
        }
    }

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, op)
}

sealed class LValueExpr<T : Any> : Expr<T>() {
    fun castToNumbers(): LValueExpr<out Number> = (this as Expr<*>).castToNumbers() as LValueExpr<out Number>

    /**
     * Resolves the expression in the given context, converting it from an LValueExpr that can be assigned to,
     * into whatever underlying expr it represents.
     */
    abstract fun resolve(context: Context): Expr<T>
}

/**
 * Represents an expression on the left-hand side in an assignment.
 *
 * If you've got a key you want to assign to, you can use this. It doesn't matter if it's
 * a [QualifiedKey] or not.
 */
data class LValueKeyExpr<T : Any>(val key: UnknownKey, override val ind: SetIndicator<T>) : LValueExpr<T>() {
    companion object {
        fun new(key: UnknownKey): LValueKeyExpr<*> = LValueKeyExpr(key, key.ind)
    }

    override fun resolve(context: Context): Expr<T> {
        return context.getVar(key).tryCastTo(ind)!!
    }
}

/**
 * For situations in which a simple key just isn't sufficient to describe the LValue.
 *
 * For example, the LValue `((x > 2) ? a : b).y`
 */
data class LValueFieldExpr<T : Any>(
    val field: QualifiedKey.Field,
    val qualifier: Expr<*>,
    override val ind: SetIndicator<T>
) : LValueExpr<T>() {
    companion object {
        fun new(field: QualifiedKey.Field, qualifier: Expr<*>): LValueFieldExpr<*> =
            LValueFieldExpr(field, qualifier, field.ind)
    }

    override fun resolve(context: Context): Expr<T> {
        return qualifier.getField(context, field).tryCastTo(ind)!!
    }
}

data class BooleanInvertExpr(val expr: Expr<Boolean>) : Expr<Boolean>() {
    override val ind: SetIndicator<Boolean>
        get() = BooleanSetIndicator
}

data class NegateExpr<T : Number>(val expr: Expr<T>) : Expr<T>() {
    override val ind: SetIndicator<T>
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
    override val ind: SetIndicator<T>
        get() = TODO("Not yet implemented")

    companion object {
        fun <T : Number> new(
            constraint: NumberSet<T>,
            variable: VariableExpr<out Number>,
            terms: ConstraintSolver.CollectedTerms<out Number>
        ): NumIterationTimesExpr<T> {
            val setIndicator = constraint.ind

            assert(setIndicator == variable.ind) {
                "Variable and constraint have different set indicators: ${variable.ind} and $setIndicator"
            }
            assert(setIndicator == terms.ind) {
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
    abstract val underlying: Any
}

/**
 * This represents a standard primitive which we do not know the value of yet.
 *
 * Related to a specific context (The context that created it).
 * This context is only used for ensuring proper usage, it's never used within the logic.
 */
@ConsistentCopyVisibility
data class VariableExpr<T : Any> private constructor(
    override val underlying: Context.KeyBackreference,
    override val ind: SetIndicator<T>
) : LeafExpr<T>() {
    val key: Context.KeyBackreference = underlying

    companion object {
        /**
         * This should only ever be called from a [Context]. Only contexts are allowed
         * to create [VariableExpr]s. Only contexts really can, anyway, because they've got control
         * of the [Context.KeyBackreference]s.
         */
        fun new(key: Context.KeyBackreference): VariableExpr<*> = VariableExpr(key, key.ind)
    }
}

/**
 * Objects are represented as a [ConstExpr] with an underlying [HeapMarker].
 */
data class ConstExpr<T : Any>(override val underlying: T, override val ind: SetIndicator<T>) : LeafExpr<T>() {
    val value: T = underlying

    companion object {
        val TRUE = ConstExpr(true, BooleanSetIndicator)
        val FALSE = ConstExpr(false, BooleanSetIndicator)
        val VOID = fromHeapMarker(HeapMarker.new(PsiTypes.voidType()))
        val NULL = fromHeapMarker(HeapMarker.new(PsiTypes.nullType()))

        fun <T : Number> zero(ind: NumberSetIndicator<T>): ConstExpr<T> =
            ConstExpr(ind.getZero(), ind)

        fun <T : Number> one(ind: NumberSetIndicator<T>): ConstExpr<T> =
            ConstExpr(ind.getOne(), ind)

        fun <T : Any> fromAny(value: T): ConstExpr<T> = ConstExpr(value, SetIndicator.fromValue(value))
        fun fromHeapMarker(marker: HeapMarker): ConstExpr<HeapMarker> = fromAny(marker)
    }
}