package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.oberdiah.deepcomplexity.utilities.Utilities.toStringPretty

/**
 * The main PsiType is relatively slow (especially the .equals()), so we create a wrapper around it.
 */
class MyPsiType(private val idx: Int, val psiType: PsiType) {
    companion object {
        val NULL_TYPE = MyPsiType(-1, PsiTypes.nullType())
        val VOID_TYPE = MyPsiType(-2, PsiTypes.voidType())

        private val typeMap = mutableMapOf(
            NULL_TYPE.psiType to NULL_TYPE,
            VOID_TYPE.psiType to VOID_TYPE
        )
        private var KEY_INDEX = 0

        fun of(psiType: PsiType): MyPsiType {
            return typeMap.getOrPut(psiType) {
                MyPsiType(KEY_INDEX++, psiType)
            }
        }
    }

    val str: String by lazy {
        psiType.toStringPretty()
    }

    override fun toString(): String = str

    override fun equals(other: Any?): Boolean {
        return other is MyPsiType && this.idx == other.idx
    }

    override fun hashCode(): Int = idx
}