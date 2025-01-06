package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return ExprGetVariables.getVariables(this, resolved)
    }

    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool
    fun asRetGeneric(): IExprRetGeneric? = this as? IExprRetGeneric
}

sealed interface IExprRetNum : IExpr
sealed interface IExprRetBool : IExpr {

//    /**
//     * Constrain the set to only include values that satisfy the condition.
//     */
//    fun constrain(varKey: VariableKey, set: IMoldableSet): IMoldableSet
}

sealed interface IExprRetGeneric : IExpr