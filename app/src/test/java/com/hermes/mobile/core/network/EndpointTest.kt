package com.hermes.mobile.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun endpointTrimsPastedPathWhitespace() {
        assertEquals(
            "https://agent.example/v1/sessions",
            "https://agent.example".endpoint("  /v1/sessions  "),
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
    fun endpointDoesNotDuplicateApiWhenBaseAlreadyIncludesIt() {
        assertEquals(
            "https://agent.example/api/sessions?limit=50",
            "https://agent.example/api".endpoint("api/sessions?limit=50"),
        )
    }

    @Test
    fun endpointIgnoresQueryAndFragmentOnSavedBaseUrl() {
        assertEquals(
            "https://agent.example/api/health",
            "https://agent.example/api?token=old#setup".endpoint("api/health"),
        )
    }

    @Test
    fun bearerHeaderTrimsPastedApiKey() {
        val request = Request.Builder()
            .url("https://agent.example/health")
            .applyBearer("  api-key  ")
            .build()

        assertEquals("Bearer api-key", request.header("Authorization"))
    }

    @Test
    fun bearerHeaderUsesFirstSafeApiKeyLine() {
        val request = Request.Builder()
            .url("https://agent.example/health")
            .applyBearer("\n api-key\nInjected: bad ")
            .build()

        assertEquals("Bearer api-key", request.header("Authorization"))
    }

    @Test
    fun hermesRequestPathTrimsPastedWhitespaceAndSlashes() {
        assertEquals("api/runs/status", hermesRequestPath("  /api/runs/status  "))
        assertEquals("health", hermesRequestPath(" /health "))
        assertEquals("api/runs/status", hermesRequestPath("\n /api/runs/status\nignored "))
    }

    @Test
    fun hermesRequestPathRejectsBlankPath() {
        assertThrows(IllegalStateException::class.java) {
            hermesRequestPath(" / ")
        }
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

    @Test
    fun sessionKeyIsNotSentWithBlankPastedApiKey() {
        val request = Request.Builder()
            .url("https://agent.example/v1/chat/completions")
            .applyHermesSessionHeaders(" session-123 ", "   ")
            .build()

        assertEquals("session-123", request.header("X-Hermes-Session-Id"))
        assertNull(request.header("X-Hermes-Session-Key"))
    }

    @Test
    fun sessionHeadersUseFirstSafeSessionIdLine() {
        val request = Request.Builder()
            .url("https://agent.example/v1/chat/completions")
            .applyHermesSessionHeaders("\n session-123\nInjected: bad ", "\n api-key\nInjected: bad ")
            .build()

        assertEquals("session-123", request.header("X-Hermes-Session-Id"))
        assertEquals("mobile:session-123", request.header("X-Hermes-Session-Key"))
    }

    @Test
    fun sessionMessagesPathEncodesSessionIdSegment() {
        assertEquals(
            "api/sessions/telegram%3Atopic%2F42%20A/messages",
            sessionMessagesPath("\n api/\nignored ", "\n telegram:topic/42 A\nignored "),
        )
    }

    @Test
    fun sessionMessagesPathRejectsBlankSessionId() {
        assertThrows(IllegalStateException::class.java) {
            sessionMessagesPath("api", "   ")
        }
    }
}
