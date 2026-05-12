package com.hermes.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String = "hermes",
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
)

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
)

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

data class ToolProgress(
    val label: String,
    val tool: String? = null,
    val status: String? = null,
)
