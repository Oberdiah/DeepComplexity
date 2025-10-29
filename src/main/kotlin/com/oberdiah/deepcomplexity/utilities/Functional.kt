package com.github.oberdiah.deepcomplexity.utilities

object Functional {
    /**
     * Merge two maps together, using the combiner to resolve every value.
     * If a key is in only one map, the blank is used to generate a partner to run combiner on.
     */
    fun <K, V> mergeMapsWithBlank(
        map1: Map<K, V>,
        map2: Map<K, V>,
        blank: V,
        combiner: (V, V) -> V
    ): Map<K, V> {
        val combinedKeys = map1.keys + map2.keys
        val result = mutableMapOf<K, V>()
        for (key in combinedKeys) {
            val value1 = map1[key] ?: blank
            val value2 = map2[key] ?: blank
            result[key] = combiner(value1, value2)
        }
        return result
    }

    /**
     * Merge two maps together, using the combiner to resolve conflicts.
     * If a key is in only one map, it is in the result anyway.
     */
    fun <K, V> mergeMapsUnion(
        map1: Map<K, V>,
        map2: Map<K, V>,
        combiner: (V, V) -> V
    ): Map<K, V> {
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