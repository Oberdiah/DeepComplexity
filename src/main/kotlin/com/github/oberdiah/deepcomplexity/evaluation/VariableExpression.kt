package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.intellij.psi.PsiType

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we may or may not know the value of.
class VariableExpression<T : ConstrainedSetCollection<T>>(
    val myKey: VariableKey,
    val id: Int,
    val setInd: SetIndicator<T>
) : Expr<T>() {
    /**
     * An unresolved expression needs both its context and the element to correctly resolve it.
     */
    data class VariableKey(val key: Context.Key, var context: Context)

    /**
     * A variable is a moldable as *at a specific point in time*. Usually at the start of a block.
     */
    var resolvedInto: IExpr<T>? = null

    fun isResolved(): Boolean {
        return resolvedInto != null
    }

    fun getKey(): VariableKey {
        return myKey
    }

    fun setResolvedExpr(expr: IExpr<*>) {
        if (expr.getSetIndicator() != setInd)
            throw IllegalArgumentException(
                "Resolved expression is not of the correct type " +
                        "(expected ${setInd.clazz}, got ${expr.getSetIndicator().clazz})"
            )

        @Suppress("UNCHECKED_CAST")
        resolvedInto = expr as IExpr<T>
    }

    companion object {
        private var VARIABLE_ID = 0

        fun fromKey(contextKey: Context.Key, context: Context): VariableExpression<*> {
            VARIABLE_ID++

            val key = VariableKey(contextKey, context)

            val type: PsiType = contextKey.getType()
            val clazz = Utilities.psiTypeToKClass(type)
                ?: throw IllegalArgumentException("Unsupported type for variable expression")

            return VariableExpression(key, VARIABLE_ID, SetIndicator.fromClass(clazz))
        }
    }
}