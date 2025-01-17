package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.staticAnalysis.MethodProcessing
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory


class SimpleMustPassTest : LightJavaCodeInsightFixtureTestCase5() {
    override fun getTestDataPath() = "src/test/java/testdata/"

    @BeforeEach
    fun setUp() {

    }

    @Test
    fun simpleTest() {

    }

    @TestFactory
    fun dynamicTestsWithCollection(): Collection<DynamicTest> {
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

        val tests = mutableListOf<DynamicTest>()

        for (method in methods) {
            tests.add(DynamicTest.dynamicTest(method.second) {
                app.runReadAction {
                    val context = MethodProcessing.getMethodContext(method.first)
                    println(context.convertToString(false))
                }
            })
        }

        return tests
    }
}
