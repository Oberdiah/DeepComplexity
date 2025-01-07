package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.VariableExpression.VariableKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.github.weisj.jsvg.T
import kotlin.reflect.KClass

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression> = ExprGetVariables.getVariables(this, resolved)
    fun getBaseClass(): KClass<*> = ExprClass.getBaseClass(this)
    fun getSetClass(): KClass<*> = ExprClass.getSetClass(this)
    fun evaluate(condition: IExprRetBool): IMoldableSet = ExprEvaluate.evaluate(this, condition)

    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool
    fun asRetGeneric(): IExprRetGeneric? = this as? IExprRetGeneric
}

sealed class Expr : IExpr {
    override fun toString(): String {
        return ExprToString.toString(this)
    }
}

sealed interface IExprRetNum : IExpr {
    override fun evaluate(condition: IExprRetBool): NumberSet = ExprEvaluate.evaluate(this, condition)
}

sealed interface IExprRetBool : IExpr {
    override fun evaluate(condition: IExprRetBool): BooleanSet = ExprEvaluate.evaluate(this, condition)
    fun getConstraints(varKey: VariableExpression): IMoldableSet? =
        ExprConstrain.getConstraints(this, varKey)
}

sealed interface IExprRetGeneric : IExpr {
    override fun evaluate(condition: IExprRetBool): GenericSet = ExprEvaluate.evaluate(this, condition)
}

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