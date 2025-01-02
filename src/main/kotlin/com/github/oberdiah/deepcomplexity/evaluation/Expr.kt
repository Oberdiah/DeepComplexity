package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

sealed interface Expr {
    fun getSetClass(): KClass<*>
    fun evaluate(): MoldableSet
    fun asRetNum(): ExprRetNum? = this as? ExprRetNum
    fun asRetBool(): ExprRetBool? = this as? ExprRetBool
}

sealed interface ExprRetNum : Expr {
    override fun evaluate(): NumberSet
}

sealed interface ExprRetBool : Expr {
    override fun evaluate(): BooleanSet
}