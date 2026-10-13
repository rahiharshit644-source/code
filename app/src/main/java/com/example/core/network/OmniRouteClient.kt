package com.example.core.network

import com.example.core.model.ModelOption
import com.example.core.model.OmniRouteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ConnectionDiagnostic(
    val success: Boolean,
    val statusCode: Int = 0,
    val latencyMs: Long = 0,
    val message: String = "",
    val detectedRoute: String? = null,
    val rateLimitRemaining: String? = null
)

class OmniRouteClient {

    private fun buildOkHttpClient(timeoutSeconds: Int): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
    }

    /**
     * Tests connectivity to the OmniRoute gateway and measures latency.
     */
    suspend fun testConnection(config: OmniRouteConfig): ConnectionDiagnostic = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val client = buildOkHttpClient(config.timeoutSeconds)

        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        // First try GET /v1/models or health
        val targetUrl = if (cleanBaseUrl.endsWith("/v1")) "$cleanBaseUrl/models" else "$cleanBaseUrl/v1/models"

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .get()

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey.trim()}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val remaining = response.header("x-ratelimit-remaining") ?: response.header("ratelimit-remaining")
                val activeRoute = response.header("x-route-selected") ?: response.header("x-provider-name")

                if (response.isSuccessful) {
                    ConnectionDiagnostic(
                        success = true,
                        statusCode = response.code,
                        latencyMs = latency,
                        message = "Connected successfully to OmniRoute gateway (${response.code} OK)",
                        detectedRoute = activeRoute,
                        rateLimitRemaining = remaining
                    )
                } else {
                    ConnectionDiagnostic(
                        success = false,
                        statusCode = response.code,
                        latencyMs = latency,
                        message = "Gateway returned error HTTP ${response.code}: ${response.message}"
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ConnectionDiagnostic(
                success = false,
                statusCode = -1,
                latencyMs = latency,
                message = "Connection failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Dynamically fetches list of available models from OmniRoute (/v1/models).
     */
    suspend fun fetchModels(config: OmniRouteConfig): Result<List<ModelOption>> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(config.timeoutSeconds)
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val targetUrl = if (cleanBaseUrl.endsWith("/v1")) "$cleanBaseUrl/models" else "$cleanBaseUrl/v1/models"

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .get()

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey.trim()}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code} while fetching models: ${response.message}")
                    )
                }
                val bodyString = response.body?.string() ?: ""
                val json = JSONObject(bodyString)
                val dataArray = json.optJSONArray("data") ?: JSONArray()

                val discovered = mutableListOf<ModelOption>()
                // Always prepend the dedicated OmniRoute auto presets
                discovered.add(
                    ModelOption(
                        id = "auto/coding",
                        name = "OmniRoute Auto Coding (Recommended)",
                        provider = "OmniRoute Gateway",
                        isFree = false,
                        isCodingSpecialized = true,
                        description = "Smart coding route. Actual provider and cost depend on your OmniRoute configuration."
                    )
                )
                discovered.add(
                    ModelOption(
                        id = "auto",
                        name = "OmniRoute Auto Router",
                        provider = "OmniRoute Gateway",
                        isFree = false,
                        isCodingSpecialized = false,
                        description = "Auto-scoring router. Actual provider and cost depend on your OmniRoute configuration."
                    )
                )

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    if (id.isNotBlank() && id != "auto/coding" && id != "auto") {
                        val ownedBy = item.optString("owned_by", "OmniRoute")
                        val isCoding = id.contains("coder", ignoreCase = true) || id.contains("code", ignoreCase = true)
                        // Unknown pricing must never be represented as free.
                        val pricing = item.optJSONObject("pricing")
                        val isFree = pricing?.optBoolean("is_free", false) ?: false
                        discovered.add(
                            ModelOption(
                                id = id,
                                name = id,
                                provider = ownedBy,
                                isFree = isFree,
                                isCodingSpecialized = isCoding,
                                description = "Provided via OmniRoute ($ownedBy)"
                            )
                        )
                    }
                }
                Result.success(discovered)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Routing presets only; the gateway determines which provider serves each request. */
    fun getFallbackModelPresets(): List<ModelOption> = listOf(
        ModelOption(
            id = "auto/coding",
            name = "OmniRoute Auto Coding",
            provider = "OmniRoute Gateway",
            isFree = false,
            isCodingSpecialized = true,
            description = "Coding-focused route. Provider availability and cost depend on gateway configuration."
        ),
        ModelOption(
            id = "auto",
            name = "OmniRoute Auto Router",
            provider = "OmniRoute Gateway",
            isFree = false,
            isCodingSpecialized = false,
            description = "Automatic routing. Provider availability and cost depend on gateway configuration."
        )
    )

    /**
     * Executes a chat completion request with streaming SSE.
     * Emits token chunks progressively.
     */
    fun streamChatCompletion(
        config: OmniRouteConfig,
        messages: List<Pair<String, String>>, // (role, content)
        targetModel: String = config.selectedModel
    ): Flow<String> = flow {
        val client = buildOkHttpClient(config.timeoutSeconds)
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val targetUrl = if (cleanBaseUrl.endsWith("/v1")) "$cleanBaseUrl/chat/completions" else "$cleanBaseUrl/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            put("stream", true)
            put("temperature", config.temperature)

            val messagesArray = JSONArray()
            for ((role, content) in messages) {
                val msgObj = JSONObject().apply {
                    put("role", role)
                    put("content", content)
                }
                messagesArray.put(msgObj)
            }
            put("messages", messagesArray)
        }

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey.trim()}")
        }

        var response = client.newCall(requestBuilder.build()).execute()

        // Handle auto-fallback if rate-limited or unavailable
        if (!response.isSuccessful && config.autoFallback && targetModel != config.fallbackModel) {
            response.close()
            // Retry with fallback model
            jsonBody.put("model", config.fallbackModel)
            val fallbackRequest = requestBuilder
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            response = client.newCall(fallbackRequest).execute()
        }

        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            response.close()
            throw Exception("OmniRoute API error (HTTP ${response.code}): $errBody")
        }

        val inputStream = response.body?.byteStream()
            ?: throw Exception("No response body stream received from OmniRoute.")

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val dataContent = trimmed.removePrefix("data:").trim()
                    if (dataContent == "[DONE]") {
                        break
                    }
                    try {
                        val chunkObj = JSONObject(dataContent)
                        val choices = chunkObj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val firstChoice = choices.getJSONObject(0)
                            val delta = firstChoice.optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (ignored: Exception) {
                        // Keep streaming on parse edge-cases
                    }
                }
                line = reader.readLine()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes non-streaming chat completion.
     */
    suspend fun chatCompletion(
        config: OmniRouteConfig,
        messages: List<Pair<String, String>>,
        targetModel: String = config.selectedModel
    ): Result<String> = withContext(Dispatchers.IO) {
        val client = buildOkHttpClient(config.timeoutSeconds)
        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val targetUrl = if (cleanBaseUrl.endsWith("/v1")) "$cleanBaseUrl/chat/completions" else "$cleanBaseUrl/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            put("stream", false)
            put("temperature", config.temperature)

            val messagesArray = JSONArray()
            for ((role, content) in messages) {
                val msgObj = JSONObject().apply {
                    put("role", role)
                    put("content", content)
                }
                messagesArray.put(msgObj)
            }
            put("messages", messagesArray)
        }

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey.trim()}")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    if (config.autoFallback && targetModel != config.fallbackModel) {
                        return@withContext chatCompletion(config, messages, config.fallbackModel)
                    }
                    return@withContext Result.failure(
                        Exception("OmniRoute error HTTP ${response.code}: ${response.body?.string()}")
                    )
                }
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content", "") ?: ""
                    Result.success(content)
                } else {
                    Result.failure(Exception("Empty choices in OmniRoute response"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
