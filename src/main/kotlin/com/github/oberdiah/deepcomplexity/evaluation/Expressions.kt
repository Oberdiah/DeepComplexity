package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOperation.AND
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOperation.OR
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

sealed interface IExprRetNum : IExpr
sealed interface IExprRetBool : IExpr
sealed interface IExprRetGeneric : IExpr

class ArithmeticExpression(
    val lhs: IExprRetNum,
    val rhs: IExprRetNum,
    val operation: BinaryNumberOperation
) : IExprRetNum {
    override fun toString(): String {
        return "($lhs $operation $rhs)"
    }
}

class ComparisonExpression(
    val lhs: IExprRetNum,
    val rhs: IExprRetNum,
    val comparison: ComparisonOperation
) : IExprRetBool {
    override fun toString(): String {
        return "($lhs $comparison $rhs)"
    }
}

class IfExpression(
    val trueExpr: IExpr,
    val falseExpr: IExpr,
    val thisCondition: IExprRetBool,
) : IExpr {
    override fun toString(): String {
        return "if $thisCondition {\n${
            trueExpr.toString().prependIndent()
        }\n} else {\n${
            falseExpr.toString().prependIndent()
        }\n}"
    }
}

class IntersectExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun toString(): String {
        return "($lhs ∩ $rhs)"
    }
}

class InvertExpression(val expr: IExprRetBool) : IExprRetBool {
    override fun toString(): String {
        return "!$expr"
    }
}

class RepeatExpression(
    val numRepeats: IExprRetNum,
    val exprToRepeat: IExpr,
) : IExpr {
    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }
}

class UnionExpression(val lhs: IExpr, val rhs: IExpr) : IExpr {
    override fun toString(): String {
        return "($lhs ∪ $rhs)"
    }
}

sealed class ConstExpr<T>(val singleElementSet: T) : IExpr {
    override fun toString(): String {
        return singleElementSet.toString()
    }
}

class ConstExprNum(singleElementSet: NumberSet) :
    ConstExpr<NumberSet>(singleElementSet), IExprRetNum

class ConstExprBool(singleElementSet: BooleanSet) :
    ConstExpr<BooleanSet>(singleElementSet), IExprRetBool

class ConstExprGeneric(singleElementSet: GenericSet) :
    ConstExpr<GenericSet>(singleElementSet), IExprRetGeneric

class BooleanExpression(
    val lhs: IExprRetBool,
    val rhs: IExprRetBool,
    val operation: BooleanOperation
) : IExprRetBool {
    override fun toString(): String {
        if (lhs == ConstantExpression.TRUE) {
            return when (operation) {
                AND -> rhs.toString()
                OR -> "TRUE"
            }
        } else if (lhs == ConstantExpression.FALSE) {
            return when (operation) {
                AND -> "FALSE"
                OR -> rhs.toString()
            }
        } else if (rhs == ConstantExpression.TRUE) {
            return when (operation) {
                AND -> lhs.toString()
                OR -> "TRUE"
            }
        } else if (rhs == ConstantExpression.FALSE) {
            return when (operation) {
                AND -> "FALSE"
                OR -> lhs.toString()
            }
        }

        return "($lhs $operation $rhs)"
    }
}