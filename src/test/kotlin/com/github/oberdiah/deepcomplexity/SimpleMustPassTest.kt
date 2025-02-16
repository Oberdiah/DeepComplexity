package com.github.oberdiah.deepcomplexity

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase5
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.net.URI


class SimpleMustPassTest : LightJavaCodeInsightFixtureTestCase5() {
    override fun getTestDataPath() = "src/test/java/testdata/"

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
                    TestUtilities.testMethod(method.first, method.second)
                }
            })
        }

        return tests
    }
}
