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
    fun checkConstraints()

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

        // Applied as an intersection to our evaluation.
        var constraint: IExpr = GaveUpExpression(this)

        // Kept around so we can check it again in future for constraints as it evolves.
        private var condition: IExprRetBool = ConstantExpression.TRUE

        override fun addCondition(condition: IExprRetBool, context: Context) {
            val resolved = this.resolved
            if (resolved != null) {
                // We don't expect this to be called because at the moment addCondition walks the unresolved tree,
                // but just in case.
                resolved.addCondition(condition, context)
                return
            }

            // This is very important. We only want to accept conditions that apply to us.
            if (key == null || context != key.context) {
                return
            }

            if (this.condition == ConstantExpression.TRUE) {
                this.condition = condition
            } else {
                this.condition = BooleanExpression(this.condition, condition, BooleanOperation.AND)
            }

            checkConstraints()
        }

        override fun checkConstraints() {
            // Do the complicated work of converting from the condition to a constraint.
            // If the constraint doesn't contain us, it doesn't concern us :)
            if (condition.getVariables(false).any { it.getKey() == key }) {
                if (constraint !is GaveUpExpression) {
                    // This requires further investigation...
                    println("Interesting :)")
                }

                constraint = condition // This is completely wrong, just a POC.
                println("Woo got here! ${condition}, ${key?.element}")
            }
        }

        protected fun updateExprConditions(expr: IExpr) {
            expr.addCondition(condition.deepClone(), getKey().context)
        }

        override fun toString(): String {
            if (key == null) return "Unresolved (on-the-fly)"
            val constraintStr = if (constraint is GaveUpExpression) "" else "[$constraint]"
            val mainExprStr = if (isResolved()) resolved.toString() else key.element.toString()
            return mainExprStr + constraintStr
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
            val conditionVariables = condition.getVariables(resolved)

            return if ((isResolved() && resolved) || (!isResolved() && !resolved)) {
                conditionVariables + this
            } else {
                conditionVariables - this
            }
        }
    }

    class VariableBool(key: VariableKey?) : VariableImpl<IExprRetBool>(key),
        IExprRetBool {
        override fun setResolvedExpr(expr: IExpr) {
            updateExprConditions(expr)
            resolved = (expr as? IExprRetBool)
                ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
        }

        override fun evaluate(): BooleanSet {
            return (resolved?.evaluate() ?: throw IllegalStateException("Unresolved expression"))
                .intersect(constraint.evaluate()) as BooleanSet
        }

        override fun getConstraints(): Map<VariableExpression, IExpr> {
            return resolved?.getConstraints() ?: mapOf(this to this)
        }

        override fun deepClone(): IExprRetBool {
            val clone = VariableBool(getKey())
            clone.resolved = resolved?.deepClone()
            return clone
        }
    }

    class VariableNumber(key: VariableKey?) : VariableImpl<IExprRetNum>(key),
        IExprRetNum {
        override fun setResolvedExpr(expr: IExpr) {
            updateExprConditions(expr)
            resolved = (expr as? IExprRetNum)
                ?: throw IllegalArgumentException("Resolved expression must be a number expression")
        }

        override fun evaluate(): NumberSet {
            return (resolved?.evaluate() ?: throw IllegalStateException("Unresolved expression"))
                .intersect(constraint.evaluate()) as NumberSet
        }

        override fun deepClone(): IExprRetNum {
            val clone = VariableNumber(getKey())
            clone.resolved = resolved?.deepClone()
            return clone
        }
    }

    class VariableGeneric(key: VariableKey?) : VariableImpl<IExprRetGeneric>(key),
        IExprRetGeneric {
        override fun setResolvedExpr(expr: IExpr) {
            updateExprConditions(expr)
            resolved = (expr as? IExprRetGeneric)
                ?: throw IllegalArgumentException("Resolved expression must be a generic expression")
        }

        override fun evaluate(): GenericSet {
            return (resolved?.evaluate() ?: throw IllegalStateException("Unresolved expression"))
                .intersect(constraint.evaluate()) as GenericSet
        }

        override fun deepClone(): IExprRetGeneric {
            val clone = VariableGeneric(getKey())
            clone.resolved = resolved?.deepClone()
            return clone
        }
    }
}