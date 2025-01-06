package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import kotlin.reflect.KClass

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we may or may not know the value of.
sealed interface VariableExpression : IExpr {
    fun setResolvedExpr(expr: IExpr)

    /**
     * Whether we have a concrete expression for this yet or it's just a placeholder.
     */
    fun isResolved(): Boolean
    fun getKey(): VariableKey

    companion object {
        private var VARIABLE_ID = 0

        fun fromElement(element: PsiElement, context: Context): VariableExpression {
            VARIABLE_ID++

            val type: PsiType =
                (element as? PsiVariable)?.type
                    ?: throw IllegalArgumentException("Element must be a PsiVariable (got ${element::class})")

            val key = VariableKey(element, context)

            return when (type) {
                PsiTypes.byteType(),
                PsiTypes.shortType(),
                PsiTypes.intType(),
                PsiTypes.longType(),
                PsiTypes.floatType(),
                PsiTypes.doubleType() -> {
                    val clazz = Utilities.psiTypeToKClass(type)
                        ?: throw IllegalArgumentException("Unsupported type for variable expression")

                    VariableNumber(key, clazz, VARIABLE_ID)
                }

                PsiTypes.booleanType() -> VariableBool(key, VARIABLE_ID)
                else -> VariableGeneric(key, VARIABLE_ID)
            }
        }
    }


    /**
     * An unresolved expression needs both its context and the element to correctly resolve it.
     */
    data class VariableKey(val element: PsiElement, var context: Context)

    /**
     * A variable is a moldable as *at a specific point in time*. Usually at the start of a block.
     */
    sealed class VariableImpl<T : IExpr>(private val key: VariableKey?, private val id: Int) : VariableExpression {
        protected var resolvedInto: T? = null

        override fun toString(): String {
            if (key == null) return "Unresolved (on-the-fly)"
            return if (isResolved()) resolvedInto.toString() else (key.element.toString() + "[$$id]")
        }

        override fun isResolved(): Boolean {
            return resolvedInto != null
        }

        override fun getKey(): VariableKey {
            if (key == null)
                throw IllegalStateException("Unresolved expression was created on-the-fly, cannot grab its key.")
            return key
        }

        override fun getVariables(resolved: Boolean): Set<VariableExpression> {
            val resolvedVariables = resolvedInto?.getVariables(resolved) ?: emptySet()

            return if ((isResolved() && resolved) || (!isResolved() && !resolved)) {
                resolvedVariables + this
            } else {
                resolvedVariables
            }
        }
    }

    class VariableBool(key: VariableKey?, id: Int) : VariableImpl<IExprRetBool>(key, id),
        IExprRetBool {
        override fun setResolvedExpr(expr: IExpr) {
            resolvedInto = (expr as? IExprRetBool)
                ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
        }

        override fun evaluate(condition: IExprRetBool): BooleanSet {
            return (resolvedInto?.evaluate(condition) ?: throw IllegalStateException("Unresolved expression"))
        }
    }

    class VariableNumber(key: VariableKey?, val clazz: KClass<*>, id: Int) : VariableImpl<IExprRetNum>(key, id),
        IExprRetNum {
        override fun setResolvedExpr(expr: IExpr) {
            resolvedInto = (expr as? IExprRetNum)
                ?: throw IllegalArgumentException("Resolved expression must be a number expression")
        }

        override fun evaluate(condition: IExprRetBool): NumberSet {
            resolvedInto?.let {
                return it.evaluate(condition)
            }

            // If we're here we're at the end of the line, assume a full range.
            val range = NumberSet.fullRange(clazz)
//            return condition.constrain(range, getKey())
            return range
        }

        override fun getBaseClass(): KClass<*> {
            return clazz
        }
    }

    class VariableGeneric(key: VariableKey?, id: Int) : VariableImpl<IExprRetGeneric>(key, id),
        IExprRetGeneric {
        override fun setResolvedExpr(expr: IExpr) {
            resolvedInto = (expr as? IExprRetGeneric)
                ?: throw IllegalArgumentException("Resolved expression must be a generic expression")
        }

        override fun evaluate(condition: IExprRetBool): GenericSet {
            return (resolvedInto?.evaluate(condition) ?: throw IllegalStateException("Unresolved expression"))
        }

        override fun getBaseClass(): KClass<*> {
            throw IllegalStateException("Base class for a generic is a strange concept...")
        }
    }
}