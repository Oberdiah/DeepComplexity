package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.max

object NumberUtilities {
    fun <T : Number> mergeAndDeduplicate(ranges: List<Pair<T, T>>): List<Pair<T, T>> {
        if (ranges.size <= 1) {
            return ranges
        }

        val sortedRange: List<Pair<T, T>> = ranges.sortedWith { a, b -> a.first.compareTo(b.first) }

        val newRanges = mutableListOf<Pair<T, T>>()
        var currentRange = sortedRange[0]
        for (i in 1 until sortedRange.size) {
            val nextRange = sortedRange[i]
            if (currentRange.second >= nextRange.first) {
                currentRange = Pair(
                    currentRange.first,
                    nextRange.second.max(currentRange.second)
                )
            } else {
                newRanges.add(currentRange)
                currentRange = nextRange
            }
        }
        newRanges.add(currentRange)

        return newRanges
    }
}