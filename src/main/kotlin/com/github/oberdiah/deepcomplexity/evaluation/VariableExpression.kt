package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.evaluation.BooleanExpression.BooleanOperation
import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.checkConstraint

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we may or may not know the value of.
interface VariableExpression : IExpr {
    fun setResolvedExpr(expr: IExpr)

    /**
     * Whether we have a concrete expression for this yet or it's just a placeholder.
     */
    fun isResolved(): Boolean
    fun getKey(): VariableKey

    companion object {
        fun fromElement(element: PsiElement, context: Context): VariableExpression {
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
                PsiTypes.doubleType(),
                    -> VariableNumber(key)

                PsiTypes.booleanType() -> VariableBool(key)
                else -> VariableGeneric(key)
            }
        }

        fun onTheFlyUnresolvedNumber(): VariableNumber {
            return VariableNumber(null)
        }

        fun onTheFlyUnresolvedBool(): VariableBool {
            return VariableBool(null)
        }

        fun onTheFlyUnresolvedGeneric(): VariableGeneric {
            return VariableGeneric(null)
        }
    }


    /**
     * An unresolved expression needs both its context and the element to correctly resolve it.
     */
    data class VariableKey(val element: PsiElement, var context: Context)

    /**
     * A variable is a moldable as *at a specific point in time*. Usually at the start of a block.
     */
    abstract class VariableImpl<T : IExpr>(private val key: VariableKey?) : VariableExpression {
        protected var resolved: T? = null

        override fun toString(): String {
            if (key == null) return "Unresolved (on-the-fly)"
            return if (isResolved()) resolved.toString() else key.element.toString()
        }

        override fun isResolved(): Boolean {
            return resolved != null
        }

        override fun getKey(): VariableKey {
            if (key == null)
                throw IllegalStateException("Unresolved expression was created on-the-fly, cannot grab its key.")
            return key
        }

        override fun getVariables(resolved: Boolean): Set<VariableExpression> {
            return if ((isResolved() && resolved) || (!isResolved() && !resolved)) {
                setOf(this)
            } else {
                emptySet()
            }
        }
    }

    class VariableBool(key: VariableKey?) : VariableImpl<IExprRetBool>(key),
        IExprRetBool {
        override fun setResolvedExpr(expr: IExpr) {
            resolved = (expr as? IExprRetBool)
                ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
        }

        override fun evaluate(condition: IExprRetBool): BooleanSet {
            return (resolved?.evaluate(condition) ?: throw IllegalStateException("Unresolved expression"))
        }
    }

    class VariableNumber(key: VariableKey?) : VariableImpl<IExprRetNum>(key),
        IExprRetNum {
        override fun setResolvedExpr(expr: IExpr) {
            resolved = (expr as? IExprRetNum)
                ?: throw IllegalArgumentException("Resolved expression must be a number expression")
        }

        override fun evaluate(condition: IExprRetBool): NumberSet {
            return (resolved?.evaluate(condition) ?: throw IllegalStateException("Unresolved expression"))
        }
    }

    class VariableGeneric(key: VariableKey?) : VariableImpl<IExprRetGeneric>(key),
        IExprRetGeneric {
        override fun setResolvedExpr(expr: IExpr) {
            resolved = (expr as? IExprRetGeneric)
                ?: throw IllegalArgumentException("Resolved expression must be a generic expression")
        }

        override fun evaluate(condition: IExprRetBool): GenericSet {
            return (resolved?.evaluate(condition) ?: throw IllegalStateException("Unresolved expression"))
        }
    }
}