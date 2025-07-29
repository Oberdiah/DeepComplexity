package com.github.oberdiah.deepcomplexity

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object AITestDataGenerator {
    private data class Message(val role: String, val content: String)
    private data class RequestPayload(
        val model: String,
        @SerializedName("max_tokens") val maxTokens: Int,
        val messages: List<Message>,
        val system: String? = null,
    )

    private data class ContentItem(val type: String, val text: String)
    private data class Usage(
        @SerializedName("input_tokens") val inputTokens: Int,
        @SerializedName("cache_creation_input_tokens") val cacheCreationInputTokens: Int,
        @SerializedName("cache_read_input_tokens") val cacheReadInputTokens: Int,
        @SerializedName("output_tokens") val outputTokens: Int,
        @SerializedName("service_tier") val serviceTier: String
    )

    private data class ResponsePayload(
        val id: String,
        val type: String,
        val role: String,
        val model: String,
        val content: List<ContentItem>,
        @SerializedName("stop_reason") val stopReason: String,
        @SerializedName("stop_sequence") val stopSequence: String?,
        val usage: Usage
    )

    lateinit var apiKey: String

    @JvmStatic
    fun main(args: Array<String>) {
        apiKey = System.getenv("ANTHROPIC_API_KEY")

        generateTestData()
    }

    fun generateTestData() {
        val promptPath = "src/test/testData/system_prompt.txt"
        val systemPrompt = java.io.File(promptPath).takeIf { it.exists() }
            ?.readText()
            ?: throw IllegalStateException("System prompt file not found at $promptPath")

        val descriptionsList = listOf(
            ""
//            "Variable tracking through arithmetic, ensuring that x - x = 0, 2x - x = x, etc.",
//            "Short arithmetic specifically on wrapping behaviour",
//            "Long chained if statements with complex conditions and nested logic",
//            "Early return statements and how that interacts with method calls",
//            "Methods with many parameters, and the order of parameter evaluation",
//            "Methods with side effects, especially when parameters are mutable",
//            "Operation evaluation order, especially with short-circuiting logic",
//            "Many variable declarations and tracking their values between them",
//            "Tracking variable constraints in if statements forcing the values of later checks",
//            "Class methods, fields, and modification by reference",
        )

        for (testDescription in descriptionsList) {
            val instruction = "20 Tests focused on: $testDescription"

            val testName = doRequest(
                "You are an AI assistant that writes good Java class names for test classes." +
                        "You should only return the name of the class, without any additional text.",
                "Generate a concise, descriptive name for a test class that will contain tests for: $testDescription",
                maxTokens = 100
            )

            val generatedMethods = doRequest(
                systemPrompt,
                instruction,
                maxTokens = 7500
            )

            val methods = generatedMethods.lines()
                .filter { !it.startsWith("// Expected") && !it.contains("```") }
                .map { it.replace("Test(short x", "(short x") }
                .joinToString("\n") { "\t$it" }

            val testDataFile = "src/test/java/testdata/ai/${
                testName.replace(".java", "")
            }.java"

            val file = java.io.File(testDataFile)
            if (file.exists()) {
                println("File already exists: $testDataFile. Aborting.")
            } else {
                file.writeText(
                    """
package testdata.ai;

public class $testName {
    $methods
}"""
                )
                println("Test data written to: $testDataFile")
            }
        }
    }

    private fun doRequest(systemPrompt: String, instruction: String, maxTokens: Int): String {
        val payload = RequestPayload(
            model = "claude-sonnet-4-20250514",
            maxTokens = 7500,
            system = systemPrompt,
            messages = listOf(
                Message("user", instruction)
            )
        )

        println("Sending request: [$instruction] ($maxTokens tokens)")

        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.anthropic.com/v1/messages"))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(Gson().toJson(payload)))
            .build()

        val response = HttpClient.newBuilder()
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString())

        val responseData = Gson().fromJson(response.body(), ResponsePayload::class.java)

        println("Response received: ${responseData.usage.outputTokens} output tokens")

        return responseData.content.firstOrNull()?.text ?: "No content"
    }
}