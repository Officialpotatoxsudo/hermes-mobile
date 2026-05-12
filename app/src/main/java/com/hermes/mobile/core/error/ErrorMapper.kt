package com.hermes.mobile.core.error

import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {
    fun userMessage(error: Throwable?, fallback: String = "Something went wrong. Please try again."): String {
        val message = error?.message.orEmpty()
        return when {
            error is UnknownHostException -> "Server not reachable. Check the URL and network."
            error is SocketTimeoutException || message.contains("timeout", ignoreCase = true) -> "Connection timed out. Hermes may still be working. Try again."
            message.contains("401") || message.contains("403") -> "API key rejected. Check your credentials."
            message.contains("404") -> "Hermes endpoint not found. Check server version and URL."
            message.contains("ngrok", ignoreCase = true) -> "Tunnel is offline or expired. Restart ngrok and update server URL."
            message.contains("Expected text/event-stream", ignoreCase = true) -> "Streaming unavailable. Retrying with normal response."
            message.isNotBlank() -> message
            else -> fallback
        }
    }
}
