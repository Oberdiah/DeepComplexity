package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.staticAnalysis.Indicator

/**
 * Used to represent a variable that is looping.
 */
@ConsistentCopyVisibility
data class LoopKey<T : Any> private constructor(val key: MethodProcessingKey, override val ind: Indicator<T>) :
    EvaluationKey<T> {
    // Some form of index/distinguishing feature may be needed at some point to tell keys apart,
    // I've not quite figured that out yet. For now, we'll do without.
    companion object {
        fun new(key: MethodProcessingKey): LoopKey<*> = LoopKey(key, key.ind)
    }

    override fun toString(): String = "$$key"
}