package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.model.DashboardMessagesResponse
import com.hermes.mobile.core.model.DashboardModelInfoResponse
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.DashboardSessionsResponse
import com.hermes.mobile.core.model.MessagesResponse
import com.hermes.mobile.core.model.OpenAiModelsResponse
import com.hermes.mobile.core.model.SessionsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
                if (response.isSuccessful) {
                    val info = json.decodeFromString<DashboardModelInfoResponse>(response.body.string())
                    return@runCatching DashboardModelOptionsResponse(
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
                if (response.code != 404) error("Model options failed: HTTP ${response.code}")
            }

            val modelsRequest = authenticatedRequest("v1/models")
            client.newCall(modelsRequest).execute().use { response ->
                if (!response.isSuccessful) error("Model options failed: HTTP ${response.code}")
                val models = json.decodeFromString<OpenAiModelsResponse>(response.body.string())
                val ids = models.data.map { it.id }.filter { it.isNotBlank() }.ifEmpty { listOf("hermes-agent") }
                DashboardModelOptionsResponse(
                    providers = listOf(
                        com.hermes.mobile.core.model.DashboardProviderDto(
                            slug = "hermes",
                            name = "Hermes",
                            isCurrent = true,
                            models = ids,
                        ),
                    ),
                    model = ids.first(),
                    provider = "hermes",
                )
            }
        }
    }

    suspend fun fetchMessages(sessionId: String): Result<MessagesResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val request = authenticatedRequest(sessionMessagesPath("api", sessionId))
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val dashboard = json.decodeFromString<DashboardMessagesResponse>(response.body.string())
                    return@runCatching dashboard.toMessagesResponse()
                }
                if (response.code != 404) error("Messages failed: HTTP ${response.code}")
            }

            val fallback = authenticatedRequest(sessionMessagesPath("v1", sessionId))
            client.newCall(fallback).execute().use { response ->
                if (!response.isSuccessful) error("Messages failed: HTTP ${response.code}")
                json.decodeFromString<MessagesResponse>(response.body.string())
            }
        }
    }

    suspend fun pushMessage(sessionId: String, role: String, content: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val messageBody = json.encodeToString(
                buildJsonObject {
                    put("role", role)
                    put("content", content)
                    put("timestamp", timestamp)
                }
            )
            val paths = listOf(
                "api/sessions/${sessionId.encodedPathSegment()}/messages",
                "v1/sessions/${sessionId.encodedPathSegment()}/messages",
            )
            var lastError: String? = null
            paths.forEach { path ->
                val request = Request.Builder()
                    .url(tokenStore.serverUrl.endpoint(path))
                    .applyBearer(tokenStore.apiKey)
                    .post(messageBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@runCatching Unit
                    if (response.code == 404) {
                        lastError = "Endpoint not found: $path"
                    } else {
                        error("Push message failed: HTTP ${response.code}")
                    }
                }
            }
            error(lastError ?: "Push message failed")
        }
    }

    suspend fun getText(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = hermesRequestPath(path)
            executeRequest("GET", cleanPath, null)
        }
    }

    suspend fun putText(path: String, body: String): Result<String> = sendText("PUT", path, body)

    suspend fun postText(path: String, body: String): Result<String> = sendText("POST", path, body)

    suspend fun deleteText(path: String): Result<String> = sendText("DELETE", path, null)

    private suspend fun sendText(method: String, path: String, body: String?): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = hermesRequestPath(path)
            executeRequest(method, cleanPath, body)
        }
    }

    private fun executeRequest(method: String, cleanPath: String, body: String?): String {
        val paths = when {
            cleanPath.startsWith("v1/") || cleanPath.startsWith("api/") || cleanPath in directRootPaths -> listOf(cleanPath)
            else -> listOf("api/$cleanPath", "v1/$cleanPath")
        }
        var notFound: String? = null
        paths.forEachIndexed { index, path ->
            val request = Request.Builder()
                .url(tokenStore.serverUrl.endpoint(path))
                .applyBearer(tokenStore.apiKey)
                .method(method, body?.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) return response.body.string()
                val message = "$method /$path failed: HTTP ${response.code}"
                if (response.code == 404 && index < paths.lastIndex) {
                    notFound = message
                } else {
                    error(message)
                }
            }
        }
        error(notFound ?: "$method /$cleanPath failed")
    }

    private fun authenticatedRequest(path: String): Request {
        if (path in directRootPaths) {
            return Request.Builder()
                .url(tokenStore.serverUrl.endpoint(path))
                .applyBearer(tokenStore.apiKey)
                .get()
                .build()
        }
        return Request.Builder()
            .url(tokenStore.serverUrl.endpoint(path))
            .applyBearer(tokenStore.apiKey)
            .get()
            .build()
    }
}

private val directRootPaths = setOf("health", "health/detailed")

fun Request.Builder.applyBearer(apiKey: String): Request.Builder {
    val cleanApiKey = apiKey.safeHeaderLine()
    if (cleanApiKey.isNotBlank()) {
        header("Authorization", "Bearer $cleanApiKey")
    }
    return this
}

internal fun String.safeHeaderLine(maxLength: Int = Int.MAX_VALUE): String {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
        .filter { it >= ' ' && it != '\u007F' }
        .take(maxLength)
        .trim()
}

fun String.endpoint(path: String): String {
    val base = trim()
        .substringBefore("#")
        .substringBefore("?")
        .trimEnd('/')
    val requestedPath = path.trim().trimStart('/')
    val baseSegment = base.substringAfterLast("/")
    val cleanPath = if (baseSegment in apiBaseSegments && requestedPath.startsWith("$baseSegment/")) {
        requestedPath.removePrefix("$baseSegment/")
    } else {
        requestedPath
    }
    return "$base/$cleanPath"
}

private val apiBaseSegments = setOf("api", "v1")

internal fun hermesRequestPath(path: String): String {
    return path.cleanPathLine().trimStart('/').ifBlank { error("Hermes request path required") }
}

internal fun sessionMessagesPath(root: String, sessionId: String): String {
    val cleanRoot = root.cleanPathLine().trim('/').ifBlank { error("Session messages root required") }
    val cleanSessionId = sessionId.cleanPathLine().ifBlank { error("Session id required") }.encodedPathSegment()
    return "$cleanRoot/sessions/$cleanSessionId/messages"
}

private fun String.cleanPathLine(): String {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
}

private fun String.encodedPathSegment(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
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
