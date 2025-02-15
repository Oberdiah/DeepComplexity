package com.github.oberdiah.deepcomplexity.staticAnalysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntegerAffineTest {
    val key1 = Context.Key.EphemeralKey("key1")
    val key2 = Context.Key.EphemeralKey("key2")

    @Test
    fun `Ensure a constant gives the constant back`() {
        val integerAffine = IntegerAffine.fromConstant(5)

        val range = integerAffine.toRange()

        assertEquals(5, range.first)
        assertEquals(5, range.second)
    }

    @Test
    fun `Ensure a range gives the range back`() {
        val integerAffine = IntegerAffine.fromRange(5, 10, key1)

        val range = integerAffine.toRange()

        assertEquals(5, range.first)
        assertEquals(10, range.second)
    }

    @Test
    fun `Ensure basic adding works fine`() {
        for (i in 0..127) {
            for (j in 0..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong())
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong())

                val integerAffine3 = integerAffine1.add(integerAffine2)

                assertEquals((i + j).toLong(), integerAffine3.toRange().first)
            }
        }
    }

    @Test
    fun `Ensure basic subtraction works fine`() {
        for (i in 0..127) {
            for (j in 0..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong())
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong())
                val integerAffine3 = integerAffine1.subtract(integerAffine2)

                assertEquals((i - j).toLong(), integerAffine3.toRange().first)
            }
        }
    }

    @Test
    fun `Ensure basic multiplication works fine`() {
        for (i in 0..15) {
            for (j in 0..15) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong())
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong())
                val integerAffine3 = integerAffine1.multiply(integerAffine2)

                assertEquals((i * j).toLong(), integerAffine3.toRange().first)
            }
        }
    }

    @Test
    fun `Multiply range by constant`() {
        val affine1 = IntegerAffine.fromRange(5, 10, key1)
        val affine2 = IntegerAffine.fromConstant(3)

        val result = affine1.multiply(affine2).toRange()

        assertEquals(15, result.first)
        assertEquals(30, result.second)
    }
}
