package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.BundleSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

sealed interface IExpr<T : Any> {
    /**
     * The indicator represents what the expression will be once evaluated.
     */
    fun getSetIndicator(): SetIndicator<T> = SetIndicator.getSetIndicator(this)
    fun getVariables(resolved: Boolean): Set<VariableExpression<*>> = ExprGetVariables.getVariables(this, resolved)
    fun evaluate(condition: IExpr<Boolean>): BundleSet<T> = ExprEvaluate.evaluate(this, condition)
    fun dStr(): String = ExprToString.toDebugString(this)
}

sealed class Expr<T : Any> : IExpr<T> {
    override fun toString(): String {
        return ExprToString.toString(this)
    }
}

fun <T : Number> IExpr<T>.getNumberSetIndicator() = getSetIndicator() as NumberSetIndicator<T>

fun IExpr<*>.tryCastToNumbers(): IExpr<out Number>? {
    if (this.getSetIndicator() is NumberSetIndicator<*>) {
        @Suppress("UNCHECKED_CAST")
        return this as IExpr<out Number>
    } else {
        return null
    }
}

inline fun <Set : Any, reified T : IExpr<Set>> IExpr<*>.tryCastExact(indicator: SetIndicator<Set>): T? {
    return if (this::class == T::class && indicator == this.getSetIndicator()) {
        @Suppress("UNCHECKED_CAST")
        this as T
    } else {
        null
    }
}

fun <T : Any> IExpr<*>.performACastTo(indicator: SetIndicator<T>, explicit: Boolean): IExpr<T> {
    return if (this.getSetIndicator() == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as IExpr<T>
    } else {
        TypeCastExpression(this, indicator, explicit)
    }
}

fun <T : Any> IExpr<*>.tryCastTo(indicator: SetIndicator<T>): IExpr<T>? {
    return if (this.getSetIndicator() == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as IExpr<T>
    } else {
        null
    }
}

fun IExpr<Boolean>.getConstraints(): List<Constraints> =
    ExprConstrain.getConstraints(this)

class ArithmeticExpression<T : Number>(val lhs: IExpr<T>, val rhs: IExpr<T>, val op: BinaryNumberOp) : Expr<T>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Adding expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class ComparisonExpression<T : Number>(val lhs: IExpr<T>, val rhs: IExpr<T>, val comp: ComparisonOp) :
    Expr<Boolean>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Comparing expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
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
class TypeCastExpression<T : Any, Q : Any>(
    val expr: IExpr<Q>,
    val setInd: SetIndicator<T>,
    val explicit: Boolean
) : Expr<T>()

class IfExpression<T : Any>(
    val trueExpr: IExpr<T>,
    val falseExpr: IExpr<T>,
    val thisCondition: IExpr<Boolean>
) : Expr<T>() {
    init {
        assert(trueExpr.getSetIndicator() == falseExpr.getSetIndicator()) {
            "Incompatible types in if statement: ${trueExpr.getSetIndicator()} and ${falseExpr.getSetIndicator()}"
        }
    }

    companion object {
        fun <A : Any, B : Any> new(
            a: IExpr<A>,
            b: IExpr<B>,
            condition: IExpr<Boolean>
        ): IExpr<A> {
            return if (a.getSetIndicator() == b.getSetIndicator()) {
                @Suppress("UNCHECKED_CAST")
                IfExpression(a, b as IExpr<A>, condition)
            } else {
                throw IllegalStateException("Incompatible types in if statement: ${a.getSetIndicator()} and ${b.getSetIndicator()}")
            }
        }
    }
}

class UnionExpression<T : Any>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Unioning expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class BooleanExpression(val lhs: IExpr<Boolean>, val rhs: IExpr<Boolean>, val op: BooleanOp) :
    Expr<Boolean>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Boolean expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class ConstExpr<T : Any>(val constSet: BundleSet<T>) : Expr<T>() {
    companion object {
        fun <T : Any> new(bundle: Variances<T>): ConstExpr<T> = ConstExpr(BundleSet.unconstrainedBundle(bundle))
    }
}

class BooleanInvertExpression(val expr: IExpr<Boolean>) : Expr<Boolean>()
class NegateExpression<T : Number>(val expr: IExpr<T>) : Expr<T>()

class NumIterationTimesExpression<T : Number>(
    // How the variable is constrained; if the variable changes such that this returns false,
    // the loop will end.
    val constraint: NumberSet<T>,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpression<T>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms<T>
) : Expr<T>() {
    companion object {
        fun <T : Number> new(
            constraint: NumberSet<T>,
            variable: VariableExpression<out Number>,
            terms: ConstraintSolver.CollectedTerms<out Number>
        ): NumIterationTimesExpression<T> {
            val setIndicator = SetIndicator.fromValue(constraint)
            assert(setIndicator == variable.getSetIndicator()) {
                "Variable and constraint have different set indicators: ${variable.getSetIndicator()} and $setIndicator"
            }
            assert(setIndicator == terms.setIndicator) {
                "Variable and terms have different set indicators: ${variable.getSetIndicator()} and ${terms.setIndicator}"
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