package com.github.oberdiah.deepcomplexity.evaluation

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

    override fun addContextId(newId: Context.ContextId): Qualifier = this
    override val ind: ObjectSetIndicator = ObjectSetIndicator(type)
    override fun toString(): String {
        if (this == NULL) return "null"
        if (this == VOID) return "void"
        return "#$idx"
    }

    override fun isNew(): Boolean = true
    override fun safelyResolveUsing(context: Context): Expr<*> = ConstExpr.fromHeapMarker(this)
    override fun toLeafExpr(): LeafExpr<*> = ConstExpr.fromHeapMarker(this)
}