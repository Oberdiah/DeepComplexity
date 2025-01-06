package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import kotlin.reflect.KClass

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression>
    fun getSetClass(): KClass<*>
    fun evaluate(condition: IExprRetBool): IMoldableSet
    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool
}

sealed interface IExprRetNum : IExpr {
    override fun evaluate(condition: IExprRetBool): NumberSet
    override fun getSetClass(): KClass<*> {
        return NumberSet::class
    }
}

sealed interface IExprRetBool : IExpr {
    override fun evaluate(condition: IExprRetBool): BooleanSet
    override fun getSetClass(): KClass<*> {
        return BooleanSet::class
    }

    /**
     * Returns, for every unresolved we depend on, an expression that when evaluated would return the set
     * of all values that would result in the condition being true.
     */
    fun getConstraints(): Map<VariableExpression, IExpr>
}

sealed interface IExprRetGeneric : IExpr {
    override fun evaluate(condition: IExprRetBool): GenericSet
    override fun getSetClass(): KClass<*> {
        return GenericSet::class
    }
}