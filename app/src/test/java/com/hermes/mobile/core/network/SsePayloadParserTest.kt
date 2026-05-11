package com.hermes.mobile.core.network

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SsePayloadParserTest {
    private val parser = SsePayloadParser(Json { ignoreUnknownKeys = true })

    @Test
    fun parsesStreamingChunk() {
        val chunk = parser.parseChunk(
            """{"choices":[{"delta":{"content":"hi"}}],"usage":{"prompt_tokens":1,"completion_tokens":2,"total_tokens":3}}""",
        )

        assertEquals("hi", chunk?.choices?.first()?.delta?.content)
        assertEquals(3, chunk?.usage?.totalTokens)
    }

    @Test
    fun parsesNonStreamingCompletionFallback() {
        val completion = parser.parseCompletion(
            """{"choices":[{"message":{"role":"assistant","content":"hello"}}],"usage":{"prompt_tokens":4,"completion_tokens":5,"total_tokens":9}}""",
        )

        assertEquals("hello", completion?.choices?.first()?.message?.content)
        assertEquals(9, completion?.usage?.totalTokens)
    }
}
