package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.model.MessagesResponse
import com.hermes.mobile.core.model.SendPaymentRequest
import com.hermes.mobile.core.model.SendPaymentResponse
import com.hermes.mobile.core.model.SessionsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HermesRestClient @Inject constructor(
    private val client: OkHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json,
) {
    suspend fun checkHealth(serverUrl: String, apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(serverUrl.endpoint("health"))
                .applyBearer(apiKey)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Health check failed: HTTP ${response.code}")
            }
        }
    }

    suspend fun fetchSessions(limit: Int = 50, offset: Int = 0): Result<SessionsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = authenticatedRequest("v1/sessions?limit=$limit&offset=$offset")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Sessions failed: HTTP ${response.code}")
                json.decodeFromString<SessionsResponse>(response.body.string())
            }
        }
    }

    suspend fun fetchMessages(sessionId: String): Result<MessagesResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = authenticatedRequest("v1/sessions/$sessionId/messages")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Messages failed: HTTP ${response.code}")
                json.decodeFromString<MessagesResponse>(response.body.string())
            }
        }
    }

    suspend fun getText(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(authenticatedRequest(path)).execute().use { response ->
                if (!response.isSuccessful) error("GET /$path failed: HTTP ${response.code}")
                response.body.string()
            }
        }
    }

    suspend fun putText(path: String, body: String): Result<String> = sendText("PUT", path, body)

    suspend fun postText(path: String, body: String): Result<String> = sendText("POST", path, body)

    private suspend fun sendText(method: String, path: String, body: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = body.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(tokenStore.serverUrl.endpoint(path))
                .applyBearer(tokenStore.apiKey)
                .method(method, requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("$method /$path failed: HTTP ${response.code}")
                response.body.string()
            }
        }
    }

    private fun authenticatedRequest(path: String): Request {
        return Request.Builder()
            .url(tokenStore.serverUrl.endpoint(path))
            .applyBearer(tokenStore.apiKey)
            .get()
            .build()
    }
}

fun Request.Builder.applyBearer(apiKey: String): Request.Builder {
    if (apiKey.isNotBlank()) {
        header("Authorization", "Bearer $apiKey")
    }
    return this
}

fun String.endpoint(path: String): String {
    val base = trim().trimEnd('/')
    val cleanPath = path.trimStart('/')
    return "$base/$cleanPath"
}
