package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class RepeatExpression(
    val numRepeats: IExprRetNum,
    val exprToRepeat: IExpr,
) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return numRepeats.getVariables(resolved) + exprToRepeat.getVariables(resolved)
    }

    override fun getBaseClass(): KClass<*> {
        return exprToRepeat.getBaseClass()
    }

    override fun evaluate(condition: IExprRetBool): IMoldableSet {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }
}