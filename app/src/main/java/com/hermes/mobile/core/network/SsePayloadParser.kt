package com.hermes.mobile.core.network

import com.hermes.mobile.core.model.ChatCompletionChunk
import com.hermes.mobile.core.model.ChatCompletionResponse
import com.hermes.mobile.core.model.ToolProgress
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class SsePayloadParser(
    private val json: Json,
) {
    fun parseChunk(data: String): ChatCompletionChunk? {
        return runCatching { json.decodeFromString<ChatCompletionChunk>(data) }.getOrNull()
    }

    fun parseCompletion(data: String): ChatCompletionResponse? {
        return runCatching { json.decodeFromString<ChatCompletionResponse>(data) }.getOrNull()
    }

    fun parseTool(data: String): ToolProgress? {
        return runCatching {
            val obj = json.decodeFromString<JsonObject>(data)
            val tool = obj["tool"]?.readableMessage()
            ToolProgress(
                label = obj["label"]?.readableMessage()
                    ?: obj["message"]?.readableMessage()
                    ?: tool
                    ?: "Tool running",
                tool = tool,
                status = obj["status"]?.readableMessage(),
            )
        }.getOrNull()
    }

    fun parseError(data: String): String {
        return runCatching {
            val obj = json.decodeFromString<JsonObject>(data)
            obj["error"]?.readableMessage()
                ?: obj["message"]?.readableMessage()
                ?: obj["detail"]?.readableMessage()
                ?: data.errorPreview()
        }.getOrDefault(data.errorPreview())
    }
}

private fun JsonElement.readableMessage(): String? {
    return when (this) {
        is JsonPrimitive -> contentOrNull?.readablePreview()
        is JsonObject -> this["message"]?.readableMessage()
            ?: this["detail"]?.readableMessage()
            ?: this["error"]?.readableMessage()
            ?: this["label"]?.readableMessage()
            ?: this["name"]?.readableMessage()
            ?: this["tool"]?.readableMessage()
        else -> null
    }
}

private fun String.errorPreview(): String {
    return readablePreview(maxLength = MaxErrorPreviewLength) ?: "Unknown server error"
}

private fun String.readablePreview(maxLength: Int = MaxReadableMessageLength): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.compactPreview(maxLength)
}

private fun String.compactPreview(maxLength: Int): String {
    if (length <= maxLength) return this
    val head = take(maxLength).trimEnd()
    val boundary = head.lastIndexOf(' ').takeIf { it >= maxLength / 2 }
    return boundary?.let { head.take(it) } ?: head
}

private const val MaxReadableMessageLength = 180
private const val MaxErrorPreviewLength = 200
