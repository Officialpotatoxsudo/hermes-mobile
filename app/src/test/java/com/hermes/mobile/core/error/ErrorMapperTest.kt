package com.hermes.mobile.core.error

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorMapperTest {
    @Test
    fun detectsEndpointNotFoundBeforeMessageMapping() {
        assertTrue(ErrorMapper.isEndpointNotFound(IllegalStateException("Sessions failed: HTTP 404")))
        assertTrue(ErrorMapper.isEndpointNotFound(IllegalStateException("Hermes endpoint not found. Check server version and URL.")))
        assertFalse(ErrorMapper.isEndpointNotFound(IllegalStateException("Server error HTTP 500")))
        assertFalse(ErrorMapper.isEndpointNotFound(IllegalStateException("Server error HTTP 4040")))
    }

    @Test
    fun userMessageDoesNotTreat404SubstringAsMissingEndpoint() {
        assertEquals(
            "Hermes endpoint not found. Check server version and URL.",
            ErrorMapper.userMessage(IllegalStateException("Sessions failed: HTTP 404")),
        )
        assertEquals(
            "Server error HTTP 4040",
            ErrorMapper.userMessage(IllegalStateException("Server error HTTP 4040")),
        )
    }

    @Test
    fun userMessageDoesNotTreatAuthSubstringAsRejectedApiKey() {
        assertEquals(
            "API key rejected. Check your credentials.",
            ErrorMapper.userMessage(IllegalStateException("Health check failed: HTTP 401")),
        )
        assertEquals(
            "Server error HTTP 4030",
            ErrorMapper.userMessage(IllegalStateException("Server error HTTP 4030")),
        )
    }

    @Test
    fun userMessageTrimsRawThrowableMessage() {
        assertEquals(
            "Server error HTTP 500",
            ErrorMapper.userMessage(IllegalStateException("  Server error HTTP 500  ")),
        )
    }

    @Test
    fun userMessageUsesFirstUsefulLineForRawThrowableMessage() {
        assertEquals(
            "Server error HTTP 500",
            ErrorMapper.userMessage(IllegalStateException("\n  Server error HTTP 500  \nstack trace line")),
        )
    }

    @Test
    fun userMessageKeepsClassificationWhenSignalIsAfterFirstLine() {
        assertEquals(
            "API key rejected. Check your credentials.",
            ErrorMapper.userMessage(IllegalStateException("Request failed\nHTTP 403")),
        )
    }

    @Test
    fun userMessageMapsTunnelClosed() {
        assertEquals(
            "Tunnel closed. Keep the Hermes server tunnel running, then retry.",
            ErrorMapper.userMessage(IllegalStateException("tunnel closed")),
        )
    }

    @Test
    fun userMessageCapsLongRawThrowableMessage() {
        val message = ErrorMapper.userMessage(IllegalStateException("x".repeat(220)))

        assertEquals(180, message.length)
    }

    @Test
    fun userMessageCapsLongRawThrowableMessageAtWordBoundary() {
        val message = ErrorMapper.userMessage(IllegalStateException("word ".repeat(35) + "tailword"))

        assertEquals("word ".repeat(35).trim(), message)
    }
}
