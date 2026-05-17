package com.hermes.mobile.feature.home

import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.util.agentIdFromChatSessionId
import com.hermes.mobile.core.util.newAgentChatSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeAgentListTest {
    @Test
    fun latestSessionsByAgentUsesNewestSession() {
        val newer = session(id = "agent-chat-hermes", startedAt = 2_000, messageCount = 4)
        val older = session(id = "agent-chat-hermes", startedAt = 1_000, messageCount = 9)
        val other = session(id = "manual-session", startedAt = 3_000, messageCount = 1)

        assertEquals(mapOf("hermes" to newer), latestSessionsByAgent(listOf(older, other, newer)))
    }

    @Test
    fun conversationInboxOrdersByLocalActivity() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val olderActive = session(
            id = "agent-chat-hermes--1",
            startedAt = 3_000,
            messageCount = 3,
            localLastActivityAt = 3_000,
        )
        val newerActive = session(
            id = "agent-chat-hermes--2",
            startedAt = 2_000,
            messageCount = 4,
            localLastActivityAt = 4_000,
        )

        val rows = conversationInboxRows(listOf(olderActive, newerActive), agents)

        assertEquals(listOf("agent-chat-hermes--2", "agent-chat-hermes--1"), rows.map { it.sessionId })
        assertEquals(listOf("Hermes Agent", "Hermes Agent"), rows.map { it.agentName })
        assertEquals(listOf(true, true), rows.map { it.usesDefaultAvatar })
        assertEquals(listOf("none", "none"), rows.map { it.liveState })
        assertEquals(listOf(0, 0), rows.map { it.unreadCount })
    }

    @Test
    fun conversationInboxKeepsUnreadCount() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val rows = conversationInboxRows(
            listOf(
                session(
                    id = "agent-chat-hermes--1",
                    startedAt = 1_000,
                    messageCount = 2,
                    unreadCount = 2,
                ),
            ),
            agents,
        )

        assertEquals(listOf(2), rows.map { it.unreadCount })
    }

    @Test
    fun latestSessionsByAgentGroupsDistinctAgentThreads() {
        val older = session(id = "agent-chat-hermes--1000", startedAt = 1_000, messageCount = 2)
        val newer = session(id = "agent-chat-hermes--2000", startedAt = 2_000, messageCount = 5)

        assertEquals(mapOf("hermes" to newer), latestSessionsByAgent(listOf(older, newer)))
    }

    @Test
    fun latestSessionsByAgentUsesLastActivityBeforeStartedAt() {
        val recentlyActive = session(
            id = "agent-chat-hermes--1000",
            startedAt = 1_000,
            messageCount = 2,
            localLastActivityAt = 3_000,
        )
        val newerButInactive = session(
            id = "agent-chat-hermes--2000",
            startedAt = 2_000,
            messageCount = 5,
            localLastActivityAt = 2_000,
        )

        assertEquals(mapOf("hermes" to recentlyActive), latestSessionsByAgent(listOf(recentlyActive, newerButInactive)))
    }

    @Test
    fun newAgentThreadKeepsOldThreadInHistory() {
        assertEquals("agent-chat-hermes--2", newAgentChatSessionId("hermes", 2))
        assertEquals("hermes", agentIdFromChatSessionId("agent-chat-hermes--1"))
        assertEquals("hermes", agentIdFromChatSessionId(newAgentChatSessionId("hermes", 2)))
    }

    @Test
    fun agentRowSubtitleFallsBackWhenAgentRoleBlank() {
        val agent = AgentProfile(id = "agent-1", name = "Research", subtitle = "   ", initial = "R")

        assertEquals("Custom Hermes agent", agentRowSubtitle(agent, session = null))
    }

    @Test
    fun agentRowSubtitleUsesFirstUsefulRoleLine() {
        val agent = AgentProfile(id = "agent-1", name = "Research", subtitle = "\n Research aide \n ignored", initial = "R")

        assertEquals("Research aide", agentRowSubtitle(agent, session = null))
    }

    @Test
    fun agentRowSubtitleUsesCompactRolePreview() {
        val agent = AgentProfile(
            id = "agent-1",
            name = "Research",
            subtitle = "Research assistant for long-form codebase investigation, planning, and implementation support",
            initial = "R",
        )

        assertEquals("Research assistant for long-form codebase...", agentRowSubtitle(agent, session = null))
    }

    @Test
    fun agentRowSubtitleUsesSessionSummaryWhenPresent() {
        val agent = AgentProfile(id = "hermes", name = "Hermes", subtitle = "Default", initial = "H")

        assertEquals(
            "2 messages · Jan 1",
            agentRowSubtitle(agent, session(id = "agent-chat-hermes", startedAt = 1_704_067_200_000, messageCount = 2)),
        )
    }

    private fun session(
        id: String,
        startedAt: Long,
        messageCount: Int,
        localLastActivityAt: Long = startedAt,
        unreadCount: Int = 0,
    ): SessionEntity {
        return SessionEntity(
            id = id,
            title = null,
            source = "mobile",
            startedAt = startedAt,
            endedAt = null,
            messageCount = messageCount,
            model = "hermes-agent",
            localLastActivityAt = localLastActivityAt,
            unreadCount = unreadCount,
        )
    }
}
