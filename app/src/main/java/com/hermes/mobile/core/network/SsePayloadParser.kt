package com.hermes.mobile.core.network

import com.hermes.mobile.core.model.ChatCompletionChunk
import com.hermes.mobile.core.model.ChatCompletionResponse
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.util.ReceivedAttachment
import com.hermes.mobile.core.util.receivedAttachmentFromRemoteFile
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

    fun parseReasoning(data: String): String? {
        return runCatching {
            val element = json.parseToJsonElement(data)
            element.reasoningMessage()
        }.getOrNull()
    }

    fun parseAttachment(data: String): ReceivedAttachment? {
        return parseAttachments(data).firstOrNull()
    }

    fun parseAttachments(data: String): List<ReceivedAttachment> {
        return runCatching {
            json.parseToJsonElement(data).toReceivedAttachments()
        }.getOrDefault(emptyList())
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

private fun JsonElement.reasoningMessage(): String? {
    return when (this) {
        is JsonPrimitive -> contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        is JsonObject -> listOf(
            this["delta"],
            this["reasoning"],
            this["reasoning_content"],
            this["reasoning_delta"],
            this["analysis"],
            this["message"],
            this["content"],
            this["text"],
        ).firstNotNullOfOrNull { it?.reasoningMessage() }
        else -> null
    }
}

private fun JsonElement.toReceivedAttachments(): List<ReceivedAttachment> {
    return when (this) {
        is JsonArray -> flatMap { it.toReceivedAttachments() }
        is JsonObject -> {
            val direct = toReceivedAttachment()?.let(::listOf).orEmpty()
            direct + listOfNotNull(
                this["attachments"],
                this["files"],
                this["artifacts"],
                this["data"],
            ).flatMap { it.toReceivedAttachments() }
        }
        else -> emptyList()
    }
}

private fun JsonObject.toReceivedAttachment(): ReceivedAttachment? {
    val url = firstReadableText("url", "href", "download_url", "downloadUrl", "file_url", "fileUrl", "uri")
        ?: return null
    val label = firstReadableText("name", "filename", "file_name", "fileName", "label", "title").orEmpty()
    val mimeType = firstReadableText("mime_type", "mimeType", "content_type", "contentType")
    return receivedAttachmentFromRemoteFile(url, label, mimeType)
}

private fun JsonObject.firstReadableText(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key -> this[key]?.readableMessage() }
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
