package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable

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

    abstract class VariableImpl<T>(private val key: VariableKey?) : VariableExpression {
        protected var resolvedExpr: T? = null

        // Applied as an intersection to our evaluation.
        var constraints: IExpr = GaveUpExpression(this)

        override fun addCondition(condition: IExprRetBool) {
            // Do the immensely complicated work of converting from this to a constraint.
            constraints = condition // This is completely wrong, just a POC.
        }

        override fun toString(): String {
            if (key == null) return "Unresolved (on-the-fly)"
            return (if (isResolved()) resolvedExpr.toString() else key.element.toString())// + "[${constraints}]"
        }

        override fun isResolved(): Boolean {
            return resolvedExpr != null
        }

        override fun getKey(): VariableKey {
            if (key == null)
                throw IllegalStateException("Unresolved expression was created on-the-fly, cannot grab its key.")
            return key
        }

        override fun getCurrentlyUnresolved(): Set<VariableExpression> {
            return if (isResolved()) setOf() else setOf(this)
        }
    }

    class VariableBool(key: VariableKey?) : VariableImpl<IExprRetBool>(key),
        IExprRetBool {
        override fun setResolvedExpr(expr: IExpr) {
            resolvedExpr = (expr as? IExprRetBool)
                ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
        }

        override fun evaluate(): BooleanSet {
            return (resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression"))
                .intersect(constraints.evaluate()) as BooleanSet
        }

        override fun getConstraints(): Map<VariableExpression, IExpr> {
            return resolvedExpr?.getConstraints() ?: mapOf(this to this)
        }
    }

    class VariableNumber(key: VariableKey?) : VariableImpl<IExprRetNum>(key),
        IExprRetNum {
        override fun setResolvedExpr(expr: IExpr) {
            resolvedExpr = (expr as? IExprRetNum)
                ?: throw IllegalArgumentException("Resolved expression must be a number expression")
        }

        override fun evaluate(): NumberSet {
            return (resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression"))
                .intersect(constraints.evaluate()) as NumberSet
        }
    }

    class VariableGeneric(key: VariableKey?) : VariableImpl<IExprRetGeneric>(key),
        IExprRetGeneric {
        override fun setResolvedExpr(expr: IExpr) {
            resolvedExpr = (expr as? IExprRetGeneric)
                ?: throw IllegalArgumentException("Resolved expression must be a generic expression")
        }

        override fun evaluate(): GenericSet {
            return (resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression"))
                .intersect(constraints.evaluate()) as GenericSet
        }
    }
}