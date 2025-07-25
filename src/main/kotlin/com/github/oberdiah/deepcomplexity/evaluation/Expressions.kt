package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.Context.Key
import com.github.oberdiah.deepcomplexity.evaluation.Context.Key.ExpressionKey
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiThisExpression

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
    val ind: SetIndicator<T>
        get() = SetIndicator.getSetIndicator(this)

    final override fun toString(): String {
        return ExprToString.toString(this)
    }

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, it calls the replacer on every expression so you can make any modifications
     * you want.
     */
    fun rebuildTree(replacer: ExprTreeRebuilder.Replacer) = ExprTreeRebuilder.rebuildTree(this, replacer)

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
        }
        )
    }

    fun <NewT : Any> replaceLeaves(replacer: ExprTreeRebuilder.LeafReplacer<NewT>): Expr<NewT> =
        ExprTreeRebuilder.replaceTreeLeaves(this, replacer)

    fun iterateTree(): Sequence<Expr<*>> = ExprTreeVisitor.iterateTree(this)
    fun getVariables(): Set<VariableExpression<*>> = iterateTree()
        .filterIsInstance<VariableExpression<*>>()
        .toSet()

    /**
     * Resolves all LValueExprs in the expression tree to their underlying expressions using the context.
     *
     * LValueExprs cannot end up in the final expression tree.
     */
    fun resolveLValues(context: Context): Expr<T> =
        // todo use replaceTypeInTree (super easy)
        this.rebuildTree(
            object : ExprTreeRebuilder.Replacer {
                override fun <T : Any> replace(expr: Expr<T>): Expr<T> {
                    return if (expr is LValueExpr) {
                        expr.resolve(context)
                    } else {
                        expr
                    }
                }
            }
        )

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

/**
 * Wrap the expression in a type cast to the given indicator.
 */
fun <T : Any> Expr<*>.castToUsingTypeCast(indicator: SetIndicator<T>, explicit: Boolean): Expr<T> {
    return if (this.ind == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as Expr<T>
    } else {
        TypeCastExpression(this, indicator, explicit)
    }
}

fun Expr<*>.getField(context: Context, key: Key.FieldKey): Expr<*> {
    assert(this !is VoidExpression) {
        "Cannot get field from a VoidExpression: $this"
    }

    fun <T : Any> extra(ind: SetIndicator<T>): Expr<T> {
        return this.replaceLeaves(ExprTreeRebuilder.LeafReplacer(ind) { expr ->
            val newExpr = if (expr is ClassExpression) {
                val heap = context.heap[expr.heapKey] ?: throw IllegalArgumentException(
                    "Heap for ${expr.heapKey} not found in context"
                )

                heap.variables[key] ?: throw IllegalArgumentException(
                    "Qualifier for $key not found in context"
                )
            } else {
                throw IllegalArgumentException(
                    "Expected ClassExpr, got ${expr::class.simpleName}"
                )
            }

            newExpr.tryCastTo(ind) ?: throw IllegalStateException(
                "(${newExpr.ind} != $ind) ${newExpr.dStr()} does not match ${expr.dStr()}"
            )
        })
    }

    return extra(key.ind)
}

/**
 * An expression that doesn't return anything.
 *
 * This is used in `if` statements, void methods, etc.
 */
class VoidExpression : Expr<VoidExpression>() {
    override fun hashCode(): Int = 0
    override fun equals(other: Any?) = other is VoidExpression
}

data class ArithmeticExpression<T : Number>(
    val lhs: Expr<T>,
    val rhs: Expr<T>,
    val op: BinaryNumberOp,
) : Expr<T>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Adding expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

data class ComparisonExpression<T : Number>(
    val lhs: Expr<T>,
    val rhs: Expr<T>,
    val comp: ComparisonOp,
) : Expr<Boolean>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Comparing expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we do not know the value of yet.
data class VariableExpression<T : Any>(val key: Key) : Expr<T>()

/**
 * Tries to cast the expression to the given set indicator.
 * Does nothing if the expression is already of the given type.
 *
 * Given that there's an assumption baked into all of this that we're working on a compilable program,
 * explicit isn't strictly necessary, but it's nice debugging and printing purposes.
 */
data class TypeCastExpression<T : Any, Q : Any>(
    val expr: Expr<Q>,
    val setInd: SetIndicator<T>,
    val explicit: Boolean,
) : Expr<T>() {
    init {
        assert(expr !is VoidExpression)
    }
}

data class IfExpression<T : Any>(
    val trueExpr: Expr<T>,
    val falseExpr: Expr<T>,
    val thisCondition: Expr<Boolean>,
) : Expr<T>() {
    init {
        assert(trueExpr.ind == falseExpr.ind) {
            "Incompatible types in if statement: ${trueExpr.ind} and ${falseExpr.ind}"
        }
    }

    companion object {
        fun <A : Any, B : Any> new(
            a: Expr<A>,
            b: Expr<B>,
            condition: Expr<Boolean>
        ): Expr<A> {
            val castB = b.tryCastTo(a.ind)
                ?: throw IllegalStateException("Incompatible types in if statement: ${a.ind} and ${b.ind}")

            return IfExpression(a, castB, condition)
        }
    }
}

data class UnionExpression<T : Any>(val lhs: Expr<T>, val rhs: Expr<T>) : Expr<T>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Unioning expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

data class BooleanExpression(val lhs: Expr<Boolean>, val rhs: Expr<Boolean>, val op: BooleanOp) : Expr<Boolean>() {
    init {
        assert(lhs.ind == rhs.ind) {
            "Boolean expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

data class ConstExpr<T : Any>(val constSet: Bundle<T>) : Expr<T>() {
    companion object {
        fun <T : Any> new(bundle: Variances<T>): ConstExpr<T> = ConstExpr(Bundle.unconstrained(bundle))
    }
}

/**
 * Represents an object.
 */
data class ClassExpression(val psi: PsiNewExpression, val heapKey: Key.HeapKey) : Expr<Any>()

data class ThisExpression(val psi: PsiThisExpression) : Expr<Any>()

/**
 * Represents any expression that can be used as a left-hand value in an assignment.
 *
 * For example, `x` in `x = 5`, or `this.x` in `this.x = 5`.
 *
 * The `qualifier` is optional.
 */
data class LValueExpr<T : Any>(
    val key: Key,
    val qualifier: Expr<*>?,
    val myInd: SetIndicator<T>
) : Expr<T>() {
    init {
        assert(qualifier == null || key is Key.FieldKey) {
            "Qualifier can only be set for field keys, got: $key"
        }
    }

    /**
     * Resolves the expression in the given context, converting it from an LValueExpr to whatever underlying
     * expr it represents.
     */
    fun resolve(context: Context): Expr<T> {
        val resolved = qualifier?.getField(context, key as Key.FieldKey) ?: context.getVar(key)
        return resolved.tryCastTo(myInd)!!
    }
}

data class BooleanInvertExpression(val expr: Expr<Boolean>) : Expr<Boolean>()
data class NegateExpression<T : Number>(val expr: Expr<T>) : Expr<T>()

data class NumIterationTimesExpression<T : Number>(
    // How the variable is constrained; if the variable changes such that this returns false,
    // the loop will end.
    val constraint: NumberSet<T>,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpression<T>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms<T>,
) : Expr<T>() {
    companion object {
        fun <T : Number> new(
            constraint: NumberSet<T>,
            variable: VariableExpression<out Number>,
            terms: ConstraintSolver.CollectedTerms<out Number>
        ): NumIterationTimesExpression<T> {
            val setIndicator = constraint.ind

            assert(setIndicator == variable.ind) {
                "Variable and constraint have different set indicators: ${variable.ind} and $setIndicator"
            }
            assert(setIndicator == terms.ind) {
                "Variable and terms have different set indicators: ${variable.ind} and ${terms.ind}"
            }

            @Suppress("UNCHECKED_CAST")
            return NumIterationTimesExpression(
                constraint,
                variable as VariableExpression<T>,
                terms as ConstraintSolver.CollectedTerms<T>
            )
        }
    }
}