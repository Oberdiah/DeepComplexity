package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

sealed class SetClass<T : IMoldableSet<T>>(val clazz: KClass<T>)
data object NumberSetClass : SetClass<NumberSet>(NumberSet::class)
data object BooleanSetClass : SetClass<BooleanSet>(BooleanSet::class)
data object GenericSetClass : SetClass<GenericSet>(GenericSet::class)

sealed interface IExpr<T : IMoldableSet<T>> {
    fun getVariables(resolved: Boolean): Set<VariableExpression<*>> = ExprGetVariables.getVariables(this, resolved)
    fun getBaseClass(): KClass<*> = ExprClass.getBaseClass(this)
    fun getSetClass(): SetClass<T> = ExprClass.getSetClass(this)
    fun evaluate(condition: IExpr<BooleanSet>): T = ExprEvaluate.evaluate(this, condition)
}

sealed class Expr<T : IMoldableSet<T>> : IExpr<T> {
    override fun toString(): String {
        return ExprToString.toString(this)
    }
}

inline fun <reified R : IMoldableSet<R>> IExpr<*>.tryCast(): IExpr<R>? {
    return if (this.getSetClass().clazz == R::class) {
        @Suppress("UNCHECKED_CAST")
        this as IExpr<R>
    } else {
        null
    }
}

inline fun <reified T : IMoldableSet<T>, reified R : IExpr<T>> IExpr<*>.tryExactCast(): R? {
    return if (this::class == R::class && this.getSetClass() == T::class) {
        this as R
    } else {
        null
    }
}

fun <T : IMoldableSet<T>> IExpr<BooleanSet>.getConstraints(varKey: VariableExpression<T>): IExpr<T>? =
    ExprConstrain.getConstraints(this, varKey)

class ArithmeticExpression(val lhs: IExpr<NumberSet>, val rhs: IExpr<NumberSet>, val op: BinaryNumberOp) :
    Expr<NumberSet>()

class ComparisonExpression(val lhs: IExpr<NumberSet>, val rhs: IExpr<NumberSet>, val comp: ComparisonOp) :
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
            return if (a.getSetClass() == b.getSetClass()) {
                @Suppress("UNCHECKED_CAST")
                IfExpression(a, b as IExpr<A>, condition)
            } else {
                throw IllegalStateException("Incompatible types in if statement: ${a.getSetClass()} and ${b.getSetClass()}")
            }
        }
    }
}

class IntersectExpression<T : IMoldableSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>()
class BooleanInvertExpression(val expr: IExpr<BooleanSet>) : Expr<BooleanSet>()
class InvertExpression<T : IMoldableSet<T>>(val expr: IExpr<T>) : Expr<T>()
class NegateExpression(val expr: IExpr<NumberSet>) : Expr<NumberSet>()

/**
 * Returns the range of numbers above or below a given limit, depending on cmp.
 */
class NumberLimitsExpression(
    // The value we're either going to be above or below.
    val limit: IExpr<NumberSet>,
    // Whether we should flip the comparison operator or not.
    val shouldFlipCmp: IExpr<BooleanSet>,
    // The comparison operator to use.
    val cmp: ComparisonOp
) : Expr<NumberSet>()

class NumIterationTimesExpression(
    // How the variable is constrained; if the variable changes such that this returns false,
    // the loop will end.
    val constraint: IExpr<NumberSet>,
    // The variable that's being modified as it changes inside the loop.
    val variable: VariableExpression<NumberSet>,
    // How the variable is changing each iteration.
    val terms: ConstraintSolver.CollectedTerms
) : Expr<NumberSet>()

class UnionExpression<T : IMoldableSet<T>>(val lhs: IExpr<T>, val rhs: IExpr<T>) : Expr<T>()
class BooleanExpression(val lhs: IExpr<BooleanSet>, val rhs: IExpr<BooleanSet>, val op: BooleanOp) : Expr<BooleanSet>()

class ConstExpr<T : IMoldableSet<T>>(val singleElementSet: T) : Expr<T>()