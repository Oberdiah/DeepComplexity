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

    class TestInfo(val psiMethod: PsiMethod, val name: String, val file: String)

    @Test
    @Order(1)
    fun setup() {
    }

    @TestFactory
    @Order(2)
    fun runTests(): Collection<DynamicTest> {
        // Find all files in the testdata/ai directory using standard Java I/O
        val testDirectory = "src/test/java/testdata/"
        val allFiles = java.io.File(testDirectory).walk()
            .filter { it.isFile && it.extension == "java" }
            .map { it.path.replace("src\\test\\java\\testdata\\", "") }

        // Run on all files in the ai directory, plus MyTestData.java
        val outputFilesPreFilter: List<PsiFile?> = fixture.configureByFiles(
            *allFiles.toList().toTypedArray()
        ).toList()

        val fileToRun = System.getenv("FILE_FILTER")

        val outputFiles = if (fileToRun != null) {
            outputFilesPreFilter.filter { it?.name?.contains(fileToRun) == true }
        } else {
            outputFilesPreFilter.filterNotNull()
        }

        val app = ApplicationManager.getApplication()

        val methods = app.runReadAction<List<TestInfo>> {
            val list = mutableListOf<TestInfo>()
            for (file in outputFiles) {
                if (file is PsiJavaFile) {
                    list.addAll(file.classes.flatMap { psiClass ->
                        psiClass.methods
                            .filter { it.hasModifierProperty("public") }
                            .map { psiMethod ->
                                val relativeFile = file.virtualFile.path
                                    .replace("/src/", "")
                                    .replace(".java", "")
                                    .replace("/", ".")

                                TestInfo(
                                    psiMethod,
                                    psiMethod.name,
                                    relativeFile
                                )
                            }
                    })
                }
            }
            list
        }

        val testToRun = System.getenv("TEST_FILTER")

        val methodsToRun = if (testToRun != null) {
            methods.filter { it.name.contains(testToRun) }
        } else {
            methods
        }

        val tests = mutableListOf<DynamicTest>()

        for (method in methodsToRun) {
            val file = method.file
            val testSourceUri = URI.create("method:testdata.${file}#${method.name}")

            tests.add(DynamicTest.dynamicTest(method.name, testSourceUri) {
                app.runReadAction {
                    val (msg, passed) = TestUtilities.testMethod(method)
                    summaryDescription.add("${method.name.padEnd(25)}: $msg")
                    if (!passed) {
                        throw AssertionError("Test ${method.name} failed.")
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
