package com.github.oberdiah.deepcomplexity.context

import com.github.oberdiah.deepcomplexity.evaluation.ConstExpr
import com.github.oberdiah.deepcomplexity.evaluation.Expr
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes

/**
 * A marker representing a heap-allocated object. (i.e. its location in memory)
 * The index is used to distinguish different heap objects of the same type.
 */
@ConsistentCopyVisibility
data class HeapMarker private constructor(
    private val idx: Int,
    val type: PsiType,
) : Qualifier {
    companion object {
        val NULL = HeapMarker(0, PsiTypes.nullType())
        val VOID = HeapMarker(1, PsiTypes.voidType())
        private var KEY_INDEX = 2
        fun new(type: PsiType): HeapMarker {
            if (type == PsiTypes.nullType()) return NULL
            if (type == PsiTypes.voidType()) return VOID
            return HeapMarker(KEY_INDEX++, type)
        }
    }

    override val ind: ObjectSetIndicator = ObjectSetIndicator(type)
    override fun toString(): String {
        if (this == NULL) return "null"
        if (this == VOID) return "void"
        return "#$idx"
    }

    override fun safelyResolveUsing(context: Context): Expr<HeapMarker> = ConstExpr.fromHeapMarker(this)
}