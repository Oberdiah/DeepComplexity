package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import kotlin.reflect.KClass

sealed interface IExpr {
    fun getVariables(resolved: Boolean): Set<VariableExpression>
    fun getSetClass(): KClass<*>
    fun evaluate(): IMoldableSet
    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool
    fun deepClone(): IExpr

    /**
     * When you add a condition you need to provide a context that that condition applies within, as
     * an IExpr won't just have variables under a single context.
     */
    fun addCondition(condition: IExprRetBool, context: Context) {
        for (unresolved in getVariables(false)) {
            unresolved.addCondition(condition, context)
        }
    }
}

sealed interface IExprRetNum : IExpr {
    override fun evaluate(): NumberSet
    override fun getSetClass(): KClass<*> {
        return NumberSet::class
    }
}

sealed interface IExprRetBool : IExpr {
    override fun evaluate(): BooleanSet
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
    override fun evaluate(): GenericSet
    override fun getSetClass(): KClass<*> {
        return GenericSet::class
    }
}