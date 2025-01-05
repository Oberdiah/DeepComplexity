package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class RepeatExpression(
    val numRepeats: IExprRetNum,
    val exprToRepeat: IExpr,
) : IExpr {
    override fun getCurrentlyUnresolved(): Set<VariableExpression> {
        return numRepeats.getCurrentlyUnresolved() + exprToRepeat.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return exprToRepeat.getSetClass()
    }

    override fun evaluate(): IMoldableSet {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }
}