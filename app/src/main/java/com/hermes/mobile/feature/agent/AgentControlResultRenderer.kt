package com.hermes.mobile.feature.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

sealed interface RenderedControlResult {
    data class Text(val text: String) : RenderedControlResult
    data class Structured(
        val title: String,
        val fields: List<RenderedControlField>,
        val raw: String,
    ) : RenderedControlResult
}

data class RenderedControlField(
    val key: String,
    val value: String,
)

fun renderControlResult(raw: String): RenderedControlResult {
    val clean = raw.trim()
    if (clean.isBlank()) return RenderedControlResult.Text("")
    val element = runCatching { Json.parseToJsonElement(clean) }.getOrNull()
        ?: return RenderedControlResult.Text(clean)
    val fields = when (element) {
        is JsonObject -> element.map { (key, value) -> RenderedControlField(key, value.readableControlValue()) }
        is JsonArray -> element.mapIndexed { index, value -> RenderedControlField("#${index + 1}", value.readableControlValue()) }
        else -> emptyList()
    }
    return if (fields.isEmpty()) {
        RenderedControlResult.Text(clean)
    } else {
        RenderedControlResult.Structured(
            title = "JSON response",
            fields = fields,
            raw = clean,
        )
    }
}

private fun JsonElement.readableControlValue(): String {
    return when (this) {
        is JsonPrimitive -> contentOrNull ?: toString()
        is JsonArray -> joinToString(", ") { it.readableControlValue() }
        is JsonObject -> entries.joinToString(", ") { (key, value) -> "$key: ${value.readableControlValue()}" }
    }.ifBlank { "empty" }
}
