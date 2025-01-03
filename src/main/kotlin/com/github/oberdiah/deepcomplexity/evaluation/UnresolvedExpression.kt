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

    abstract class Unresolved(val element: PsiElement) : Expr {
        abstract var resolvedExpr: Expr?

        override fun toString(): String {
            return if (resolvedExpr != null) resolvedExpr.toString() else element.toString()
        }
    }

    class UnresolvedBool(element: PsiElement) : Unresolved(element), ExprRetBool {
        override var resolvedExpr: Expr? = null
            set(value) {
                field = (value as? ExprRetBool)
                    ?: throw IllegalArgumentException("Resolved expression must be a boolean expression")
            }

        override fun evaluate(): BooleanSet {
            return (resolvedExpr as ExprRetBool?)?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }

    class UnresolvedNumber(element: PsiElement) : Unresolved(element), ExprRetNum {
        override var resolvedExpr: Expr? = null
            set(value) {
                field = (value as? ExprRetNum)
                    ?: throw IllegalArgumentException("Resolved expression must be a number expression")
            }

        override fun evaluate(): NumberSet {
            return (resolvedExpr as ExprRetNum?)?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }
    }

    class UnresolvedGeneric(element: PsiElement) : Unresolved(element) {
        override var resolvedExpr: Expr? = null
        override fun evaluate(): MoldableSet {
            return resolvedExpr?.evaluate() ?: throw IllegalStateException("Unresolved expression")
        }

        override fun getSetClass(): KClass<*> {
            return GenericSet::class
        }
    }
}