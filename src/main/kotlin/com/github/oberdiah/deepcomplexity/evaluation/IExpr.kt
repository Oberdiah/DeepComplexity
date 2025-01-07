package com.github.oberdiah.deepcomplexity.evaluation

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