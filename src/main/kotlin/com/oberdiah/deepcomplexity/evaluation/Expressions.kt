package com.oberdiah.deepcomplexity.evaluation

import ai.grazie.utils.merge
import com.oberdiah.deepcomplexity.context.*
import com.oberdiah.deepcomplexity.context.EvaluationKey.ExpressionKey
import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder.rewriteInTree
import com.oberdiah.deepcomplexity.evaluation.ExprTreeRebuilder.rewriteInTreeSameType
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.IfExpr.Companion.new
import com.oberdiah.deepcomplexity.evaluation.simplification.BooleanSimplification
import com.oberdiah.deepcomplexity.evaluation.simplification.ComparisonSimplification
import com.oberdiah.deepcomplexity.evaluation.simplification.IfSimplification
import com.oberdiah.deepcomplexity.staticAnalysis.*
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ConstraintsOrPile
import com.oberdiah.deepcomplexity.utilities.Utilities.sum
import java.math.BigInteger

const val SKIP_OPTIMIZATIONS = false

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
     * If you want something as a key, use this object directly.
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
     *
     * Note: This is also used by default for tree traversal. If you return anything here that
     * isn't an expression but contain expressions, be sure to add your class to [subExprs]
     * to ensure your sub expressions are properly represented. This also applies
     * if you have any requirements around [TreeTraversal] behaviour.
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

    val subExprCounts: Map<Expr<*>, BigInteger> by lazy {
        directSubExprs.map {
            it.subExprCounts
        }.fold(mapOf(this to BigInteger.ONE)) { mapA, mapB ->
            mapA.merge(mapB) { _, a, b -> a + b }
        }
    }

    /**
     * All sub-expressions in this tree, including this one.
     */
    val recursiveSubExprs: Set<Expr<*>> by lazy {
        directSubExprs.flatMap { it.recursiveSubExprs }.toSet() + this
    }

    /**
     * The size of this expression tree, defined as the number of expression nodes it contains.
     * Can be really, really big. 10^50+ is not out of the question.
     */
    val size: BigInteger by lazy { subExprCounts.values.sum() }

    val directSubExprs by lazy { subExprs(TreeTraversal.All) }

    fun subExprs(treeTraversal: TreeTraversal): List<Expr<*>> {
        return when (this) {
            is IfExpr -> when (treeTraversal) {
                TreeTraversal.All -> listOf(trueExpr, falseExpr, thisCondition)
                TreeTraversal.PrimaryPathOnly -> listOf(trueExpr, falseExpr)
                TreeTraversal.AuxPathsOnly -> listOf(thisCondition)
            }

            is LoopExpr -> {
                // This isn't complete, eventually we'll have to deal with the primary paths and aux paths
                // todo loops
                listOf(condition) + variables.values.flatMap { listOf(it.initial, it.update) }
            }

            else -> parts().filterIsInstance<Expr<*>>()
        }
    }

    fun allPrimaryPathLeaves(): Set<LeafExpr<*>> = collectToSet(TreeTraversal.PrimaryPathOnly) { it as? LeafExpr<*> }

    internal inline fun <reified Q : Expr<*>> allSubExprsOfType(
        treeTraversal: TreeTraversal = TreeTraversal.All
    ): Set<Q> = collectToSet(treeTraversal) { it as? Q }

    fun <O> collectToSet(
        treeTraversal: TreeTraversal = TreeTraversal.All,
        getItem: (Expr<*>) -> O?
    ): Set<O> {
        return ExprTreeVisitor.reduce(
            treeTraversal,
            this,
            { expr ->
                val item = getItem(expr)
                if (item != null) {
                    setOf(item)
                } else {
                    emptySet()
                }
            },
            { setA, setB -> setA + setB }
        )
    }

    fun resolveUnknowns(mCtx: Context): Expr<T> =
        mCtx.resolveKnownVariables(this)

    /**
     * Variant of [rewriteTypeInTree] that enforces indicator preservation.
     *
     * For every expression of type [Q], [replacer] must return an expression with the same indicator as
     * its input.
     *
     * All traversal and caching behaviour is identical to [rewriteInTree].
     */
    inline fun <reified Q : Expr<*>> rewriteTypeInTreeSameType(
        treeTraversal: TreeTraversal = TreeTraversal.All,
        crossinline replacer: (Q) -> Expr<*>
    ): Expr<T> = this.rewriteTypeInTree<Q>(treeTraversal) { e -> replacer(e).castOrThrow(e.ind) }
        .castOrThrow(this.ind)

    /**
     * Variant of [rewriteInTree] that only applies [replacer] to expressions of type [Q].
     *
     * Expressions that are not instances of [Q] are left unchanged. Replacements are allowed to change
     * indicators.
     *
     * All traversal and caching behaviour is identical to [rewriteInTree].
     */
    inline fun <reified Q : Expr<*>> rewriteTypeInTree(
        treeTraversal: TreeTraversal = TreeTraversal.All,
        crossinline replacer: (Q) -> Expr<*>
    ): Expr<*> {
        return this.rewriteInTree(treeTraversal) { expr: Expr<*> ->
            if (expr is Q) {
                replacer(expr)
            } else {
                expr
            }
        }
    }

    /**
     * Recursively calls [simplify] on every node in the tree, rebuilding the tree as it goes.
     */
    fun optimise(): Expr<T> = this.rewriteInTreeSameType { it.simplify() }

    /**
     * Simplifies the expression if possible.
     * Does not operate recursively.
     */
    open fun simplify(): Expr<T> = this

    fun evaluate(constraints: ConstraintsOrPile, assistant: EvaluatorAssistant): Bundle<T> =
        ExprEvaluate.evaluate(this, constraints, assistant)
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
        fun new(vars: DynamicOrStatic = DynamicOrStatic.Dynamic): VarsExpr = ExprPool.create { VarsExpr(vars) }
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
    val op: BinaryNumberOp
) : Expr<T>(), AnyBinaryExpr<T> {
    companion object {
        fun <T : Number> new(lhs: Expr<T>, rhs: Expr<T>, op: BinaryNumberOp): ArithmeticExpr<T> =
            ExprPool.create { ArithmeticExpr(lhs, rhs, op) }
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
            ExprPool.create { ComparisonExpr(lhs, rhs, comp) }

        /**
         * Compile-time casts [rhs] for you so you don't have to worry about it. If you provide
         * a wrongly typed [rhs] you'll get a runtime exception.
         */
        fun <A : Any> new(lhs: Expr<A>, rhs: Expr<*>, comp: ComparisonOp): Expr<Boolean> {
            val rhs = rhs.castOrThrow(lhs.ind)
            return ComparisonSimplification.attemptToSimplifyComparison(lhs, rhs, comp)
        }
    }

    override fun parts(): List<Any> = listOf(lhs, rhs, comp)

    override val ind: Indicator<Boolean> = BooleanIndicator

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, comp)

    init {
        require(lhs.ind == rhs.ind) {
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
            return ExprPool.create { TypeCastExpr(expr, targetInd, explicit) }
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
        require(trueExpr.ind == falseExpr.ind) {
            "Incompatible types in if statement: ${trueExpr.ind} and ${falseExpr.ind}"
        }
    }

    override fun parts(): List<Any> = listOf(trueExpr, falseExpr, thisCondition)

    override val ind: Indicator<T> get() = trueExpr.ind

    override fun simplify(): Expr<T> = new(trueExpr, falseExpr, thisCondition)

    companion object {
        /**
         * Like [new], but doesn't perform on-the-fly optimizations. Should only really be used during tree traversal.
         *
         * A tree replacement traversal relies on the only changes coming from the replacer itself -
         * if we did our own optimizations, we could end up in a situation where a replacer sees the same
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
         * the original leaf node, and then again once the inner if was optimized away. This would obviously
         * be incorrect.
         */
        fun <T : Any> newRaw(trueExpr: Expr<T>, falseExpr: Expr<T>, condition: Expr<Boolean>): IfExpr<T> =
            ExprPool.create { IfExpr(trueExpr, falseExpr, condition) }

        /**
         * Compile-time casts [falseExpr] for you so you don't have to worry about it. If you provide
         * a wrongly typed [falseExpr] you'll get a runtime exception.
         */
        fun <A : Any> new(trueExpr: Expr<A>, falseExpr: Expr<*>, condition: Expr<Boolean>): Expr<A> {
            val falseExpr = falseExpr.castOrThrow(trueExpr.ind)
            return IfSimplification.attemptToSimplifyIfExpr(trueExpr, falseExpr, condition)
        }
    }
}

class BooleanOpExpr private constructor(
    override val lhs: Expr<Boolean>,
    override val rhs: Expr<Boolean>,
    val op: BooleanOp
) : Expr<Boolean>(), AnyBinaryExpr<Boolean> {
    init {
        require(lhs.ind == rhs.ind) {
            "Boolean expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }

    override fun parts(): List<Any> = listOf(lhs, rhs, op)

    override val ind: Indicator<Boolean> = BooleanIndicator

    companion object {
        fun newRaw(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): BooleanOpExpr =
            ExprPool.create { BooleanOpExpr(lhs, rhs, op) }

        fun new(lhs: Expr<Boolean>, rhs: Expr<Boolean>, op: BooleanOp): Expr<Boolean> {
            return BooleanSimplification.attemptToSimplifyBooleanExpr(lhs, rhs, op)
        }
    }

    override fun simplify(): Expr<Boolean> = new(lhs, rhs, op)
}

class BooleanInvertExpr private constructor(val expr: Expr<Boolean>) : Expr<Boolean>() {
    companion object {
        fun new(expr: Expr<Boolean>): BooleanInvertExpr = ExprPool.create { BooleanInvertExpr(expr) }
    }

    override fun parts(): List<Any> = listOf(expr)

    override val ind: Indicator<Boolean> = BooleanIndicator
}

class NegateExpr<T : Number> private constructor(val expr: Expr<T>) : Expr<T>() {
    companion object {
        fun <T : Number> new(expr: Expr<T>): NegateExpr<T> = ExprPool.create { NegateExpr(expr) }
    }

    override fun parts(): List<Any> = listOf(expr)

    override val ind: Indicator<T> = expr.ind
}

/**
 * Here's how loops work in more complicated multiple-object scenarios
 * ```java
 * Foo foo = new Foo(5);
 * Foo bar = new Foo(6);
 * Foo baz = new Foo(5);
 * for (int i = 0; i < 10; i++) {
 *    if (foo == baz) {
 *      foo = bar;
 *    }
 * }
 * return foo.x;
 * ```
 * Now, while we're building the loop, everything is A-OK. We end up with a structure like this:
 * ```
 * LoopExpr(
 *   target = $foo,
 *   variables = {
 *     $i: LoopVar(initial = 0, update = $i + 1)
 *     $foo: LoopVar(initial = foo, update = if ($foo == baz) bar else $foo)
 *   }
 * )
 * ```
 * Now, what happens when we try to perform `.x` on this loop? Things get a little bit funky.
 * I believe it still works; however, we do need to be very careful in maintaining that old '$foo'
 * when we perform the replacement. I'm not quite sure how we detect that.
 * ```
 * LoopExpr(
 *   target = $foo.x,
 *   variables = {
 *     $i: LoopVar(initial = 0, update = $i + 1)
 *     $foo: LoopVar(initial = foo.x, update = if ($foo == baz) bar else $foo)
 *     $foo.x: LoopVar(initial = foo.x, update = if ($foo == baz) bar.x else $foo.x)
 *   }
 * )
 * ```
 */
class LoopExpr<T : Any> private constructor(
    val target: LoopKey<T>,
    val condition: Expr<Boolean>,
    val variables: Map<LoopKey<*>, LoopVar<*>>,
) : Expr<T>() {
    companion object {
        fun <T : Any> new(
            target: LoopKey<T>,
            condition: Expr<Boolean>,
            variables: Map<LoopKey<*>, LoopVar<*>>
        ): LoopExpr<T> = ExprPool.create { LoopExpr(target, condition, variables) }
    }

    override val ind: Indicator<T>
        get() = target.ind

    override fun parts(): List<Any> = listOf(target, condition, variables)

    data class LoopVar<T : Any>(val initial: Expr<T>, val update: Expr<T>)

    @ConsistentCopyVisibility
    data class LoopKey<T : Any> private constructor(val key: MethodProcessingKey, val idx: Int, val ind: Indicator<T>) {
        companion object {
            private var IDX_COUNT = 0
            fun new(key: MethodProcessingKey): LoopKey<*> = LoopKey(key, IDX_COUNT++, key.ind)
        }
    }

    class LoopLeaf<T : Any> private constructor(
        val key: LoopKey<T>,
    ) : Expr<T>() {
        companion object {
            fun new(key: MethodProcessingKey): LoopLeaf<*> = new(LoopKey.new(key))
            fun <T : Any> new(key: LoopKey<T>): LoopLeaf<T> = ExprPool.create { LoopLeaf(key) }
        }

        override fun parts(): List<Any> = listOf(key)
        override val ind: Indicator<T> get() = key.ind
    }
}

sealed class LeafExpr<T : Any> : Expr<T>() {
    abstract fun resolve(vars: Vars): Expr<T>
}

/**
 * This represents an unknown in code which we do not know the value of yet within our scope. Variable expressions
 * are resolved at method processing time.
 */
class VariableExpr<T : Any> private constructor(
    val key: MethodProcessingKey,
    override val ind: Indicator<T>
) : LeafExpr<T>() {
    companion object {
        fun <T : Any> new(key: MethodProcessingKey, ind: Indicator<T>): VariableExpr<T> =
            ExprPool.create { VariableExpr(key, ind) }

        fun new(key: MethodProcessingKey): VariableExpr<*> = new(key, key.ind)
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
            ExprPool.create { ConstExpr(value, indicator) }

        val TRUE = new(true, BooleanIndicator)
        val FALSE = new(false, BooleanIndicator)
        val VOID = fromHeapMarker(HeapMarker.new(MyPsiType.VOID_TYPE))

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