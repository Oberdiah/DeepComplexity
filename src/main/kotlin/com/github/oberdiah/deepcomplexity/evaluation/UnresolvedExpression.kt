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
class UnresolvedExpression<T : MoldableSet<T>>(val element: PsiElement) : Expression<T> {
    var underlyingSet: T? = null

    override fun evaluate(): T {
        throw NotImplementedError()
    }

    override fun toString(): String {
        return element.toString()
    }

    override fun getSetClass(): KClass<*> {
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
                -> NumberSet::class

            PsiTypes.booleanType() -> BooleanSet::class
            else -> GenericSet::class
        }
    }
}