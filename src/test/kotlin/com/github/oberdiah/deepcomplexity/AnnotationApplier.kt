package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.TestUtilities.annotationInformation
import java.nio.file.Files
import java.nio.file.Paths

object AnnotationApplier {
    val ANNOTATIONS = listOf(
        "com.github.oberdiah.deepcomplexity.RequiredScore",
        "com.github.oberdiah.deepcomplexity.ExpectedExpressionSize"
    )

    private fun updateAnnotationIfNeeded(
        content: MutableList<String>,
        methodIndex: Int,
        annotationName: String,
        newValue: Number,
        filePath: String,
        methodName: String,
        // New, Existing -> Should we update?
        shouldUpdate: (Double, Double?) -> Boolean
    ) {
        // Regex to capture the value from annotations like @Name(1.0) or @Name(value = 1.0) or @Name(1)
        val valueRegex = "${annotationName}\\((?:value\\s*=\\s*)?([0-9.]+)\\)".toRegex()
        val newScoreAsDouble = newValue.toDouble()

        var existingAnnotationIndex = -1
        var existingScore: Double? = null

        // Look backward from the method definition to find an existing annotation
        var lookback = methodIndex - 1
        while (lookback >= 0) {
            val trimmedLine = content[lookback].trim()

            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("//") || trimmedLine.startsWith("/*") || trimmedLine.endsWith(
                    "*/"
                )
            ) {
                lookback--
                continue
            }

            // Check if this is the annotation we care about
            if (trimmedLine.startsWith(annotationName)) {
                existingAnnotationIndex = lookback
                // Try to parse the score from it
                valueRegex.find(trimmedLine)?.let { match ->
                    existingScore = match.groupValues[1].toDoubleOrNull()
                }
                break // Found it, stop looking
            }

            // If it's another annotation, keep looking up
            if (trimmedLine.startsWith("@")) {
                lookback--
                continue
            }

            // If we hit code (not an annotation, comment, or empty line), stop
            break
        }

        val indent = content[methodIndex].takeWhile { it.isWhitespace() }
        val annotationString =
            "$indent$annotationName($newValue)"

        if (shouldUpdate(newScoreAsDouble, existingScore)) {
            if (existingAnnotationIndex != -1) {
                // --- Update existing annotation ---
                content[existingAnnotationIndex] = annotationString
                println("Updated $annotationName from $existingScore to $newValue for $filePath#$methodName")
            } else {
                // --- Add new annotation ---
                content.add(methodIndex, annotationString)
                println("Added $annotationString to $filePath#$methodName")
            }
        }
    }

    fun applyAnnotations() {
        val methodsByFile = annotationInformation.groupBy { it.filePath }
        methodsByFile.forEach { (filePath, methods) ->
            try {
                val path = Paths.get(filePath)
                var content = Files.readAllLines(path).toMutableList()

                // --- Ensure imports exist ---
                for (annotation in ANNOTATIONS) {
                    val importLine = "import $annotation;"
                    val hasImport = content.any { it.trim() == importLine }
                    if (!hasImport) {
                        val packageIndex = content.indexOfFirst { it.trim().startsWith("package ") }
                        var insertIndex = if (packageIndex != -1) packageIndex + 1 else 0

                        // Find last import or first non-empty line
                        while (insertIndex < content.size && (content[insertIndex].trim()
                                .startsWith("import ") || content[insertIndex].trim().isEmpty())
                        ) {
                            insertIndex++
                        }

                        // Find the first non-empty/non-import line after package
                        val firstCodeLineIndex = content.drop(packageIndex + 1).indexOfFirst {
                            !it.trim().startsWith("import ") && it.trim().isNotEmpty()
                        }

                        var finalInsertIndex =
                            if (firstCodeLineIndex != -1) (packageIndex + 1 + firstCodeLineIndex) else insertIndex

                        // If we are inserting right after the package, add a blank line
                        if (finalInsertIndex == packageIndex + 1 && packageIndex != -1) {
                            content.add(finalInsertIndex, "")
                            finalInsertIndex++
                        }

                        content.add(finalInsertIndex, importLine)

                        // Add a blank line after the new import if needed
                        if (finalInsertIndex < content.size - 1 && content[finalInsertIndex + 1].trim()
                                .isNotEmpty() && !content[finalInsertIndex + 1].trim().startsWith("import ")
                        ) {
                            content.add(finalInsertIndex + 1, "")
                        }
                    }
                }

                // First, find all method indices based on the *original* content
                val methodsWithIndices = methods.map { pm ->
                    val methodName = pm.methodName
                    val idx = content.indexOfFirst { line ->
                        val trimmed = line.trim()
                        // Match lines like: public ..., private ..., protected ..., or default package ... methodName(
                        (trimmed.startsWith("public ") || trimmed.startsWith("private ") || trimmed.startsWith("protected ") ||
                                !trimmed.startsWith("@")) &&
                                trimmed.contains(" ${methodName}(")
                    }
                    pm to idx
                }

                // Process methods in REVERSE line order to avoid index shifting
                methodsWithIndices.filter { it.second >= 0 }.sortedByDescending { it.second }.forEach { (pm, idx) ->
                    val methodName = pm.methodName

                    updateAnnotationIfNeeded(
                        content = content,
                        methodIndex = idx,
                        annotationName = "@ExpectedExpressionSize",
                        newValue = pm.expressionSize,
                        filePath = filePath,
                        methodName = methodName,
                        shouldUpdate = { new, existing -> new > 0.0 && (existing == null || new < existing) }
                    )
                    if (pm.scoreAchieved == 1.0) {
                        updateAnnotationIfNeeded(
                            content = content,
                            methodIndex = idx,
                            annotationName = "@RequiredScore",
                            newValue = pm.scoreAchieved,
                            filePath = filePath,
                            methodName = methodName,
                            shouldUpdate = { new, existing -> new > (existing ?: 0.0) }
                        )
                    }
                }

                // Log warnings for methods not found
                methodsWithIndices.filter { it.second < 0 }.forEach { (pm, idx) ->
                    println("Warning: Could not locate method declaration for $filePath#${pm.methodName}")
                }

                Files.writeString(path, content.joinToString("\n"))
            } catch (e: Throwable) {
                println("Failed to update annotations in file '$filePath': ${e.message}")
                e.printStackTrace()
            }
        }
    }
}