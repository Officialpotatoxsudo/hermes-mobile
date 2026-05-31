package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SseEvent {
    data class Opened(val sessionId: String?) : SseEvent
    data class Delta(val text: String) : SseEvent
    data class Tool(val progress: ToolProgress) : SseEvent
    data class Usage(val usage: TokenUsage) : SseEvent
    data object Done : SseEvent
}

@Singleton
class SseClient @Inject constructor(
    private val client: OkHttpClient,
    private val tokenStore: TokenStore,
    private val json: Json,
) {
    suspend fun completeChat(request: ChatCompletionRequest, sessionId: String? = null): List<SseEvent> {
        return withContext(Dispatchers.IO) {
            executeNonStreaming(request, sessionId, SsePayloadParser(json))
        }
    }

    fun streamChat(request: ChatCompletionRequest, sessionId: String? = null): Flow<SseEvent> = callbackFlow {
        val parser = SsePayloadParser(json)
        val streamingClient = client.newBuilder()
            .readTimeout(300, TimeUnit.SECONDS)
            .build()
        val body = json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(tokenStore.serverUrl.endpoint("v1/chat/completions"))
            .applyBearer(tokenStore.apiKey)
            .applyHermesSessionHeaders(sessionId, tokenStore.apiKey)
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val source = EventSources.createFactory(streamingClient).newEventSource(
            httpRequest,
            object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    trySend(SseEvent.Opened(response.header("x-hermes-session-id")))
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    when {
                        data == "[DONE]" -> {
                            trySend(SseEvent.Done)
                            close()
                        }
                        type == "hermes.tool.progress" -> parser.parseTool(data)?.let { trySend(SseEvent.Tool(it)) }
                        else -> parser.parseChunk(data)?.let { chunk ->
                            val delta = chunk.choices.firstOrNull()?.delta?.content
                            if (!delta.isNullOrEmpty()) trySend(SseEvent.Delta(delta))
                            chunk.usage?.let { trySend(SseEvent.Usage(it)) }
                        }
                    }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    val bodyText = response?.peekBody(16_384)?.string().orEmpty()
                    val fallback = parser.parseCompletion(bodyText)
                    if (response?.isSuccessful == true && fallback != null) {
                        trySend(SseEvent.Opened(response.header("x-hermes-session-id")))
                        val text = fallback.choices.firstOrNull()?.message?.content
                            ?: fallback.choices.firstOrNull()?.text
                        if (!text.isNullOrBlank()) trySend(SseEvent.Delta(text))
                        fallback.usage?.let { trySend(SseEvent.Usage(it)) }
                        trySend(SseEvent.Done)
                        close()
                        return
                    }
                    if (response?.isProxyLike() == true || t?.message?.contains("content-type", ignoreCase = true) == true) {
                        runCatching {
                            executeNonStreaming(request, sessionId, parser)
                        }.onSuccess { events ->
                            events.forEach { trySend(it) }
                            close()
                        }.onFailure { fallbackError ->
                            close(IOException(fallbackError.message ?: "Non-streaming fallback failed", fallbackError))
                        }
                        return
                    }

                    val cause = t ?: IOException("SSE failed: HTTP ${response?.code ?: "unknown"}")
                    val readable = when {
                        t != null && t.message?.contains("content-type", ignoreCase = true) == true -> {
                            if (bodyText.isNotBlank()) {
                                val contentType = response?.header("content-type").orEmpty()
                                "Expected text/event-stream, got ${contentType.ifBlank { "unknown content-type" }}. Response: ${parser.parseError(bodyText)}"
                            } else {
                                "Expected text/event-stream, got ${response?.header("content-type") ?: "unknown content-type"}."
                            }
                        }
                        response != null && !response.isSuccessful -> {
                            val msg = bodyText.takeIf { it.isNotBlank() }?.let(parser::parseError) ?: response.message
                            "Server error HTTP ${response.code}: $msg"
                        }
                        else -> cause.message ?: "Connection failed"
                    }
                    close(IOException(readable, cause))
                }
            },
        )

        awaitClose { source.cancel() }
    }

    private fun executeNonStreaming(
        request: ChatCompletionRequest,
        sessionId: String?,
        parser: SsePayloadParser,
    ): List<SseEvent> {
        val body = json.encodeToString(request.copy(stream = false)).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(tokenStore.serverUrl.endpoint("v1/chat/completions"))
            .applyBearer(tokenStore.apiKey)
            .applyHermesSessionHeaders(sessionId, tokenStore.apiKey)
            .post(body)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            val bodyText = response.body.string()
            if (!response.isSuccessful) {
                error("Server error HTTP ${response.code}: ${parser.parseError(bodyText)}")
            }
            val completion = parser.parseCompletion(bodyText) ?: error("Server returned unreadable response")
            val text = completion.choices.firstOrNull()?.message?.content
                ?: completion.choices.firstOrNull()?.text
                ?: ""
            return buildList {
                add(SseEvent.Opened(response.header("x-hermes-session-id")))
                if (text.isNotBlank()) add(SseEvent.Delta(text))
                completion.usage?.let { add(SseEvent.Usage(it)) }
                add(SseEvent.Done)
            }
        }
    }
}

private fun Response.isProxyLike(): Boolean {
    val headerText = headers.joinToString("\n") { "${it.first}: ${it.second}" }.lowercase()
    return "ngrok" in headerText ||
        "cloudflare" in headerText ||
        "cf-ray" in headerText ||
        "cloudfront" in headerText ||
        "x-vercel" in headerText ||
        "fly-request-id" in headerText
}

fun Request.Builder.applyHermesSessionHeaders(sessionId: String?, apiKey: String): Request.Builder {
    val cleanSessionId = sessionId?.safeHeaderLine(maxLength = 512).orEmpty()
    val cleanApiKey = apiKey.safeHeaderLine()
    if (cleanSessionId.isBlank()) return this
    header("X-Hermes-Session-Id", cleanSessionId)
    if (cleanApiKey.isNotBlank()) {
        header("X-Hermes-Session-Key", "mobile:${cleanSessionId.take(240)}")
    }
    return this
}
