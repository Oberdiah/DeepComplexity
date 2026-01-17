package com.oberdiah.deepcomplexity.evaluation

import com.intellij.psi.PsiTypes
import com.oberdiah.deepcomplexity.context.Context
import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.context.Key.ExpressionKey
import com.oberdiah.deepcomplexity.context.UnknownKey
import com.oberdiah.deepcomplexity.context.Vars
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.IfExpr.Companion.new
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle

sealed class Expr<T : Any> {
    init {
        require(ExprPool.isCreating()) {
            "Expressions must be created via ExprPool.create() to ensure proper pooling."
        }
    }

    fun assignInternId(id: Long) {
        require(transientInternIdDoNotUse == 0L) {
            "Intern id is already set for $this"
        }
        transientInternIdDoNotUse = id
    }

    /**
     * This is marked do not use as it's *not* unique for the lifetime of the program and may change if the
     * expression doesn't exist and gets garbage collected.
     * It's fine for use so long as this Expression object itself still exists, which is why these internal uses are
     * fine, but that's a restriction confusing enough to caution against usage entirely.
     * If you want something to use as a key, either use [exprKey] or better yet use this object directly as your key.
     */
    var transientInternIdDoNotUse: Long = 0L

    final override fun hashCode(): Int {
        return transientInternIdDoNotUse.hashCode()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Expr<*>) return false
        if (this.javaClass != other.javaClass) return false
        return this.transientInternIdDoNotUse == other.transientInternIdDoNotUse
    }

    fun myParts(): List<Any> = parts() + ind

    /**
     * Subclasses should return their properties here for equality/hashcode purposes.
     */
    protected abstract fun parts(): List<Any>

    /**
     * So we can treat expressions as variables in their own right in the constraint system.
     */
    val exprKey get() = ExpressionKey(this)

    /**
     * The indicator represents what the expression will be once evaluated.
     */
    abstract val ind: Indicator<T>

    final override fun toString(): String {
        return ExprToString.toString(this)
    }

    fun iterateTree(ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches): Sequence<Expr<*>> =
        ExprTreeVisitor.iterateTree(this, ifTraversal)

    internal inline fun <reified Q : Expr<*>> iterateTree(ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches): Sequence<Q> =
        ExprTreeVisitor.iterateTree(this, ifTraversal).filterIsInstance<Q>()

    fun iterateLeaves(ifTraversal: IfTraversal = IfTraversal.BranchesOnly): Sequence<LeafExpr<T>> =
        ExprTreeVisitor.iterateTree(this, ifTraversal).filterIsInstance<LeafExpr<T>>()

    fun resolveUnknowns(mCtx: Context): Expr<T> =
        mCtx.resolveKnownVariables(this)

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, whenever it encounters an expression of type [Q],
     * it replaces it with the result of calling [replacement] on it. If [replacement] returns null,
     * it will skip the replacement.
     *
     * [replacement] must return an expression with the same indicator as it consumes. If you don't want
     * that constraint, use [replaceTypeInTree] instead.
     */
    inline fun <reified Q> swapInplaceTypeInTree(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        crossinline replacement: (Q) -> Expr<*>?
    ): Expr<T> {
        return ExprTreeRebuilder.swapInplaceInTree(this, ifTraversal) { expr: Expr<*> ->
            if (expr is Q) {
                replacement(expr) ?: expr
            } else {
                expr
            }
        }
    }

    inline fun <reified Q> replaceTypeInTree(
        ifTraversal: IfTraversal = IfTraversal.ConditionAndBranches,
        crossinline replacement: (Q) -> Expr<*>?
    ): Expr<*> {
        return ExprTreeRebuilder.rebuildTree(this, ifTraversal) { expr: Expr<*> ->
            if (expr is Q) {
                replacement(expr) ?: expr
            } else {
                expr
            }
        }
    }

    /**
     * Recursively calls [simplify] on every node in the tree, rebuilding the tree as it goes.
     */
    fun optimise(): Expr<T> = ExprTreeRebuilder.swapInplaceInTree(this) { it.simplify() }

    /**
     * Simplifies the expression if possible.
     * Does not operate recursively.
     */
    open fun simplify(): Expr<T> = this

    fun evaluate(scope: ExprEvaluate.Scope): Bundle<T> = ExprEvaluate.evaluate(this, scope)
    fun dStr(): String = ExprToString.toDebugString(this)
}

/**
 * An expression with an [lhs] and [rhs]
 */
sealed interface AnyBinaryExpr<T : Any> {
    val lhs: Expr<T>
    val rhs: Expr<T>
}

fun <T : Number> Expr<T>.getNumberIndicator() = ind as NumberIndicator<T>

/**
 * Represents a link to an entire context of variables.
 */
class VarsExpr private constructor(val vars: DynamicOrStatic = DynamicOrStatic.Dynamic) : Expr<VarsMarker>() {
    companion object {
        const val STRING_PLACEHOLDER = "##VarsExpr##"
        fun new(vars: DynamicOrStatic = DynamicOrStatic.Dynamic): VarsExpr = ExprPool.create<VarsExpr>(vars)
    }

    override fun parts(): List<Any> = listOf(vars)

    @Suppress("unused")
    val isStatic = vars is DynamicOrStatic.Static
    val isDynamic = vars is DynamicOrStatic.Dynamic

    fun map(operation: (Vars) -> Vars): VarsExpr = new(
        when (vars) {
            is DynamicOrStatic.Dynamic -> DynamicOrStatic.Dynamic
            is DynamicOrStatic.Static -> DynamicOrStatic.Static(operation(vars.vars))
        }
    )

    override val ind: VarsIndicator = VarsIndicator

    sealed class DynamicOrStatic {
        /**
         * Representing the dynamic variables already stored in the context.
         */
        object Dynamic : DynamicOrStatic() {
            override fun toString(): String = STRING_PLACEHOLDER
        }

        /**
         * A static set of variables that's been locked in by a 'return'.
         */
        class Static(val vars: Vars) : DynamicOrStatic() {
            override fun toString(): String = vars.toString()
        }
    }
}

class ArithmeticExpr<T : Number> private constructor(
    override val lhs: Expr<T>,
    override val rhs: Expr<T>,
    val op: BinaryNumberOp,
) : Expr<T>(), AnyBinaryExpr<T> {
    companion object {
        fun <T : Number> new(lhs: Expr<T>, rhs: Expr<T>, op: BinaryNumberOp): ArithmeticExpr<T> =
            ExprPool.create(lhs, rhs, op)
    }

    init {
        require(lhs.ind == rhs.ind) {
            "Adding expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override fun parts(): List<Any> = listOf(lhs, rhs, op)

    override val ind: Indicator<T> = lhs.ind
}


class ComparisonExpr<T : Any> private constructor(
    override val lhs: Expr<T>,
    override val rhs: Expr<T>,
    val comp: ComparisonOp,
) : Expr<Boolean>(), AnyBinaryExpr<T> {
    companion object {
        fun <T : Any> newRaw(lhs: Expr<T>, rhs: Expr<T>, comp: ComparisonOp): ComparisonExpr<T> =
            ExprPool.create(lhs, rhs, comp)

        /**
         * Compile-time casts [rhs] for you so you don't have to worry about it. If you provide
         * a wrongly typed [rhs] you'll get a runtime exception.
         */
        fun <A : Any> new(lhs: Expr<A>, rhs: Expr<*>, comp: ComparisonOp): Expr<Boolean> {
            val rhs = rhs.castOrThrow(lhs.ind)
            return StaticExpressionAnalysis.attemptToSimplifyComparison(lhs, rhs, comp)
        }
    }

    override fun parts(): List<Any> = listOf(lhs, rhs, comp)

    override val ind: Indicator<Boolean> = BooleanIndicator

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, comp)

    init {
        assert(lhs.ind == rhs.ind) {
            "Comparing expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

}

/**
 * Tries to cast the expression to the given set indicator.
 * Does nothing if the expression is already of the given type.
 *
 * Given that there's an assumption baked into all of this that we're working on a compilable program,
 * explicit isn't strictly necessary, but it's nice debugging and printing purposes.
 */
class TypeCastExpr<T : Any, Q : Any> private constructor(
    val expr: Expr<Q>,
    override val ind: Indicator<T>,
    val explicit: Boolean,
) : Expr<T>() {
    companion object {
        fun <T : Any, Q : Any> new(
            expr: Expr<Q>,
            targetInd: Indicator<T>,
            explicit: Boolean = false
        ): Expr<T> {
            if (expr.ind == targetInd) {
                return expr.castOrThrow(targetInd)
            }
            return ExprPool.create<TypeCastExpr<T, Q>>(expr, targetInd, explicit)
        }
    }

    override fun parts(): List<Any> = listOf(expr, ind, explicit)
}

class IfExpr<T : Any> private constructor(
    val trueExpr: Expr<T>,
    val falseExpr: Expr<T>,
    val thisCondition: Expr<Boolean>,
) : Expr<T>() {
    init {
        assert(trueExpr.ind == falseExpr.ind) {
            "Incompatible types in if statement: ${trueExpr.ind} and ${falseExpr.ind}"
        }
    }

    override fun parts(): List<Any> = listOf(trueExpr, falseExpr, thisCondition)

    override val ind: Indicator<T> get() = trueExpr.ind

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
            ExprPool.create(trueExpr, falseExpr, condition)

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

class UnionExpr<T : Any> private constructor(override val lhs: Expr<T>, override val rhs: Expr<T>) : Expr<T>(),
    AnyBinaryExpr<T> {
    init {
        assert(lhs.ind == rhs.ind) {
            "Unioning expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override fun parts(): List<Any> = listOf(lhs, rhs)

    companion object {
        fun <T : Any> new(lhs: Expr<T>, rhs: Expr<T>): UnionExpr<T> = ExprPool.create(lhs, rhs)
    }

    override val ind: Indicator<T> = lhs.ind
}

class BooleanExpr private constructor(
    override val lhs: Expr<Boolean>,
    override val rhs: Expr<Boolean>,
    val op: BooleanOp
) : Expr<Boolean>(), AnyBinaryExpr<Boolean> {
    init {
        assert(lhs.ind == rhs.ind) {
            "Boolean expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override fun parts(): List<Any> = listOf(lhs, rhs, op)

    override val ind: Indicator<Boolean> = BooleanIndicator

    companion object {
        fun newRaw(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): BooleanExpr =
            ExprPool.create(lhs, rhs, op)

        fun new(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
            return StaticExpressionAnalysis.attemptToSimplifyBooleanExpr(lhs, rhs, op)
        }
    }

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, op)
}

class BooleanInvertExpr private constructor(val expr: Expr<Boolean>) : Expr<Boolean>() {
    companion object {
        fun new(expr: Expr<Boolean>): BooleanInvertExpr = ExprPool.create(expr)
    }

    override fun parts(): List<Any> = listOf(expr)

    override val ind: Indicator<Boolean> = BooleanIndicator
}

class NegateExpr<T : Number> private constructor(val expr: Expr<T>) : Expr<T>() {
    companion object {
        fun <T : Number> new(expr: Expr<T>): NegateExpr<T> = ExprPool.create(expr)
    }

    override fun parts(): List<Any> = listOf(expr)

    override val ind: Indicator<T> = expr.ind
}

sealed class LeafExpr<T : Any> : Expr<T>() {
    abstract fun resolve(vars: Vars): Expr<T>
}

/**
 * This represents a variable in code which we do not know the value of yet within our scope. Variable expressions
 * are resolved at method processing time.
 *
 * Related to a specific context (The context that created it).
 * This context is only used for ensuring proper usage, it's never used within the logic.
 */
class VariableExpr<T : Any> private constructor(
    val key: UnknownKey,
    override val ind: Indicator<T>
) : LeafExpr<T>() {
    companion object {
        fun <T : Any> new(key: UnknownKey, ind: Indicator<T>): VariableExpr<T> = ExprPool.create(key, ind)
        fun new(key: UnknownKey): VariableExpr<*> = new(key, key.ind)
    }

    override fun parts(): List<Any> = listOf(key, ind)

    override fun resolve(vars: Vars): Expr<T> {
        return vars.get(vars.resolveKey(key)).castOrThrow(ind)
    }
}

/**
 * Objects are represented as a [ConstExpr] with an underlying [com.oberdiah.deepcomplexity.context.HeapMarker].
 */
class ConstExpr<T : Any> private constructor(val value: T, override val ind: Indicator<T>) : LeafExpr<T>() {
    override fun parts(): List<Any> = listOf(value, ind)

    override fun resolve(vars: Vars): Expr<T> = this

    val isHeapMarker: Boolean
        get() = value is HeapMarker

    val isPlaceholder: Boolean
        get() = isHeapMarker && (value as HeapMarker).isPlaceholder

    companion object {
        fun <T : Any> new(value: T, indicator: Indicator<T>): ConstExpr<T> =
            ExprPool.create(value, indicator)

        val TRUE = new(true, BooleanIndicator)
        val FALSE = new(false, BooleanIndicator)
        val VOID = fromHeapMarker(HeapMarker.new(PsiTypes.voidType()))

        @Suppress("unused")
        fun <T : Number> zero(ind: NumberIndicator<T>): ConstExpr<T> = new(ind.getZero(), ind)
        fun <T : Number> one(ind: NumberIndicator<T>): ConstExpr<T> = new(ind.getOne(), ind)
        fun <T : Any> fromAny(value: T): ConstExpr<T> = new(value, Indicator.fromValue(value))
        fun fromHeapMarker(marker: HeapMarker): ConstExpr<HeapMarker> = fromAny(marker)
        fun placeholderOf(ind: ObjectIndicator): ConstExpr<HeapMarker> = new(
            HeapMarker.newPlaceholder(ind.type),
            ind
        )
    }
}