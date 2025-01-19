package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

sealed interface IExpr<T : IMoldableSet<T>> {
    fun getSetIndicator(): SetIndicator<T> = ExprClass.getSetIndicator(this)
    fun getVariables(resolved: Boolean): Set<VariableExpression<*>> = ExprGetVariables.getVariables(this, resolved)
    fun evaluate(condition: IExpr<BooleanSet>): T = ExprEvaluate.evaluate(this, condition)
}

sealed class Expr<T : IMoldableSet<T>> : IExpr<T> {
    override fun toString(): String {
        return ExprToString.toString(this)
    }
}

fun <Q : NumberSet<Q>, T : IMoldableSet<T>> mapNumExprToSet(expr: IExpr<T>, numExpr: (IExpr<Q>) -> Q): T? {
    if (expr.getSetIndicator() is NumberSetIndicator<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return numExpr(expr as IExpr<Q>) as T
    } else {
        return null
    }
}

fun <T : IMoldableSet<T>> IExpr<*>.tryCastTo(indicator: SetIndicator<T>): IExpr<T>? {
    return if (this.getSetIndicator() == indicator) {
        @Suppress("UNCHECKED_CAST")
        this as IExpr<T>
    } else {
        null
    }
}

fun <T : IMoldableSet<T>> IExpr<BooleanSet>.getConstraints(varKey: VariableExpression<T>): IExpr<T>? =
    ExprConstrain.getConstraints(this, varKey)

class ArithmeticExpression<T : NumberSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>, val op: BinaryNumberOp) : Expr<T>()
class ComparisonExpression<T : NumberSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>, val comp: ComparisonOp) :
    Expr<BooleanSet>()

class IfExpression<T : IMoldableSet<T>>(
    val trueExpr: IExpr<T>,
    val falseExpr: IExpr<T>,
    val thisCondition: IExpr<BooleanSet>
) : Expr<T>() {
    companion object {
        fun <A : IMoldableSet<A>, B : IMoldableSet<B>> new(
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

class IntersectExpression<T : IMoldableSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>()
class BooleanInvertExpression(val expr: IExpr<BooleanSet>) : Expr<BooleanSet>()
class InvertExpression<T : IMoldableSet<T>>(val expr: IExpr<T>) : Expr<T>()
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
    val constraint: IExpr<T>,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpression<T>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms<T>
) : Expr<T>()

class UnionExpression<T : IMoldableSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>()
class BooleanExpression(val lhs: IExpr<BooleanSet>, val rhs: IExpr<BooleanSet>, val op: BooleanOp) : Expr<BooleanSet>()

class ConstExpr<T : IMoldableSet<T>>(val singleElementSet: T) : Expr<T>()