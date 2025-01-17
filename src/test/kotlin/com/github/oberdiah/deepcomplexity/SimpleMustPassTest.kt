package com.github.oberdiah.deepcomplexity

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import testdata.MyTestData

class SimpleMustPassTest : LightJavaCodeInsightFixtureTestCase5() {
    override fun getTestDataPath() = "src/test/java/testdata/"

    @BeforeEach
    fun setUp() {

    }

    @Test
    fun simpleTest() {
        val outputFile = fixture.configureByFile("MyTestData.java")
        MyTestData.test1(1)

        println(outputFile)
    }
}
