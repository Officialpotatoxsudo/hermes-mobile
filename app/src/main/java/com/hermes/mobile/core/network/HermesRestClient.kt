package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.model.DashboardMessagesResponse
import com.hermes.mobile.core.model.DashboardModelInfoResponse
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.DashboardSessionsResponse
import com.hermes.mobile.core.model.MessagesResponse
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
            val request = authenticatedRequest("api/sessions?limit=$limit&offset=$offset")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val dashboard = json.decodeFromString<DashboardSessionsResponse>(response.body.string())
                    return@runCatching dashboard.toSessionsResponse()
                }
                if (response.code != 404) error("Sessions failed: HTTP ${response.code}")
            }

            val fallback = authenticatedRequest("v1/sessions?limit=$limit&offset=$offset")
            client.newCall(fallback).execute().use { response ->
                if (!response.isSuccessful) error("Sessions failed: HTTP ${response.code}")
                json.decodeFromString<SessionsResponse>(response.body.string())
            }
        }
    }

    suspend fun fetchModelOptions(): Result<DashboardModelOptionsResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = authenticatedRequest("api/model/options")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@runCatching json.decodeFromString<DashboardModelOptionsResponse>(response.body.string())
                }
            }
            val infoRequest = authenticatedRequest("api/model/info")
            client.newCall(infoRequest).execute().use { response ->
                if (!response.isSuccessful) error("Model options failed: HTTP ${response.code}")
                val info = json.decodeFromString<DashboardModelInfoResponse>(response.body.string())
                DashboardModelOptionsResponse(
                    providers = listOf(
                        com.hermes.mobile.core.model.DashboardProviderDto(
                            slug = info.provider,
                            name = info.provider,
                            isCurrent = true,
                            models = listOf(info.model).filter { it.isNotBlank() },
                        ),
                    ),
                    model = info.model,
                    provider = info.provider,
                )
            }
        }
    }

    suspend fun fetchMessages(sessionId: String): Result<MessagesResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = authenticatedRequest("api/sessions/$sessionId/messages")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val dashboard = json.decodeFromString<DashboardMessagesResponse>(response.body.string())
                    return@runCatching dashboard.toMessagesResponse()
                }
                if (response.code != 404) error("Messages failed: HTTP ${response.code}")
            }

            val fallback = authenticatedRequest("v1/sessions/$sessionId/messages")
            client.newCall(fallback).execute().use { response ->
                if (!response.isSuccessful) error("Messages failed: HTTP ${response.code}")
                json.decodeFromString<MessagesResponse>(response.body.string())
            }
        }
    }

    suspend fun getText(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = path.trimStart('/')
            if (cleanPath.startsWith("v1/") || cleanPath.startsWith("api/")) {
                client.newCall(authenticatedRequest(cleanPath)).execute().use { response ->
                    if (!response.isSuccessful) error("GET /$cleanPath failed: HTTP ${response.code}")
                    response.body.string()
                }
            } else {
                // Try api/ first
                val apiRequest = authenticatedRequest("api/$cleanPath")
                client.newCall(apiRequest).execute().use { response ->
                    if (response.isSuccessful) return@runCatching response.body.string()
                    if (response.code != 404) error("GET /api/$cleanPath failed: HTTP ${response.code}")
                }
                // Fallback to v1/
                val v1Request = authenticatedRequest("v1/$cleanPath")
                client.newCall(v1Request).execute().use { response ->
                    if (!response.isSuccessful) error("GET /v1/$cleanPath failed: HTTP ${response.code}")
                    response.body.string()
                }
            }
        }
    }

    suspend fun putText(path: String, body: String): Result<String> = sendText("PUT", path, body)

    suspend fun postText(path: String, body: String): Result<String> = sendText("POST", path, body)

    private suspend fun sendText(method: String, path: String, body: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = path.trimStart('/')
            val requestBody = body.toRequestBody("application/json; charset=utf-8".toMediaType())
            
            if (cleanPath.startsWith("v1/") || cleanPath.startsWith("api/")) {
                val request = Request.Builder()
                    .url(tokenStore.serverUrl.endpoint(cleanPath))
                    .applyBearer(tokenStore.apiKey)
                    .method(method, requestBody)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("$method /$cleanPath failed: HTTP ${response.code}")
                    response.body.string()
                }
            } else {
                // Try api/ first
                val apiRequest = Request.Builder()
                    .url(tokenStore.serverUrl.endpoint("api/$cleanPath"))
                    .applyBearer(tokenStore.apiKey)
                    .method(method, requestBody)
                    .build()
                client.newCall(apiRequest).execute().use { response ->
                    if (response.isSuccessful) return@runCatching response.body.string()
                    if (response.code != 404) error("$method /api/$cleanPath failed: HTTP ${response.code}")
                }
                // Fallback to v1/
                val v1Request = Request.Builder()
                    .url(tokenStore.serverUrl.endpoint("v1/$cleanPath"))
                    .applyBearer(tokenStore.apiKey)
                    .method(method, requestBody)
                    .build()
                client.newCall(v1Request).execute().use { response ->
                    if (!response.isSuccessful) error("$method /v1/$cleanPath failed: HTTP ${response.code}")
                    response.body.string()
                }
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
        header("x-hermes-session-token", apiKey)
    }
    return this
}

fun String.endpoint(path: String): String {
    val base = trim().trimEnd('/')
    val requestedPath = path.trimStart('/')
    val cleanPath = if (base.endsWith("/v1") && requestedPath.startsWith("v1/")) {
        requestedPath.removePrefix("v1/")
    } else {
        requestedPath
    }
    return "$base/$cleanPath"
}

private fun DashboardSessionsResponse.toSessionsResponse(): SessionsResponse {
    return SessionsResponse(
        sessions = sessions.map { session ->
            com.hermes.mobile.core.model.SessionDto(
                id = session.id,
                title = session.title ?: session.preview,
                source = session.source,
                startedAt = session.startedAt.asEpochMillis(),
                endedAt = session.endedAt.asEpochMillisOrNull(),
                messageCount = session.messageCount,
                model = session.model,
            )
        },
        total = total,
        hasMore = hasMore,
    )
}

private fun DashboardMessagesResponse.toMessagesResponse(): MessagesResponse {
    return MessagesResponse(
        messages = messages.mapNotNull { message ->
            val content = message.content ?: return@mapNotNull null
            com.hermes.mobile.core.model.SessionMessageDto(
                id = message.id,
                role = message.role,
                content = content,
                timestamp = message.timestamp.asEpochMillis(),
            )
        },
    )
}

private fun kotlinx.serialization.json.JsonElement?.asEpochMillis(): Long {
    return asEpochMillisOrNull() ?: System.currentTimeMillis()
}

private fun kotlinx.serialization.json.JsonElement?.asEpochMillisOrNull(): Long? {
    val primitive = this as? kotlinx.serialization.json.JsonPrimitive ?: return null
    val numeric = primitive.content.toDoubleOrNull()
    if (numeric != null) {
        return if (numeric < 10_000_000_000L) (numeric * 1000).toLong() else numeric.toLong()
    }
    return runCatching {
        java.time.Instant.parse(primitive.content).toEpochMilli()
    }.getOrNull()
}
