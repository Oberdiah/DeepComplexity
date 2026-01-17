package com.oberdiah.deepcomplexity

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

    class TestInfo(
        val testDisplayName: String,
        val psiMethod: PsiMethod,
        val className: String,
        val file: String,
        val testSettings: TestSettings
    )

    data class TestSettings(
        val cloneContexts: Boolean,
        val updateAnnotations: Boolean,
        val ignoreExpressionSize: Boolean
    )

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
            .map { it.invariantSeparatorsPath.replace(testDirectory, "") }

        // Run on all files in the AI directory, plus MyTestData.java
        val outputFilesPreFilter: List<PsiFile?> = fixture.configureByFiles(
            *allFiles.toList().toTypedArray()
        ).toList()

        val fileToRun = System.getenv("FILE_FILTER")
        val ignoreExpressionSize = System.getenv("IGNORE_EXPRESSION_SIZE") == "True"

        val outputFiles = if (fileToRun != null) {
            outputFilesPreFilter.filter { it?.name?.contains(fileToRun) == true }
        } else {
            outputFilesPreFilter.filterNotNull()
        }

        val app = ApplicationManager.getApplication()

        var methods = app.runReadAction<List<TestInfo>> {
            val list = mutableListOf<TestInfo>()
            for (file in outputFiles) {
                if (file is PsiJavaFile) {
                    list.addAll(file.classes.flatMap { psiClass ->
                        psiClass.methods
                            .filter { it.hasModifierProperty("public") }
                            .flatMap { psiMethod ->
                                val relativeFile = file.virtualFile.path
                                    .replace("/src/", "")
                                    .replace(".java", "")
                                    .replace("/", ".")

                                listOf(
                                    TestInfo(
                                        psiMethod.name,
                                        psiMethod,
                                        psiMethod.name,
                                        relativeFile,
                                        TestSettings(
                                            cloneContexts = false,
                                            updateAnnotations = true,
                                            ignoreExpressionSize = ignoreExpressionSize
                                        )
                                    ), TestInfo(
                                        psiMethod.name + " C.",
                                        psiMethod,
                                        psiMethod.name,
                                        relativeFile,
                                        TestSettings(
                                            cloneContexts = true,
                                            updateAnnotations = false,
                                            ignoreExpressionSize = ignoreExpressionSize
                                        )
                                    )
                                )
                            }
                    })
                }
            }
            list
        }

        val testToRun = System.getenv("TEST_FILTER")

        methods = if (testToRun != null) {
            methods.filter { it.className.contains(testToRun) }
        } else {
            methods
        }

        val ignoreClonedContextTests = System.getenv("IGNORE_CLONED_CONTEXTS") == "True"
        val ignoreNonClonedContextTests = System.getenv("IGNORE_NON_CLONED_CONTEXTS") == "True"
        methods = methods.filter {
            if (it.testSettings.cloneContexts) {
                !ignoreClonedContextTests
            } else {
                !ignoreNonClonedContextTests
            }
        }

        val tests = mutableListOf<DynamicTest>()

        for (method in methods) {
            val file = method.file
            val testSourceUri = URI.create("method:testdata.${file}#${method.className}")

            tests.add(DynamicTest.dynamicTest(method.testDisplayName, testSourceUri) {
                app.runReadAction {
                    val (msg, passed) = TestUtilities.testMethod(method)
                    summaryDescription.add("${method.testDisplayName.padEnd(32)}: $msg")
                    if (!passed) {
                        throw AssertionError("Test ${method.testDisplayName} failed.")
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

        if (System.getenv("UPDATE_ANNOTATIONS") == "True") {
            AnnotationApplier.applyAnnotations()
        } else {
            println("UPDATE_ANNOTATIONS is not 'True'. Skipping annotation updates.")
        }
    }
}
