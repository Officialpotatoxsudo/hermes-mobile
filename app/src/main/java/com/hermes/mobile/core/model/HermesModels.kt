package com.hermes.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class SessionMessageDto(
    val id: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
)

data class ToolProgress(
    val label: String,
    val tool: String? = null,
    val status: String? = null,
)

@Serializable
data class SendPaymentRequest(
    val recipientAddress: String,
    val amount: String,
    val network: String = "matic",
    val description: String = "",
)

@Serializable
data class SendPaymentResponse(
    val id: String = "",
    val paymentUrl: String = "",
    val status: String = "pending",
)
