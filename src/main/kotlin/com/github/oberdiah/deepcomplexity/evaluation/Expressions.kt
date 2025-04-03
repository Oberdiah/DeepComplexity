package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.TypedNumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.ConstrainedSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

sealed interface IExpr<T : ConstrainedSet<T>> {
    /**
     * The indicator represents what the expression will be once evaluated.
     */
    fun getSetIndicator(): SetIndicator<T> = SetIndicator.getSetIndicator(this)
    fun getVariables(resolved: Boolean): Set<VariableExpression<*>> = ExprGetVariables.getVariables(this, resolved)
    fun evaluate(condition: IExpr<BooleanSet>): T = ExprEvaluate.evaluate(this, condition)
    fun dStr(): String = ExprToString.toDebugString(this)
}

sealed class Expr<T : ConstrainedSet<T>> : IExpr<T> {
    override fun toString(): String {
        return ExprToString.toString(this)
    }
}

fun <T : TypedNumberSet<*, T>> IExpr<T>.getNumberSetIndicator() = getSetIndicator() as NumberSetIndicator<*, T>

fun IExpr<*>.tryCastToNumbers(): IExpr<out NumberSet<*>>? {
    if (this.getSetIndicator() is NumberSetIndicator<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return this as IExpr<out NumberSet<*>>
    } else {
        return null
    }
}

inline fun <Set : ConstrainedSet<Set>, reified T : IExpr<Set>> IExpr<*>.tryCastExact(indicator: SetIndicator<Set>): T? {
    return if (this::class == T::class && indicator == this.getSetIndicator()) {
        @Suppress("UNCHECKED_CAST")
        this as T
    } else {
        null
    }
}

fun <T : ConstrainedSet<T>> IExpr<*>.performACastTo(indicator: SetIndicator<T>, explicit: Boolean): IExpr<T> {
    return if (this.getSetIndicator() == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as IExpr<T>
    } else {
        TypeCastExpression(this, indicator, explicit)
    }
}

fun <T : ConstrainedSet<T>> IExpr<*>.tryCastTo(indicator: SetIndicator<T>): IExpr<T>? {
    return if (this.getSetIndicator() == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as IExpr<T>
    } else {
        null
    }
}

fun IExpr<BooleanSet>.getConstraints(): List<Constraints> =
    ExprConstrain.getConstraints(this)

class ArithmeticExpression<T : NumberSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>, val op: BinaryNumberOp) : Expr<T>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Adding expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class ComparisonExpression<T : NumberSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>, val comp: ComparisonOp) :
    Expr<BooleanSet>() {
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
class TypeCastExpression<T : ConstrainedSet<T>, Q : ConstrainedSet<Q>>(
    val expr: IExpr<Q>,
    val setInd: SetIndicator<T>,
    val explicit: Boolean
) : Expr<T>()

class IfExpression<T : ConstrainedSet<T>>(
    val trueExpr: IExpr<T>,
    val falseExpr: IExpr<T>,
    val thisCondition: IExpr<BooleanSet>
) : Expr<T>() {
    init {
        assert(trueExpr.getSetIndicator() == falseExpr.getSetIndicator()) {
            "Incompatible types in if statement: ${trueExpr.getSetIndicator()} and ${falseExpr.getSetIndicator()}"
        }
    }

    companion object {
        fun <A : ConstrainedSet<A>, B : ConstrainedSet<B>> new(
            a: IExpr<A>,
            b: IExpr<B>,
            condition: IExpr<BooleanSet>
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

class IntersectExpression<T : ConstrainedSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Intersecting expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class UnionExpression<T : ConstrainedSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Unioning expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class BooleanExpression(val lhs: IExpr<BooleanSet>, val rhs: IExpr<BooleanSet>, val op: BooleanOp) :
    Expr<BooleanSet>() {
    init {
        assert(lhs.getSetIndicator() == rhs.getSetIndicator()) {
            "Boolean expressions with different set indicators: ${lhs.getSetIndicator()} and ${rhs.getSetIndicator()}"
        }
    }
}

class ConstExpr<T : ConstrainedSet<T>>(val singleElementSet: T) : Expr<T>()
class BooleanInvertExpression(val expr: IExpr<BooleanSet>) : Expr<BooleanSet>()
class InvertExpression<T : ConstrainedSet<T>>(val expr: IExpr<T>) : Expr<T>()
class NegateExpression<T : NumberSet<T>>(val expr: IExpr<T>) : Expr<T>()

/**
 * Returns the range of numbers above or below a given limit, depending on cmp.
 */
class NumberLimitsExpression<T : NumberSet<T>>(
    // The value we're either going to be above or below.
    val limit: IExpr<T>,
    // Whether we should flip the comparison operator or not.
    val shouldFlipCmp: IExpr<BooleanSet>,
    // The comparison operator to use.
    val cmp: ComparisonOp
) : Expr<T>()

class NumIterationTimesExpression<T : NumberSet<T>>(
    // How the variable is constrained; if the variable changes such that this returns false,
    // the loop will end.
    val constraint: T,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpression<T>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms<T>
) : Expr<T>() {
    companion object {
        fun <T : NumberSet<T>> new(
            constraint: T,
            variable: VariableExpression<out NumberSet<*>>,
            terms: ConstraintSolver.CollectedTerms<out NumberSet<*>>
        ): NumIterationTimesExpression<T> {
            val setIndicator = constraint.getSetIndicator()
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