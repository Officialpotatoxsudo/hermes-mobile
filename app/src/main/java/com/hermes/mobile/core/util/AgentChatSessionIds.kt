package com.hermes.mobile.core.util

private const val AGENT_CHAT_PREFIX = "agent-chat-"
private const val AGENT_CHAT_THREAD_SEPARATOR = "--"

fun agentChatSessionId(agentId: String): String {
    val clean = agentId.cleanAgentId()
    return "$AGENT_CHAT_PREFIX$clean"
}

fun newAgentChatSessionId(agentId: String, seed: Long = System.currentTimeMillis()): String {
    return "${agentChatSessionId(agentId)}$AGENT_CHAT_THREAD_SEPARATOR$seed"
}

fun agentIdFromChatSessionId(sessionId: String?): String? {
    val value = sessionId.cleanChatSessionId()
    return value
        .takeIf { it.startsWith(AGENT_CHAT_PREFIX) }
        ?.removePrefix(AGENT_CHAT_PREFIX)
        ?.substringBefore(AGENT_CHAT_THREAD_SEPARATOR)
        ?.takeIf { it.isNotBlank() }
}

fun resolveOpenedChatSessionId(localSessionId: String, remoteSessionId: String?): String {
    val cleanLocalSessionId = localSessionId.cleanChatSessionId()
    return if (agentIdFromChatSessionId(cleanLocalSessionId) != null) {
        cleanLocalSessionId
    } else {
        remoteSessionId.cleanChatSessionId().takeIf { it.isNotBlank() } ?: cleanLocalSessionId
    }
}

private fun String.cleanAgentId(): String = cleanChatSessionId().ifBlank { "hermes" }

private fun String?.cleanChatSessionId(): String {
    return this
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        .orEmpty()
}
