package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.ByteSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.LongSetIndicator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntegerAffineTest {
    val key1 = Context.Key.EphemeralKey("key1")
    val key2 = Context.Key.EphemeralKey("key2")

    @Test
    fun `Ensure a constant gives the constant back`() {
        val integerAffine = IntegerAffine.fromConstant(5, ByteSetIndicator)

        val range = integerAffine.toRange()

        assertEquals(5, range.first)
        assertEquals(5, range.second)
    }

    @Test
    fun `Ensure a range gives the range back`() {
        val integerAffine = IntegerAffine.fromRange(5, 10, ByteSetIndicator, key1)

        val range = integerAffine.toRange()

        assertEquals(5, range.first)
        assertEquals(10, range.second)
    }

    @Test
    fun `Add constant to range and cause an upper wrap`() {
        val affine1 = IntegerAffine.fromRange(5, 127, ByteSetIndicator, key1)
        val affine2 = IntegerAffine.fromConstant(3, ByteSetIndicator)

        val (resultA, resultB) = affine1.add(affine2)
        val result1 = resultA.toRange()
        val result2 = resultB?.toRange()

        assertEquals(8, result1.first)
        assertEquals(127, result1.second)
        assertEquals(-128, result2!!.first)
        assertEquals(-126, result2.second)
    }

    @Test
    fun `Add a big range to another big range and cause the final result to take the whole limit`() {
        val affine1 = IntegerAffine.fromRange(-126, 120, ByteSetIndicator, key1)
        val affine2 = IntegerAffine.fromRange(-126, 121, ByteSetIndicator, key2)

        val (resultA, resultB) = affine1.add(affine2)
        val result1 = resultA.toRange()
        val result2 = resultB?.toRange()

        assertEquals(-128, result1.first)
        assertEquals(127, result1.second)
        assertNull(result2)
    }

    @Test
    fun `Ensure basic adding works fine`() {
        for (i in 0..127) {
            for (j in 0..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), LongSetIndicator)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), LongSetIndicator)

                val integerAffine3 = integerAffine1.add(integerAffine2)

                assertEquals((i + j).toLong(), integerAffine3.first.toRange().first)
            }
        }
    }

    @Test
    fun `Ensure adding constants with wrapping works fine`() {
        for (i in -128..127) {
            for (j in -128..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), ByteSetIndicator)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), ByteSetIndicator)
                val integerAffine3 = integerAffine1.add(integerAffine2)

                val expected = (i.toByte() + j.toByte()).toByte().toLong()
                assertEquals(expected, integerAffine3.first.toRange().first, "i = $i, j = $j")
            }
        }
    }

    @Test
    fun `Ensure basic subtraction works fine`() {
        for (i in 0..127) {
            for (j in 0..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), LongSetIndicator)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), LongSetIndicator)
                val integerAffine3 = integerAffine1.subtract(integerAffine2)

                assertEquals((i - j).toLong(), integerAffine3.first.toRange().first)
            }
        }
    }

    @Test
    fun `Ensure subtracting constants with wrapping works fine`() {
        for (i in -128..127) {
            for (j in -128..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), ByteSetIndicator)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), ByteSetIndicator)
                val integerAffine3 = integerAffine1.subtract(integerAffine2)

                val expected = (i.toByte() - j.toByte()).toByte().toLong()
                assertEquals(expected, integerAffine3.first.toRange().first, "i = $i, j = $j")
            }
        }
    }

    @Test
    fun `Ensure basic multiplication works fine`() {
        for (i in 0..15) {
            for (j in 0..15) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), LongSetIndicator)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), LongSetIndicator)
                val integerAffine3 = integerAffine1.multiply(integerAffine2)

                assertEquals((i * j).toLong(), integerAffine3.first.toRange().first)
            }
        }
    }

    @Test
    fun `Ensure multiplying constants with wrapping works fine`() {
        for (i in -128..127) {
            for (j in -128..127) {
                val integerAffine1 = IntegerAffine.fromConstant(i.toLong(), ByteSetIndicator)
                val integerAffine2 = IntegerAffine.fromConstant(j.toLong(), ByteSetIndicator)
                val integerAffine3 = integerAffine1.multiply(integerAffine2)

                val expected = (i.toByte() * j.toByte()).toByte().toLong()
                assertEquals(expected, integerAffine3.first.toRange().first, "i = $i, j = $j")
            }
        }
    }

    @Test
    fun `Multiply range by constant`() {
        val affine1 = IntegerAffine.fromRange(5, 10, ByteSetIndicator, key1)
        val affine2 = IntegerAffine.fromConstant(3, ByteSetIndicator)

        val result = affine1.multiply(affine2).first.toRange()

        assertEquals(15, result.first)
        assertEquals(30, result.second)
    }

    @Test
    fun `Multiply ranges that cause full wrapping`() {
        val affine1 = IntegerAffine.fromRange(100, 120, ByteSetIndicator, key1)
        val affine2 = IntegerAffine.fromRange(2, 4, ByteSetIndicator, key2)

        val (resultA, resultB) = affine1.multiply(affine2)
        val result1 = resultA.toRange()
        val result2 = resultB?.toRange()

        assertEquals(result1.first, -128L)
        assertEquals(result1.second, 127L)
        assertNull(result2)
    }
}
