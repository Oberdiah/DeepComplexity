package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import kotlin.reflect.KClass

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we may or may not know the value of.
class VariableExpression<T : IMoldableSet<T>>(
    val myKey: VariableKey?,
    val id: Int,
    val baseClazz: KClass<*>,
    val setClazz: SetClass,
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
        if (myKey == null)
            throw IllegalStateException("Unresolved expression was created on-the-fly, cannot grab its key.")
        return myKey
    }

    fun setResolvedExpr(expr: IExpr<*>) {
        if (expr.getSetClass() != setClazz || expr.getBaseClass() != baseClazz)
            throw IllegalArgumentException(
                "Resolved expression is not of the correct type " +
                        "(expected $baseClazz, $setClazz, got ${expr.getBaseClass()}, ${expr.getSetClass()})"
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

            return when (type) {
                PsiTypes.byteType(),
                PsiTypes.shortType(),
                PsiTypes.intType(),
                PsiTypes.longType(),
                PsiTypes.floatType(),
                PsiTypes.doubleType() -> {
                    val clazz = Utilities.psiTypeToKClass(type)
                        ?: throw IllegalArgumentException("Unsupported type for variable expression")

                    VariableExpression(key, VARIABLE_ID, clazz, NumberSetClass, SetIndicator.fromClass(clazz))
                }

                PsiTypes.booleanType() -> VariableExpression(
                    key, VARIABLE_ID, Boolean::class, BooleanSetClass,
                    BooleanSetIndicator
                )

                else -> VariableExpression(key, VARIABLE_ID, Any::class, GenericSetClass, GenericSetIndicator)
            }
        }
    }
}