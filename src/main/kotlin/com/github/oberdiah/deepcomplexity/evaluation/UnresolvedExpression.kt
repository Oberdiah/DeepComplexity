package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
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
    fun fromElement(element: PsiElement): Unresolved {
        val type: PsiType =
            (element as? PsiVariable)?.type
                ?: throw IllegalArgumentException("Element must be a PsiVariable (got ${element::class})")

        return when (type) {
            PsiTypes.byteType(),
            PsiTypes.shortType(),
            PsiTypes.intType(),
            PsiTypes.longType(),
            PsiTypes.floatType(),
            PsiTypes.doubleType(),
                -> UnresolvedNumber(element)

            PsiTypes.booleanType() -> UnresolvedBool(element)
            else -> UnresolvedGeneric(element)
        }
    }

    interface Unresolved : Expr {
        fun setResolvedExpr(expr: Expr)
    }

    abstract class UnresolvedImpl<T>(val element: PsiElement) : Unresolved {
        protected var resolvedExpr: T? = null

        override fun toString(): String {
            return if (resolvedExpr != null) resolvedExpr.toString() else element.toString()
        }

        override fun getUnresolved(): Set<Unresolved> {
            return setOf(this)
        }
    }

    class UnresolvedBool(element: PsiElement) : UnresolvedImpl<ExprRetBool>(element), ExprRetBool {
        override fun setResolvedExpr(expr: Expr) {
            resolvedExpr = (expr as? ExprRetBool)
                ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
        }

        override fun evaluate(): BooleanSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }

    class UnresolvedNumber(element: PsiElement) : UnresolvedImpl<ExprRetNum>(element), ExprRetNum {
        override fun setResolvedExpr(expr: Expr) {
            resolvedExpr = (expr as? ExprRetNum)
                ?: throw IllegalArgumentException("Resolved expression must be a number expression")
        }

        override fun evaluate(): NumberSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }

    class UnresolvedGeneric(element: PsiElement) : UnresolvedImpl<ExprRetGeneric>(element), ExprRetGeneric {
        override fun setResolvedExpr(expr: Expr) {
            resolvedExpr = (expr as? ExprRetGeneric)
                ?: throw IllegalArgumentException("Resolved expression must be a generic expression")
        }

        override fun evaluate(): GenericSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }
}