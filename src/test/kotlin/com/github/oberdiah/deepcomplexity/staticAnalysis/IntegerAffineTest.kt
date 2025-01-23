package com.github.oberdiah.deepcomplexity.staticAnalysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntegerAffineTest {
    val key1 = Context.Key.EphemeralKey("key1")
    val key2 = Context.Key.EphemeralKey("key2")

    @Test
    fun `Ensure a constant gives the constant back`() {
        val integerAffine = IntegerAffine.fromConstant(5, 10)

        val range = integerAffine.toRange().first

        assertEquals(5, range.first)
        assertEquals(5, range.second)
    }

    @Test
    fun `Ensure a range gives the range back`() {
        val integerAffine = IntegerAffine.fromRange(5, 10, 15, key1)

        val range = integerAffine.toRange().first

        assertEquals(5, range.first)
        assertEquals(10, range.second)
    }

    @Test
    fun `Add constant to range and cause an upper wrap`() {
        val loopAt = 10L
        val affine1 = IntegerAffine.fromRange(5, 9, loopAt, key1)
        val affine2 = IntegerAffine.fromConstant(3, loopAt)

        val (result1, result2) = affine1.add(affine2).toRange()

        assertEquals(8, result1.first)
        assertEquals(9, result1.second)
        assertEquals(-10, result2!!.first)
        assertEquals(-8, result2.second)
    }

    @Test
    fun `Add constant to range and cause an lower wrap`() {
        val loopAt = 10L
        val affine1 = IntegerAffine.fromRange(-8, 4, loopAt, key1)
        val affine2 = IntegerAffine.fromConstant(-4, loopAt)

        val (result1, result2) = affine1.add(affine2).toRange()

        assertEquals(8, result1.first)
        assertEquals(9, result1.second)
        assertEquals(-10, result2!!.first)
        assertEquals(0, result2.second)
    }

    @Test
    fun `Subtract constant from range and cause an lower wrap`() {
        val loopAt = 10L
        val affine1 = IntegerAffine.fromRange(-8, 4, loopAt, key1)
        val affine2 = IntegerAffine.fromConstant(4, loopAt)

        val (result1, result2) = affine1.subtract(affine2).toRange()

        assertEquals(8, result1.first)
        assertEquals(9, result1.second)
        assertEquals(-10, result2!!.first)
        assertEquals(0, result2.second)
    }

    @Test
    fun `Add a big range to another big range and cause the final result to take the whole limit`() {
        val loopAt = 10L
        val affine1 = IntegerAffine.fromRange(-8, 6, loopAt, key1)
        val affine2 = IntegerAffine.fromRange(-8, 6, loopAt, key2)

        val (result1, result2) = affine1.add(affine2).toRange()

        assertEquals(-10, result1.first)
        assertEquals(9, result1.second)
        assertNull(result2)
    }

    @Test
    fun `Ensure basic adding works fine`() {
        for (i in 0..127) {
            for (j in 0..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), 50000)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), 50000)
                val integerAffine3 = integerAffine1.add(integerAffine2)

                assertEquals((i + j).toLong(), integerAffine3.toRange().first.first)
            }
        }
    }

    @Test
    fun `Ensure adding constants with wrapping works fine`() {
        for (i in -128..127) {
            for (j in -128..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), 128)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), 128)
                val integerAffine3 = integerAffine1.add(integerAffine2)

                val expected = (i.toByte() + j.toByte()).toByte().toLong()
                assertEquals(expected, integerAffine3.toRange().first.first, "i = $i, j = $j")
            }
        }
    }

    @Test
    fun `Ensure basic subtraction works fine`() {
        for (i in 0..127) {
            for (j in 0..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), 50000)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), 50000)
                val integerAffine3 = integerAffine1.subtract(integerAffine2)

                assertEquals((i - j).toLong(), integerAffine3.toRange().first.first)
            }
        }
    }

    @Test
    fun `Ensure subtracting constants with wrapping works fine`() {
        for (i in -128..127) {
            for (j in -128..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), 128)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), 128)
                val integerAffine3 = integerAffine1.subtract(integerAffine2)

                val expected = (i.toByte() - j.toByte()).toByte().toLong()
                assertEquals(expected, integerAffine3.toRange().first.first, "i = $i, j = $j")
            }
        }
    }
}