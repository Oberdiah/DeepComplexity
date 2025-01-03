package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

class RepeatExpression(
    val numRepeats: ExprRetNum,
    val exprToRepeat: Expr,
) : Expr {
    override fun getCurrentlyUnresolved(): Set<UnresolvedExpression.Unresolved> {
        return numRepeats.getCurrentlyUnresolved() + exprToRepeat.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return exprToRepeat.getSetClass()
    }

    override fun evaluate(): MoldableSet {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }
}