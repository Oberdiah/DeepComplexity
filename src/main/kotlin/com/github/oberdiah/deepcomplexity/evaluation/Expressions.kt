package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.Context.Key
import com.github.oberdiah.deepcomplexity.evaluation.Context.Key.ExpressionKey
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.ExprConstrain
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

sealed class Expr<T : Any>(private val k: ExpressionKey? = null) {
    /**
     * This is used as a key in the caching system. If two expressions have the same key,
     * they are considered equal.
     */
    val exprKey: ExpressionKey by lazy {
        k ?: ExpressionKey(this)
    }

    /**
     * The indicator represents what the expression will be once evaluated.
     */
    val ind: SetIndicator<T>
        get() = SetIndicator.getSetIndicator(this)

    override fun toString(): String {
        return ExprToString.toString(this)
    }

    /**
     * Rebuilds every expression in the tree.
     * As it's doing that, it calls the replacer on every expression so you can make any modifications
     * you want.
     */
    fun rebuildTree(replacer: ExprTreeRebuilder.Replacer) = ExprTreeRebuilder.rebuildTree(this, replacer)
    fun iterateTree(): Sequence<Expr<*>> = ExprTreeVisitor.iterateTree(this)
    fun getVariables(): Set<VariableExpression<*>> = iterateTree()
        .filterIsInstance<VariableExpression<*>>()
        .toSet()

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

inline fun <Set : Any, reified T : Expr<Set>> Expr<*>.tryCastExact(indicator: SetIndicator<Set>): T? {
    return if (this::class == T::class && indicator == this.ind) {
        @Suppress("UNCHECKED_CAST")
        this as T
    } else {
        null
    }
}

fun <T : Any> Expr<*>.performACastTo(indicator: SetIndicator<T>, explicit: Boolean): Expr<T> {
    return if (this.ind == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as Expr<T>
    } else {
        TypeCastExpression(this, indicator, explicit)
    }
}

fun <T : Any> Expr<*>.tryCastTo(indicator: SetIndicator<T>): Expr<T>? {
    return if (this.ind == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as Expr<T>
    } else {
        null
    }
}

fun Expr<Boolean>.getConstraints(): List<Constraints> =
    ExprConstrain.getConstraints(this)

/**
 * An expression that doesn't return anything.
 *
 * This is used in `if` statements, void methods, etc.
 */
class VoidExpression : Expr<VoidExpression>()

class ArithmeticExpression<T : Number>(
    val lhs: Expr<T>,
    val rhs: Expr<T>,
    val op: BinaryNumberOp,
    k: ExpressionKey? = null
) :
    Expr<T>(k) {
    init {
        assert(lhs.ind == rhs.ind) {
            "Adding expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

class ComparisonExpression<T : Number>(
    val lhs: Expr<T>,
    val rhs: Expr<T>,
    val comp: ComparisonOp,
    k: ExpressionKey? = null
) :
    Expr<Boolean>(k) {
    init {
        assert(lhs.ind == rhs.ind) {
            "Comparing expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we do not know the value of yet.
class VariableExpression<T : Any>(val key: Key, k: ExpressionKey? = null) : Expr<T>(k)

/**
 * Tries to cast the expression to the given set indicator.
 * Does nothing if the expression is already of the given type.
 *
 * Given that there's an assumption baked into all of this that we're working on a compilable program,
 * explicit isn't strictly necessary, but it's nice debugging and printing purposes.
 */
class TypeCastExpression<T : Any, Q : Any>(
    val expr: Expr<Q>,
    val setInd: SetIndicator<T>,
    val explicit: Boolean,
    k: ExpressionKey? = null
) : Expr<T>(k)

class IfExpression<T : Any>(
    val trueExpr: Expr<T>,
    val falseExpr: Expr<T>,
    val thisCondition: Expr<Boolean>,
    k: ExpressionKey? = null
) : Expr<T>(k) {
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
            return if (a.ind == b.ind) {
                @Suppress("UNCHECKED_CAST")
                IfExpression(a, b as Expr<A>, condition)
            } else {
                throw IllegalStateException("Incompatible types in if statement: ${a.ind} and ${b.ind}")
            }
        }
    }
}

class UnionExpression<T : Any>(val lhs: Expr<T>, val rhs: Expr<T>, k: ExpressionKey? = null) : Expr<T>(k) {
    init {
        assert(lhs.ind == rhs.ind) {
            "Unioning expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

class BooleanExpression(val lhs: Expr<Boolean>, val rhs: Expr<Boolean>, val op: BooleanOp, k: ExpressionKey? = null) :
    Expr<Boolean>(k) {
    init {
        assert(lhs.ind == rhs.ind) {
            "Boolean expressions with different set indicators: ${lhs.ind} and ${rhs.ind}"
        }
    }
}

class ConstExpr<T : Any>(val constSet: Bundle<T>, key: ExpressionKey? = null) : Expr<T>(key) {
    companion object {
        fun <T : Any> new(bundle: Variances<T>): ConstExpr<T> = ConstExpr(Bundle.unconstrained(bundle))
    }
}

class BooleanInvertExpression(val expr: Expr<Boolean>, k: ExpressionKey? = null) : Expr<Boolean>(k)
class NegateExpression<T : Number>(val expr: Expr<T>, k: ExpressionKey? = null) : Expr<T>(k)

class NumIterationTimesExpression<T : Number>(
    // How the variable is constrained; if the variable changes such that this returns false,
    // the loop will end.
    val constraint: NumberSet<T>,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpression<T>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms<T>,
    k: ExpressionKey? = null
) : Expr<T>(k) {
    companion object {
        fun <T : Number> new(
            constraint: NumberSet<T>,
            variable: VariableExpression<out Number>,
            terms: ConstraintSolver.CollectedTerms<out Number>
        ): NumIterationTimesExpression<T> {
            val setIndicator = SetIndicator.fromValue(constraint)
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