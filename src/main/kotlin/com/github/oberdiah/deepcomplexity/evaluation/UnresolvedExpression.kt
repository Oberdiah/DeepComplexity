package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import kotlin.reflect.KClass

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we don't yet know the value of, but would
// if we stepped out far enough.
object UnresolvedExpression {
    fun fromElement(element: PsiElement): Expr {
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
        override fun toString(): String {
            return element.toString()
        }
    }

    class UnresolvedBool(element: PsiElement) : Unresolved(element), ExprRetBool {
        override fun evaluate(): BooleanSet {
            TODO()
        }
    }

    class UnresolvedNumber(element: PsiElement) : Unresolved(element), ExprRetNum {
        override fun evaluate(): NumberSet {
            TODO()
        }
    }

    class UnresolvedGeneric(element: PsiElement) : Unresolved(element) {
        override fun evaluate(): MoldableSet {
            TODO()
        }

        override fun getSetClass(): KClass<*> {
            return GenericSet::class
        }
    }
}