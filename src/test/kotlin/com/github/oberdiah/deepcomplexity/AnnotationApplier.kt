package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.TestUtilities.annotationInformation
import org.jetbrains.kotlin.backend.common.push

object AnnotationApplier {
    val ANNOTATIONS = listOf(
        "com.github.oberdiah.deepcomplexity.RequiredScore"
    )

    fun applyAnnotations() {
        val methodsByFile = annotationInformation.groupBy { it.filePath }
        methodsByFile.forEach { (filePath, methods) ->
            try {
                val path = java.nio.file.Paths.get(filePath)
                var content = java.nio.file.Files.readAllLines(path).toMutableList()

                // Ensure imports exist
                for (annotation in ANNOTATIONS) {
                    val importLine = "import ${annotation};"
                    val hasImport = content.any { it.trim() == importLine }
                    if (!hasImport) {
                        // Find package line and last import index
                        val packageIndex = content.indexOfFirst { it.trim().startsWith("package ") }
                        var insertIndex = packageIndex + 1
                        while (insertIndex < content.size && content[insertIndex].trim().startsWith("import ")) {
                            insertIndex++
                        }
                        // Insert a blank line if none between package and imports
                        if (insertIndex == packageIndex + 1) {
                            content.add(insertIndex, "")
                            insertIndex++
                        }
                        content.add(insertIndex, importLine)
                    }
                }

                // For each method in this file, add annotation if not already present
                methods.forEach { pm ->
                    val methodName = pm.methodName
                    // Find the line index of the method declaration
                    val idx = content.indexOfFirst { line ->
                        val trimmed = line.trim()
                        // Match public static ... methodName(
                        trimmed.startsWith("public ") && trimmed.contains(" ${methodName}(")
                    }
                    if (idx >= 0) {
                        // Check if the previous non-empty, non-comment line already has @RequiredScore
                        var lookback = idx - 1
                        var annotationsWeHave = mutableListOf<String>()
                        while (lookback >= 0) {
                            val t = content[lookback].trim()
                            if (t.isEmpty()) {
                                lookback--; continue
                            }
                            if (t.startsWith("//")) {
                                lookback--; continue
                            }
                            if (t.startsWith("@")) {
                                annotationsWeHave.push(t.takeWhile { it != '(' })
                            }
                            break
                        }
                        val indent = content[idx].takeWhile { it.isWhitespace() }

                        if (!annotationsWeHave.contains("@RequiredScore") && pm.scoreAchieved == 1.0) {
                            content.add(idx, "$indent@RequiredScore(1.0)")
                            println("Added @RequiredScore(1.0) to $filePath#$methodName")
                        }
                    } else {
                        println("Warning: Could not locate method declaration for $filePath#$methodName")
                    }
                }

                java.nio.file.Files.writeString(path, content.joinToString("\n"))
            } catch (e: Throwable) {
                println("Failed to update annotations in file '$filePath': ${e.message}")
                e.printStackTrace()
            }
        }
    }
}