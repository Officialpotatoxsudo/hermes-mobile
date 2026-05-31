package com.hermes.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
    val reasoning: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)

@Serializable
data class ChatRequestMessageDto(
    val role: String,
    val content: JsonElement,
)

@Serializable
data class ChatCompletionRequest(
    val model: String = "hermes",
    val messages: List<ChatRequestMessageDto>,
    val stream: Boolean = true,
)

fun chatRequestMessage(
    role: String,
    text: String,
    imageDataUrls: List<String> = emptyList(),
): ChatRequestMessageDto {
    if (imageDataUrls.isEmpty()) {
        return ChatRequestMessageDto(role = role, content = JsonPrimitive(text))
    }

    val content = buildJsonArray {
        add(
            buildJsonObject {
                put("type", "text")
                put("text", text.ifBlank { "Photo attached." })
            },
        )
        imageDataUrls.forEach { dataUrl ->
            add(
                buildJsonObject {
                    put("type", "image_url")
                    put(
                        "image_url",
                        buildJsonObject {
                            put("url", dataUrl)
                            put("detail", "auto")
                        },
                    )
                },
            )
        }
    }
    return ChatRequestMessageDto(role = role, content = content)
}

@Serializable
data class ChatCompletionChunk(
    val choices: List<ChatChoice> = emptyList(),
    val usage: TokenUsage? = null,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatResponseChoice> = emptyList(),
    val usage: TokenUsage? = null,
)

@Serializable
data class ChatResponseChoice(
    val message: ChatMessageDto? = null,
    val text: String? = null,
)

@Serializable
data class ChatChoice(
    val delta: ChatDelta = ChatDelta(),
)

@Serializable
data class ChatDelta(
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
    val reasoning: String? = null,
    @SerialName("reasoning_delta")
    val reasoningDelta: String? = null,
    val thought: String? = null,
    val thinking: String? = null,
    val analysis: String? = null,
) {
    val reasoningText: String?
        get() = listOf(
            reasoningContent,
            reasoning,
            reasoningDelta,
            thought,
            thinking,
            analysis,
        ).firstNonBlank()
}

private fun List<String?>.firstNonBlank(): String? {
    return firstNotNullOfOrNull { value ->
        value?.takeIf { it.isNotBlank() }
    }
}

@Serializable
data class TokenUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)

@Serializable
data class SessionsResponse(
    val sessions: List<SessionDto> = emptyList(),
    val total: Int = 0,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

@Serializable
data class DashboardSessionsResponse(
    val sessions: List<DashboardSessionDto> = emptyList(),
    val total: Int = 0,
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

@Serializable
data class DashboardSessionDto(
    val id: String,
    val title: String? = null,
    val preview: String? = null,
    val source: String = "",
    @SerialName("started_at")
    val startedAt: JsonElement? = null,
    @SerialName("ended_at")
    val endedAt: JsonElement? = null,
    @SerialName("message_count")
    val messageCount: Int = 0,
    val model: String = "",
)

@Serializable
data class SessionDto(
    val id: String,
    val title: String? = null,
    val source: String = "",
    @SerialName("started_at")
    val startedAt: Long,
    @SerialName("ended_at")
    val endedAt: Long? = null,
    @SerialName("message_count")
    val messageCount: Int = 0,
    val model: String = "",
)

@Serializable
data class MessagesResponse(
    val messages: List<SessionMessageDto> = emptyList(),
)

@Serializable
data class DashboardMessagesResponse(
    val messages: List<DashboardSessionMessageDto> = emptyList(),
)

@Serializable
data class DashboardSessionMessageDto(
    val id: Long,
    @SerialName("session_id")
    val sessionId: String? = null,
    val role: String,
    val content: String? = null,
    val timestamp: JsonElement? = null,
)

@Serializable
data class SessionMessageDto(
    val id: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
    val reasoning: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)

@Serializable
data class DashboardModelInfoResponse(
    val model: String = "",
    val provider: String = "auto",
)

@Serializable
data class DashboardModelOptionsResponse(
    val providers: List<DashboardProviderDto> = emptyList(),
    val model: String = "",
    val provider: String = "auto",
)

@Serializable
data class DashboardProviderDto(
    val slug: String,
    val name: String = slug,
    @SerialName("is_current")
    val isCurrent: Boolean = false,
    val models: List<String> = emptyList(),
    @SerialName("total_models")
    val totalModels: Int = models.size,
)

@Serializable
data class OpenAiModelsResponse(
    val data: List<OpenAiModelDto> = emptyList(),
)

@Serializable
data class OpenAiModelDto(
    val id: String,
    @SerialName("owned_by")
    val ownedBy: String = "hermes",
)

data class ToolProgress(
    val label: String,
    val tool: String? = null,
    val status: String? = null,
)
