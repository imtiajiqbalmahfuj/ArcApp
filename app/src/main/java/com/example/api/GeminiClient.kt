package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = 0.4f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = GenerationConfig()
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content? = null)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>? = null)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    private val CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GenerateContentRequest::class.java)
    private val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Executes a blocking/suspend call to generate text content using gemini-3.5-flash.
     */
    suspend fun generateContent(
        systemPrompt: String,
        screenContent: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "Error: Gemini API key is missing. Please configure your API key in the AI Studio Secrets panel."
        }

        val url = "$BASE_URL/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestPayload = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "User Screen Content:\n\n$screenContent")))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val jsonString = requestAdapter.toJson(requestPayload)
        val requestBody = jsonString.toRequestBody(CONTENT_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "API Technical Error: ${response.code} - ${response.message}"
                }
                val bodyText = response.body?.string() ?: ""
                val parsed = responseAdapter.fromJson(bodyText)
                parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Blank response output received."
            }
        } catch (e: Exception) {
            "Network Connection Failure: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Streams content chunk by chunk using Kotlin Flows.
     */
    fun generateContentStream(
        systemPrompt: String,
        screenContent: String
    ): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit("Error: Gemini API key is missing. Please configure your API key in the AI Studio Secrets panel.")
            return@flow
        }

        val url = "$BASE_URL/v1beta/models/gemini-3.5-flash:streamGenerateContent?key=$apiKey"

        val requestPayload = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "User Screen Content:\n\n$screenContent")))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val jsonString = requestAdapter.toJson(requestPayload)
        val requestBody = jsonString.toRequestBody(CONTENT_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        var response: Response? = null
        try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit("API Technical Error: ${response.code} - ${response.message}")
                return@flow
            }

            val source = response.body?.source() ?: throw IOException("Empty response body source")
            val reader = BufferedReader(source.inputStream().reader())
            var line: String?

            // Standard regex for extracting text entries from streaming content JSON chunks
            val textRegex = "\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.isNotBlank()) {
                    // Search for "text" field matches inside the streaming chunk to assemble tokens dynamically
                    val matches = textRegex.findAll(currentLine)
                    for (match in matches) {
                        var textChunk = match.groupValues[1]
                        // Simple unescape for quotes and newlines to keep text formatting gorgeous
                        textChunk = textChunk
                            .replace("\\n", "\n")
                            .replace("\\t", "\t")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        if (textChunk.isNotEmpty()) {
                            emit(textChunk)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Network Interruption: ${e.localizedMessage ?: "Connection reset"}]")
        } finally {
            response?.close()
        }
    }.flowOn(Dispatchers.IO)
}
