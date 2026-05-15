package com.hermes.mobile.core.error

import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {
    fun userMessage(error: Throwable?, fallback: String = "Something went wrong. Please try again."): String {
        val rawMessage = error?.message?.trim().orEmpty()
        val message = rawMessage.cleanUserMessage()
        return when {
            error is UnknownHostException -> "Server not reachable. Check the URL and network."
            error is SocketTimeoutException || rawMessage.contains("timeout", ignoreCase = true) -> "Connection timed out. Hermes may still be working. Try again."
            httpAuthPattern.containsMatchIn(rawMessage) -> "API key rejected. Check your credentials."
            http404Pattern.containsMatchIn(rawMessage) -> "Hermes endpoint not found. Check server version and URL."
            rawMessage.contains("ngrok", ignoreCase = true) -> "Tunnel is offline or expired. Restart ngrok and update server URL."
            rawMessage.contains("Expected text/event-stream", ignoreCase = true) -> "Streaming unavailable. Retrying with normal response."
            message.isNotBlank() -> message
            else -> fallback
        }
    }

    fun isEndpointNotFound(error: Throwable?): Boolean {
        val message = error?.message?.trim().orEmpty()
        return http404Pattern.containsMatchIn(message) ||
            message.contains("endpoint not found", ignoreCase = true)
    }
}

private fun String.cleanUserMessage(): String {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .compactUserMessage()
}

private fun String.compactUserMessage(): String {
    if (length <= MaxUserMessageLength) return this
    val head = take(MaxUserMessageLength).trimEnd()
    val boundary = head.lastIndexOf(' ').takeIf { it >= MaxUserMessageLength / 2 }
    return boundary?.let { head.take(it) } ?: head
}

private const val MaxUserMessageLength = 180
private val http404Pattern = Regex("""(?i)(?:\bhttp\s*)?404\b""")
private val httpAuthPattern = Regex("""(?i)(?:\bhttp\s*)?(?:401|403)\b""")
