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
) : Qualifier {
    companion object {
        private var KEY_INDEX = 1
        fun new(type: PsiType): HeapMarker = HeapMarker(KEY_INDEX++, type)
    }

    override fun addContextId(newId: Context.ContextId): Qualifier = this
    override val ind: ObjectSetIndicator = ObjectSetIndicator(type)
    override fun toString(): String = "#$idx"
    override fun isNew(): Boolean = true
    override fun safelyResolveUsing(context: Context): Expr<*> = ConstExpr.fromHeapMarker(this)
    override fun toLeafExpr(): LeafExpr<*> = ConstExpr.fromHeapMarker(this)
}