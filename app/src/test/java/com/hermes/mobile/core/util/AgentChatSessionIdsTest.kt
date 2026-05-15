package com.hermes.mobile.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AgentChatSessionIdsTest {
    @Test
    fun createsStableSessionIdForAgent() {
        assertEquals("agent-chat-hermes", agentChatSessionId("hermes"))
        assertEquals("agent-chat-agent-123", agentChatSessionId("agent-123"))
    }

    @Test
    fun createsStableSessionIdFromFirstPastedAgentLine() {
        assertEquals("agent-chat-agent-123", agentChatSessionId("\n agent-123\nagent-456 "))
        assertEquals("agent-chat-hermes", agentChatSessionId("\n\n"))
    }

    @Test
    fun parsesAgentIdFromStableSessionId() {
        assertEquals("hermes", agentIdFromChatSessionId("agent-chat-hermes"))
        assertEquals("agent-123", agentIdFromChatSessionId("agent-chat-agent-123"))
        assertEquals("hermes", agentIdFromChatSessionId("agent-chat-hermes--1700000000000"))
        assertEquals("hermes", agentIdFromChatSessionId("\n agent-chat-hermes\nignored "))
        assertNull(agentIdFromChatSessionId("random-session"))
    }

    @Test
    fun createsDistinctAgentChatSessionIdsForNewThreads() {
        assertEquals("agent-chat-hermes--42", newAgentChatSessionId("hermes", 42L))
        assertEquals("agent-chat-agent-123--42", newAgentChatSessionId("\n agent-123\nignored ", 42L))
    }

    @Test
    fun keepsStableAgentSessionWhenBackendReturnsAnotherSession() {
        assertEquals(
            "agent-chat-hermes",
            resolveOpenedChatSessionId(
                localSessionId = "  agent-chat-hermes  ",
                remoteSessionId = "remote-created-session",
            ),
        )
        assertEquals(
            "remote-created-session",
            resolveOpenedChatSessionId(
                localSessionId = "manual-session",
                remoteSessionId = "remote-created-session",
            ),
        )
        assertEquals(
            "remote-created-session",
            resolveOpenedChatSessionId(
                localSessionId = "manual-session",
                remoteSessionId = "\n remote-created-session\nignored ",
            ),
        )
        assertEquals(
            "manual-session",
            resolveOpenedChatSessionId(
                localSessionId = "  manual-session  ",
                remoteSessionId = "   ",
            ),
        )
    }
}
