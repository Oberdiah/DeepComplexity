package com.github.oberdiah.deepcomplexity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5
import org.junit.jupiter.api.*
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SimpleMustPassTest : LightJavaCodeInsightFixtureTestCase5() {
    override fun getTestDataPath() = "src/test/java/testdata/"

    companion object {
        val summaryDescription = mutableListOf<String>()
    }

    @Test
    @Order(1)
    fun setup() {
    }

    @TestFactory
    @Order(2)
    fun runTests(): Collection<DynamicTest> {
        val outputFile: PsiFile = fixture.configureByFile("MyTestData.java")
        val app = ApplicationManager.getApplication()

        val methods = app.runReadAction<List<Pair<PsiMethod, String>>> {
            if (outputFile is PsiJavaFile) {
                outputFile.classes.flatMap { psiClass ->
                    psiClass.methods.map { psiMethod ->
                        psiMethod to psiMethod.name
                    }
                }
            } else {
                emptyList()
            }
        }

        val testToRun = System.getenv("TEST_FILTER")

        val methodsToRun = if (testToRun != null) {
            methods.filter { it.second.contains(testToRun) }
        } else {
            methods
        }

        val tests = mutableListOf<DynamicTest>()

        for (method in methodsToRun) {
            val testSourceUri = URI.create("method:testdata.MyTestData#" + method.second)

            tests.add(DynamicTest.dynamicTest(method.second, testSourceUri) {
                app.runReadAction {
                    val (msg, passed) = TestUtilities.testMethod(method.first, method.second)
                    summaryDescription.add("${method.second.padEnd(25)}: $msg")
                    if (!passed) {
                        throw AssertionError("Test ${method.second} failed.")
                    }
                }
            })
        }

        return tests
    }

    @Test
    @Order(3)
    fun summary() {
        println("###############################")
        println("##   Simple Must Pass Test   ##")
        println("###############################")
        println()
        println("Number of tests run: ${summaryDescription.size}")
        println()
        summaryDescription.forEach { println(it) }
    }
}
