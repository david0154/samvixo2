package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- Devil AI API Request / Response Models ---

data class OllamaRequest(
    @Json(name = "model") val model: String,
    @Json(name = "prompt") val prompt: String,
    @Json(name = "stream") val stream: Boolean
)

data class OllamaResponse(
    @Json(name = "response") val response: String
)

interface DevilAiApiService {
    @POST("api/generate")
    suspend fun chat(
        @Body request: OllamaRequest
    ): OllamaResponse
}

object DevilAiClient {
    private const val BASE_URL = "https://aiapi.devilpvt.in/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: DevilAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DevilAiApiService::class.java)
    }

    /**
     * Sends message to local Ollama host at https://aiapi.devilpvt.in
     */
    suspend fun generateAiResponse(userPrompt: String, model: String = "devil-ai"): String {
        return try {
            val response = apiService.chat(
                OllamaRequest(
                    model = model,
                    prompt = userPrompt,
                    stream = false
                )
            )
            response.response
        } catch (e: Exception) {
            e.printStackTrace()
            "Devil AI Status: Connection failed (${e.localizedMessage}). Please verify network or fallback server."
        }
    }
}
