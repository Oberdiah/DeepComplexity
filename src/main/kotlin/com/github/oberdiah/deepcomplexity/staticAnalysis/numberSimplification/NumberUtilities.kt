package com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification

import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.NumberRange
import com.github.oberdiah.deepcomplexity.utilities.Utilities.compareTo
import com.github.oberdiah.deepcomplexity.utilities.Utilities.max

object NumberUtilities {
    fun <T : Number> mergeAndDeduplicate(ranges: List<NumberRange<T>>): List<NumberRange<T>> {
        if (ranges.size <= 1) {
            return ranges
        }

        val sortedRange: List<NumberRange<T>> = ranges.sortedWith { a, b -> a.start.compareTo(b.start) }

        val newRanges = mutableListOf<NumberRange<T>>()
        var currentRange = sortedRange[0]
        for (i in 1 until sortedRange.size) {
            val nextRange = sortedRange[i]
            if (currentRange.end >= nextRange.start) {
                currentRange = NumberRange.new(
                    currentRange.start,
                    nextRange.end.max(currentRange.end),
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