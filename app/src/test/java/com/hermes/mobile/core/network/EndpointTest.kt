package com.hermes.mobile.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import okhttp3.Request

class EndpointTest {
    @Test
    fun endpointJoinsBaseAndPath() {
        assertEquals("https://agent.example/v1/sessions", "https://agent.example/".endpoint("/v1/sessions"))
    }

    @Test
    fun endpointPreservesQuery() {
        assertEquals(
            "https://agent.example/v1/sessions?limit=50&offset=0",
            "https://agent.example".endpoint("v1/sessions?limit=50&offset=0"),
        )
    }

    @Test
    fun endpointDoesNotDuplicateV1WhenBaseAlreadyIncludesIt() {
        assertEquals(
            "https://agent.example/v1/chat/completions",
            "https://agent.example/v1".endpoint("v1/chat/completions"),
        )
    }

    @Test
    fun sessionHeadersUseHermesContinuationContract() {
        val request = Request.Builder()
            .url("https://agent.example/v1/chat/completions")
            .applyHermesSessionHeaders("session-123", "api-key")
            .build()

        assertEquals("session-123", request.header("X-Hermes-Session-Id"))
        assertEquals("mobile:session-123", request.header("X-Hermes-Session-Key"))
    }

    @Test
    fun sessionKeyIsNotSentWithoutApiKey() {
        val request = Request.Builder()
            .url("https://agent.example/v1/chat/completions")
            .applyHermesSessionHeaders("session-123", "")
            .build()

        assertEquals("session-123", request.header("X-Hermes-Session-Id"))
        assertNull(request.header("X-Hermes-Session-Key"))
    }
}
