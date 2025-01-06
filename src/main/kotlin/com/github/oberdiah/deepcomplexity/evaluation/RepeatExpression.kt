package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import kotlin.reflect.KClass

class RepeatExpression(
    val numRepeats: IExprRetNum,
    val exprToRepeat: IExpr,
) : IExpr {
    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }
}