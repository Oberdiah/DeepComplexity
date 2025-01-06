package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import kotlin.reflect.KClass

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression>
    fun getSetClass(): KClass<*>
    fun evaluate(condition: IExprRetBool): IMoldableSet
    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool
    fun asRetGeneric(): IExprRetGeneric? = this as? IExprRetGeneric
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
}

sealed interface IExprRetGeneric : IExpr {
    override fun evaluate(condition: IExprRetBool): GenericSet
    override fun getSetClass(): KClass<*> {
        return GenericSet::class
    }
}