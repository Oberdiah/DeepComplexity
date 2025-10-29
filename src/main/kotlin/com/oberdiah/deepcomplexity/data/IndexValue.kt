package com.oberdiah.deepcomplexity.data

class IndexValue(val data: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndexValue

        return data == other.data
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}