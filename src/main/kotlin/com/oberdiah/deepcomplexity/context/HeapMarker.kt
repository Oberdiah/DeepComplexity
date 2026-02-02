package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator

/**
 * A marker representing a heap-allocated object. (i.e. its location in memory)
 * The index is used to distinguish different heap objects of the same type.
 */
@ConsistentCopyVisibility
data class HeapMarker private constructor(
    private val idx: Int,
    val type: MyPsiType,
    val isPlaceholder: Boolean
) {
    companion object {
        private var placeholders = mutableMapOf<MyPsiType, HeapMarker>()

        val NULL = HeapMarker(0, MyPsiType.NULL_TYPE, false)
        val VOID = HeapMarker(1, MyPsiType.VOID_TYPE, false)
        private var KEY_INDEX = 2

        fun new(type: PsiType) = new(MyPsiType.of(type))
        fun new(type: MyPsiType): HeapMarker {
            if (type == MyPsiType.NULL_TYPE) return NULL
            if (type == MyPsiType.VOID_TYPE) return VOID
            return HeapMarker(KEY_INDEX++, type, false)
        }

        fun newPlaceholder(type: MyPsiType): HeapMarker {
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