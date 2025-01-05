package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

sealed interface IExpr {
    fun getCurrentlyUnresolved(): Set<VariableExpression>
    fun getSetClass(): KClass<*>
    fun evaluate(): IMoldableSet
    fun asRetNum(): IExprRetNum? = this as? IExprRetNum
    fun asRetBool(): IExprRetBool? = this as? IExprRetBool

    fun addCondition(condition: IExprRetBool) {
        for (unresolved in getCurrentlyUnresolved()) {
            unresolved.addCondition(condition)
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