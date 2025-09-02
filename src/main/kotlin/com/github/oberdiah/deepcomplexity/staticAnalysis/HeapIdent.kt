package com.github.oberdiah.deepcomplexity.staticAnalysis

@JvmInline
value class HeapIdent(val id: Int) {
    companion object {
        private var NEXT_ID = 0
        fun createNew(): HeapIdent = HeapIdent(NEXT_ID++)
    }

    override fun toString(): String = "H$id"
}