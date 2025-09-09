package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.intellij.psi.PsiType

class HeapIdent private constructor(val id: Int, val psiType: PsiType) {
    companion object {
        private var NEXT_ID = 0
        fun createNew(psiType: PsiType): HeapIdent = HeapIdent(NEXT_ID++, psiType)
    }

    val ind = ObjectSetIndicator(psiType)

    override fun toString(): String = "H$id"
}