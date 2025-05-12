package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we may or may not know the value of.
class VariableExpression<T : Any>(
    val key: Context.Key,
    val id: Int,
    val setInd: SetIndicator<T>
) : Expr<T>() {
    /**
     * A variable is a moldable as *at a specific point in time*. Usually at the start of a block.
     */
    var resolvedInto: IExpr<T>? = null

    fun isResolved(): Boolean {
        return resolvedInto != null
    }

    fun setResolvedExpr(expr: IExpr<*>) {
        if (expr.ind != setInd)
            throw IllegalArgumentException(
                "Resolved expression is not of the correct type " +
                        "(expected ${setInd.clazz}, got ${expr.ind.clazz})"
            )

        @Suppress("UNCHECKED_CAST")
        resolvedInto = expr as IExpr<T>
    }

    companion object {
        private var VARIABLE_ID = 0

        fun fromKey(contextKey: Context.Key, context: Context): VariableExpression<*> {
            VARIABLE_ID++
            return VariableExpression(contextKey, VARIABLE_ID, contextKey.ind)
        }
    }
}