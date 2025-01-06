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

    override fun getSetClass(): KClass<*> {
        return exprToRepeat.getSetClass()
    }

    override fun evaluate(): IMoldableSet {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }

    override fun deepClone(): IExpr {
        return RepeatExpression(numRepeats.deepClone(), exprToRepeat.deepClone())
    }
}