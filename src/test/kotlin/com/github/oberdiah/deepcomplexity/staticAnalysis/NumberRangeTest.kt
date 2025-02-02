package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.ByteSetIndicator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NumberRangeTest {
    val key1 = Context.Key.EphemeralKey("key1")
    val key2 = Context.Key.EphemeralKey("key2")

    @Test
    fun `Multiply range by range`() {
        val affine1 = NumberRange.fromRange(3, 7, ByteSetIndicator, key1)
        val affine2 = NumberRange.fromRange(1, 7, ByteSetIndicator, key2)

        val result = affine1.multiplication(affine2).first()

        assertEquals(3, result.start)
        assertEquals(49, result.end)
    }

    @Test
    fun `Multiply range by range 2`() {
        val affine1 = NumberRange.fromRange(2, 4, ByteSetIndicator, key1)
        val affine2 = NumberRange.fromRange(3, 5, ByteSetIndicator, key2)

        val result = affine1.multiplication(affine2).first()

        assertEquals(6, result.start)
        assertEquals(20, result.end)
    }
}