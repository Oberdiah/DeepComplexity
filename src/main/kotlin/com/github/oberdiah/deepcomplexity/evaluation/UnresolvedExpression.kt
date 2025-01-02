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
import com.jetbrains.rd.util.string.printToString
import kotlin.reflect.KClass

// Element is either PsiLocalVariable, PsiParameter, or PsiField
// This represents a variable which we don't yet know the value of, but would
// if we stepped out far enough.
class UnresolvedExpression<T : MoldableSet<T>> private constructor(
    val element: PsiElement,
    val underlyingSetClass: KClass<*>
) : Expression<T>(underlyingSetClass) {
    var underlyingSet: T? = null

    override fun evaluate(): T {
        throw NotImplementedError()
    }

    override fun toString(): String {
        return element.toString()
    }

    companion object {
        fun fromElement(element: PsiElement): UnresolvedExpression<*> {
            val type: PsiType =
                (element as? PsiVariable)?.type
                    ?: throw IllegalArgumentException("Element must be a PsiVariable (got ${element::class})")

            val clazz: KClass<*> = when (type) {
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

            return UnresolvedExpression(element, clazz)
        }
    }
}