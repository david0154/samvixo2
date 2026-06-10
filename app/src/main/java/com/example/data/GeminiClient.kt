package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Gemini API Request Models ---

data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

data class GeminiConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

// --- Gemini API Response Models ---

data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Gemini Retrofit Client ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Call the Gemini REST endpoint with a custom prompt and chat history.
     */
    suspend fun generateAiResponse(prompt: String, chatHistory: List<Pair<String, Boolean>>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Graceful handle for empty/placeholder keys in AI Studio template
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return generateSimulationResponse(prompt)
        }

        try {
            // Map the history pairs (messageText, isUser) into GeminiContent format
            val contents = mutableListOf<GeminiContent>()
            
            // Add conversation history
            chatHistory.takeLast(10).forEach { (text, isUser) ->
                contents.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }
            
            // Add latest prompt
            contents.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )

            val request = GeminiRequest(
                contents = contents,
                generationConfig = GeminiConfig(temperature = 0.7f),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = "You are Emerald, an intuitive, responsive on-demand AI Assistant chatbot residing inside the user's secure encrypted WhatsApp Connect communication app. You act friendly, helpful, concise, and provide clever insights regarding technology, offline mesh systems, cryptographic encryption, and daily routines."))
                )
            )

            val response = apiService.generateContent(apiKey, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I received an empty response. Please try rephrasing your message."
        } catch (e: Exception) {
            e.printStackTrace()
            return "Connection Error: ${e.localizedMessage}. Please double-check your network status and API key configuration in Google AI Studio."
        }
    }

    /**
     * Simulated offline smart responses as an exquisite fallback mechanism
     */
    private fun generateSimulationResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") -> {
                "Hello! I am Emerald AI, your embedded intelligence companion. 🌟\n\n*(Note: Live cloud mode requires setting a GEMINI_API_KEY in the Secrets Panel. Currently running offline simulated mode!)*"
            }
            lower.contains("mesh") || lower.contains("offline") || lower.contains("wifi") || lower.contains("bluetooth") -> {
                "Offline mesh messaging lets you connect without cell towers! It broadcasts a local beacon via SSID/Multicast, and establishes peer-to-peer TCP links between devices on the same grid. Excellent for emergencies, hiking, or remote security."
            }
            lower.contains("encrypt") || lower.contains("key") || lower.contains("aes") -> {
                "Your messages are secured using AES-256 (Advanced Encryption Standard) in CBC mode with a unique dynamic Key. To view or audit key handshakes, navigate to the 'Key Hub' tab!"
            }
            lower.contains("joke") -> {
                "Why do programmers prefer custom mesh networks?\n\nBecause they love to make connections without relying on a centralized authority! 😄"
            }
            else -> {
                "That's an interesting question! Emerald AI Assistant is fully operational.\n\nTo unlock advanced generative reasoning, please configure your `GEMINI_API_KEY` in the AI Studio Secrets panel. This will immediately enable Live Gemini 3.5-Flash capabilities!"
            }
        }
    }
}
