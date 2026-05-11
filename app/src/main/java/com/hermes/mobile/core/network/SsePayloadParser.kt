package com.hermes.mobile.core.network

import com.hermes.mobile.core.model.ChatCompletionChunk
import com.hermes.mobile.core.model.ChatCompletionResponse
import com.hermes.mobile.core.model.ToolProgress
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
            ToolProgress(
                label = obj["label"]?.jsonPrimitive?.contentOrNull
                    ?: obj["message"]?.jsonPrimitive?.contentOrNull
                    ?: obj["tool"]?.jsonPrimitive?.contentOrNull
                    ?: "Tool running",
                tool = obj["tool"]?.jsonPrimitive?.contentOrNull,
                status = obj["status"]?.jsonPrimitive?.contentOrNull,
            )
        }.getOrNull()
    }

    fun parseError(data: String): String {
        return runCatching {
            val obj = json.decodeFromString<JsonObject>(data)
            obj["error"]?.jsonPrimitive?.contentOrNull
                ?: obj["message"]?.jsonPrimitive?.contentOrNull
                ?: data.take(200)
        }.getOrDefault(data.take(200))
    }
}
