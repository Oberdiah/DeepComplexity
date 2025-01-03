package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

class RepeatExpression(
    val numRepeats: ExprRetNum,
    /**
     * The expression that's repeated.
     */
    val exprToRepeat: Expr,
    /**
     * The expression as it was before any repeats.
     */
    val beforeRepeat: Expr
) : Expr {
    override fun getSetClass(): KClass<*> {
        return exprToRepeat.getSetClass()
    }

    override fun evaluate(): MoldableSet {
        // This is going to have to do some pretty intelligent things...
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "[repeat $numRepeats times] { $exprToRepeat }"
    }
}