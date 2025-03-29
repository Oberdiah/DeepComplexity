package com.github.oberdiah.deepcomplexity.utilities

object Functional {
    /**
     * Merge two maps together, using the combiner to resolve conflicts.
     * If a key is in only one map, it is in the result anyway.
     */
    fun <K, V> mergeMapsUnion(map1: Map<K, V>, map2: Map<K, V>, combiner: (V, V) -> V): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for ((key, value) in map1) {
            result[key] = value
        }
        for ((key, value) in map2) {
            result[key] = result[key]?.let { combiner(it, value) } ?: value
        }
        return result
    }

    /**
     * Merge two maps together, using the combiner to resolve conflicts.
     * If a key is in only one map, it is not in the result.
     */
    fun <K, V> mergeMapsIntersection(map1: Map<K, V>, map2: Map<K, V>, combiner: (V, V) -> V): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for ((key, value) in map1) {
            if (map2.containsKey(key)) {
                result[key] = combiner(value, map2[key]!!)
            }
        }
        return result
    }
}