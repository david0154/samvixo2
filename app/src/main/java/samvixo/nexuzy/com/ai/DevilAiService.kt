package samvixo.nexuzy.com.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import samvixo.nexuzy.com.utils.AppConstants
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Devil AI Service — wraps the Ollama-based Devil AI API
 * Endpoint: https://aiapi.devilpvt.in/api/generate
 */
object DevilAiService {

    data class AiRequest(
        val model: String = AppConstants.AI_MODEL_PRIMARY,
        val prompt: String,
        val stream: Boolean = false
    )

    data class AiResponse(
        val response: String,
        val isError: Boolean = false
    )

    /**
     * Send a prompt to Devil AI and return the response.
     * Falls back to gemma3:4b if primary model fails.
     */
    suspend fun chat(prompt: String, model: String = AppConstants.AI_MODEL_PRIMARY): AiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val result = callApi(prompt, model)
                if (result.isError && model == AppConstants.AI_MODEL_PRIMARY) {
                    // Fallback to gemma3:4b
                    callApi(prompt, AppConstants.AI_MODEL_FALLBACK)
                } else result
            } catch (e: Exception) {
                AiResponse(response = "Sorry, I couldn't connect to Devil AI. Please try again.", isError = true)
            }
        }
    }

    private fun callApi(prompt: String, model: String): AiResponse {
        val url = URL(AppConstants.AI_BASE_URL + AppConstants.AI_ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000

            val body = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("stream", false)
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(body) }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(responseText)
                AiResponse(response = json.optString("response", "No response from AI."))
            } else {
                AiResponse(response = "API error: ${connection.responseCode}", isError = true)
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Get available models from Devil AI server
     */
    suspend fun getAvailableModels(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(AppConstants.AI_BASE_URL + AppConstants.AI_TAGS_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                val text = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val models = json.optJSONArray("models") ?: return@withContext emptyList()
                (0 until models.length()).map {
                    models.getJSONObject(it).optString("name", "")
                }.filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
