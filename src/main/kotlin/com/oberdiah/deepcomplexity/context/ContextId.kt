package com.oberdiah.deepcomplexity.context

@JvmInline
value class ContextId(private val ids: Set<Int>) {
    operator fun plus(other: ContextId): ContextId = ContextId(this.ids + other.ids)
    fun collidesWith(other: ContextId): Boolean = this.ids.any { it in other.ids }

    companion object {
        var ID_INDEX = 0
        fun new(): ContextId = ContextId(setOf(ID_INDEX++))
    }
}