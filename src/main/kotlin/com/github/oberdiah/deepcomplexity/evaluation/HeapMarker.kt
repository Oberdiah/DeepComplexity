package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.intellij.psi.PsiType

/**
 * A marker representing a heap-allocated object. (i.e. its location in memory)
 * The index is used to distinguish different heap objects of the same type.
 */
data class HeapMarker(
    private val idx: Int,
    val type: PsiType,
) : Context.QualifierRef {
    companion object {
        private var KEY_INDEX = 1
        fun new(type: PsiType): HeapMarker = HeapMarker(KEY_INDEX++, type)
    }

    override val ind: ObjectSetIndicator = ObjectSetIndicator(type)
    override fun toString(): String = "#$idx"
}