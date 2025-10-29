package com.oberdiah.deepcomplexity

import com.oberdiah.deepcomplexity.utilities.BigFraction
import org.apache.commons.lang3.math.Fraction
import org.junit.jupiter.api.Test

object FractionTest {
    @Test
    fun testMyBigFraction() {
        val fraction1 = BigFraction.of(-10, 1)
        val fraction2 = BigFraction.of(-5, 1)

        assert(fraction1.compareTo(fraction2) < 0)
    }

    @Test
    fun testApacheLang3() {
        val fraction1 = Fraction.getFraction(-10, 1)
        val fraction2 = Fraction.getFraction(-5, 1)

        assert(fraction1.compareTo(fraction2) < 0)
    }
}