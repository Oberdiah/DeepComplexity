package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator

/**
 * A marker representing a heap-allocated object. (i.e. its location in memory)
 * The index is used to distinguish different heap objects of the same type.
 */
@ConsistentCopyVisibility
data class HeapMarker private constructor(
    private val idx: Int,
    val type: PsiType,
    val isPlaceholder: Boolean
) {
    companion object {
        private var placeholders = mutableMapOf<PsiType, HeapMarker>()

        val NULL = HeapMarker(0, PsiTypes.nullType(), false)
        val VOID = HeapMarker(1, PsiTypes.voidType(), false)
        private var KEY_INDEX = 2

        fun new(type: PsiType): HeapMarker {
            if (type == PsiTypes.nullType()) return NULL
            if (type == PsiTypes.voidType()) return VOID
            return HeapMarker(KEY_INDEX++, type, false)
        }

        fun newPlaceholder(type: PsiType): HeapMarker {
            return placeholders.getOrPut(type) {
                HeapMarker(KEY_INDEX++, type, true)
            }
        }
    }

    val ind: ObjectIndicator = ObjectIndicator(type)
    override fun toString(): String {
        if (this == NULL) return "MyNull"
        if (this == VOID) return "MyVoid"
        return "#$idx"
    }
}