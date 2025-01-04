package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.*
import com.github.weisj.jsvg.T
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import kotlin.reflect.KClass

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we don't yet know the value of, but would
// if we stepped out far enough.
object UnresolvedExpression {
    fun fromElement(element: PsiElement, context: Context): Unresolved {
        val type: PsiType =
            (element as? PsiVariable)?.type
                ?: throw IllegalArgumentException("Element must be a PsiVariable (got ${element::class})")

        val key = UnresolvedKey(element, context)

        return when (type) {
            PsiTypes.byteType(),
            PsiTypes.shortType(),
            PsiTypes.intType(),
            PsiTypes.longType(),
            PsiTypes.floatType(),
            PsiTypes.doubleType(),
                -> UnresolvedNumber(key)

            PsiTypes.booleanType() -> UnresolvedBool(key)
            else -> UnresolvedGeneric(key)
        }
    }

    fun onTheFlyUnresolvedNumber(): UnresolvedNumber {
        return UnresolvedNumber(null)
    }

    fun onTheFlyUnresolvedBool(): UnresolvedBool {
        return UnresolvedBool(null)
    }

    fun onTheFlyUnresolvedGeneric(): UnresolvedGeneric {
        return UnresolvedGeneric(null)
    }

    /**
     * An unresolved expression needs both its context and the element to correctly resolve it.
     */
    data class UnresolvedKey(val element: PsiElement, var context: Context)

    interface Unresolved : Expr {
        fun setResolvedExpr(expr: Expr)
        fun isResolved(): Boolean
        fun getKey(): UnresolvedKey
    }

    abstract class UnresolvedImpl<T>(private val key: UnresolvedKey?) : Unresolved {
        protected var resolvedExpr: T? = null

        override fun toString(): String {
            if (key == null) return "Unresolved (on-the-fly)"
            return if (isResolved()) resolvedExpr.toString() else key.element.toString()
        }

        override fun isResolved(): Boolean {
            return resolvedExpr != null
        }

        override fun getKey(): UnresolvedKey {
            if (key == null)
                throw IllegalStateException("Unresolved expression was created on-the-fly, cannot grab its key.")
            return key
        }

        override fun getCurrentlyUnresolved(): Set<Unresolved> {
            return if (isResolved()) setOf() else setOf(this)
        }
    }

    class UnresolvedBool(key: UnresolvedKey?) : UnresolvedImpl<ExprRetBool>(key),
        ExprRetBool {
        override fun setResolvedExpr(expr: Expr) {
            resolvedExpr = (expr as? ExprRetBool)
                ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
        }

        override fun evaluate(): BooleanSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }

    class UnresolvedNumber(key: UnresolvedKey?) : UnresolvedImpl<ExprRetNum>(key),
        ExprRetNum {
        override fun setResolvedExpr(expr: Expr) {
            resolvedExpr = (expr as? ExprRetNum)
                ?: throw IllegalArgumentException("Resolved expression must be a number expression")
        }

        override fun evaluate(): NumberSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }

    class UnresolvedGeneric(key: UnresolvedKey?) : UnresolvedImpl<ExprRetGeneric>(key),
        ExprRetGeneric {
        override fun setResolvedExpr(expr: Expr) {
            resolvedExpr = (expr as? ExprRetGeneric)
                ?: throw IllegalArgumentException("Resolved expression must be a generic expression")
        }

        override fun evaluate(): GenericSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }
}