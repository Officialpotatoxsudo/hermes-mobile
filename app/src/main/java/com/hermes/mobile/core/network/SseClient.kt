package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.TokenUsage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    fun streamChat(request: ChatCompletionRequest): Flow<SseEvent> = callbackFlow {
        val parser = SsePayloadParser(json)
        val streamingClient = client.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
        val body = json.encodeToString(request).toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(tokenStore.serverUrl.endpoint("v1/chat/completions"))
            .applyBearer(tokenStore.apiKey)
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
}
