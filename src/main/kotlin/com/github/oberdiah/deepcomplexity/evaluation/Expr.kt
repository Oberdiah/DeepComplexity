package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.UnresolvedExpression.Unresolved
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

sealed interface Expr {
    fun getCurrentlyUnresolved(): Set<Unresolved>
    fun getSetClass(): KClass<*>
    fun evaluate(): MoldableSet
    fun asRetNum(): ExprRetNum? = this as? ExprRetNum
    fun asRetBool(): ExprRetBool? = this as? ExprRetBool
}

sealed interface ExprRetNum : Expr {
    override fun evaluate(): NumberSet
    override fun getSetClass(): KClass<*> {
        return NumberSet::class
    }
}

sealed interface ExprRetBool : Expr {
    override fun evaluate(): BooleanSet
    override fun getSetClass(): KClass<*> {
        return BooleanSet::class
    }
}

sealed interface ExprRetGeneric : Expr {
    override fun evaluate(): GenericSet
    override fun getSetClass(): KClass<*> {
        return GenericSet::class
    }
}