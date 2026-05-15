package com.hermes.mobile.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatRequestContentTest {
    private val json = Json

    @Test
    fun textOnlyMessagesStayOpenAiCompatible() {
        val encoded = json.encodeToString(
            ChatCompletionRequest(
                messages = listOf(chatRequestMessage("user", "hello")),
            ),
        )

        assertTrue(encoded.contains("\"content\":\"hello\""))
        assertFalse(encoded.contains("image_url"))
    }

    @Test
    fun imageMessagesUseOpenAiContentParts() {
        val encoded = json.encodeToString(
            ChatCompletionRequest(
                messages = listOf(
                    chatRequestMessage(
                        role = "user",
                        text = "What is in this photo?",
                        imageDataUrls = listOf("data:image/png;base64,abc123"),
                    ),
                ),
            ),
        )

        assertTrue(encoded.contains("\"type\":\"text\""))
        assertTrue(encoded.contains("\"text\":\"What is in this photo?\""))
        assertTrue(encoded.contains("\"type\":\"image_url\""))
        assertTrue(encoded.contains("\"url\":\"data:image/png;base64,abc123\""))
    }
}
