package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return ExprGetVariables.getVariables(this, resolved)
    }

    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool
    fun asRetGeneric(): IExprRetGeneric? = this as? IExprRetGeneric
}

sealed class Expr : IExpr {
    override fun toString(): String {
        return ExprToString.toString(this)
    }
}

sealed interface IExprRetNum : IExpr
sealed interface IExprRetBool : IExpr
sealed interface IExprRetGeneric : IExpr

class ArithmeticExpression(val lhs: IExprRetNum, val rhs: IExprRetNum, val op: BinaryNumberOp) : Expr(), IExprRetNum
class ComparisonExpression(val lhs: IExprRetNum, val rhs: IExprRetNum, val comp: ComparisonOp) : Expr(), IExprRetBool
class IfExpression(val trueExpr: IExpr, val falseExpr: IExpr, val thisCondition: IExprRetBool) : Expr()
class IntersectExpression(val lhs: IExpr, val rhs: IExpr) : Expr()
class InvertExpression(val expr: IExprRetBool) : Expr(), IExprRetBool
class RepeatExpression(val numRepeats: IExprRetNum, val exprToRepeat: IExpr) : Expr()
class UnionExpression(val lhs: IExpr, val rhs: IExpr) : Expr()
class BooleanExpression(val lhs: IExprRetBool, val rhs: IExprRetBool, val op: BooleanOp) : Expr(), IExprRetBool

sealed class ConstExpr<T>(val singleElementSet: T) : Expr()
class ConstExprNum(singleElementSet: NumberSet) : ConstExpr<NumberSet>(singleElementSet), IExprRetNum
class ConstExprBool(singleElementSet: BooleanSet) : ConstExpr<BooleanSet>(singleElementSet), IExprRetBool
class ConstExprGeneric(singleElementSet: GenericSet) : ConstExpr<GenericSet>(singleElementSet), IExprRetGeneric