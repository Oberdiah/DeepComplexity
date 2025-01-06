package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.VariableExpression.VariableKey
import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import kotlin.reflect.KClass

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression>

    /**
     * The class of the set itself.
     */
    fun getSetClass(): KClass<*>

    /**
     * The class of the elements in the set.
     */
    fun getBaseClass(): KClass<*>
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

    override fun getBaseClass(): KClass<*> {
        return Boolean::class
    }

//    /**
//     * Constrain the set to only include values that satisfy the condition.
//     */
//    fun constrain(varKey: VariableKey, set: IMoldableSet): IMoldableSet
}

sealed interface IExprRetGeneric : IExpr {
    override fun evaluate(condition: IExprRetBool): GenericSet
    override fun getSetClass(): KClass<*> {
        return GenericSet::class
    }
}