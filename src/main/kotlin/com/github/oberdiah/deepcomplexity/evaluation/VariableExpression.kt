package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.github.weisj.jsvg.T
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import kotlin.reflect.KClass

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we may or may not know the value of.
sealed interface VariableExpression<T : IMoldableSet<T>> : IExpr<T> {
    fun setResolvedExpr(expr: IExpr<*>)

    /**
     * Whether we have a concrete expression for this yet or it's just a placeholder.
     */
    fun isResolved(): Boolean
    fun getKey(): VariableKey

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

                    VariableImpl(key, VARIABLE_ID, clazz, NumberSetClass)
                }

                PsiTypes.booleanType() -> VariableImpl(key, VARIABLE_ID, Boolean::class, BooleanSetClass)
                else -> VariableImpl(key, VARIABLE_ID, Any::class, GenericSetClass)
            }
        }
    }


    /**
     * An unresolved expression needs both its context and the element to correctly resolve it.
     */
    data class VariableKey(val key: Context.Key, var context: Context)

    /**
     * A variable is a moldable as *at a specific point in time*. Usually at the start of a block.
     */
    class VariableImpl<T : IMoldableSet<T>>(
        val myKey: VariableKey?,
        val id: Int,
        val baseClazz: KClass<*>,
        val setClazz: SetClass<T>
    ) : Expr<T>(),
        VariableExpression<T> {
        var resolvedInto: IExpr<T>? = null

        override fun isResolved(): Boolean {
            return resolvedInto != null
        }

        override fun getKey(): VariableKey {
            if (myKey == null)
                throw IllegalStateException("Unresolved expression was created on-the-fly, cannot grab its key.")
            return myKey
        }

        override fun setResolvedExpr(expr: IExpr<*>) {
            if (expr.getSetClass() != setClazz || expr.getBaseClass() != baseClazz)
                throw IllegalArgumentException(
                    "Resolved expression is not of the correct type " +
                            "(expected $baseClazz, $setClazz, got ${expr.getBaseClass()}, ${expr.getSetClass()})"
                )

            @Suppress("UNCHECKED_CAST")
            resolvedInto = expr as IExpr<T>
        }
    }
}