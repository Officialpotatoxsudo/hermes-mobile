package com.hermes.mobile.feature.home

import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.util.agentIdFromChatSessionId
import com.hermes.mobile.core.util.newAgentChatSessionId
import com.hermes.mobile.feature.chat.ChatStreamSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeAgentListTest {
    @Test
    fun latestSessionsByAgentUsesNewestSession() {
        val newer = session(id = "agent-chat-hermes", startedAt = 2_000, messageCount = 4)
        val older = session(id = "agent-chat-hermes", startedAt = 1_000, messageCount = 9)
        val other = session(id = "manual-session", startedAt = 500, messageCount = 1)

        assertEquals(mapOf("hermes" to newer), latestSessionsByAgent(listOf(older, other, newer)))
    }

    @Test
    fun latestSessionsByAgentUsesPlainRemoteSessionsForDefaultAgent() {
        val remote = session(id = "server-session-1", startedAt = 5_000, messageCount = 4)
        val olderDefault = session(id = "agent-chat-hermes--1", startedAt = 1_000, messageCount = 2)

        assertEquals(mapOf("hermes" to remote), latestSessionsByAgent(listOf(olderDefault, remote)))
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
    fun conversationLiveStatePrioritizesUnreadAndRecentActivity() {
        assertEquals("unread", conversationLiveState(session(id = "agent-chat-hermes--1", startedAt = 1_000, messageCount = 2, unreadCount = 1), now = 10_000))
        assertEquals("recent", conversationLiveState(session(id = "agent-chat-hermes--2", startedAt = 1_000, messageCount = 2, localLastActivityAt = 9_000), now = 10_000))
        assertEquals("none", conversationLiveState(session(id = "agent-chat-hermes--3", startedAt = 1_000, messageCount = 2, localLastActivityAt = 1_000), now = 10_000_000))
    }

    @Test
    fun conversationInboxDoesNotRenderStreamingPlaceholderPreview() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val rows = conversationInboxRows(
            listOf(
                session(
                    id = "agent-chat-hermes--1",
                    startedAt = 1_000,
                    messageCount = 2,
                    lastMessagePreview = "Streaming...",
                ),
            ),
            agents,
        )

        assertEquals(listOf("2 messages"), rows.map { it.preview })
    }

    @Test
    fun conversationInboxUsesActiveStreamStateWhenPresent() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val rows = conversationInboxRows(
            listOf(session(id = "agent-chat-hermes--1", startedAt = 1_000, messageCount = 2, lastMessagePreview = "Hello")),
            agents,
            activeStreams = mapOf(
                "agent-chat-hermes--1" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--1",
                    assistantMessageId = 2L,
                    isConnecting = true,
                ),
            ),
        )

        assertEquals(listOf("Thinking"), rows.map { it.preview })
        assertEquals(listOf("thinking"), rows.map { it.liveState })
    }

    @Test
    fun conversationInboxSynthesizesRowsForActiveStreamsWithoutSavedSession() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val rows = conversationInboxRows(
            sessions = emptyList(),
            agents = agents,
            activeStreams = mapOf(
                "agent-chat-hermes--streaming" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--streaming",
                    assistantMessageId = 2L,
                    userMessage = MessageEntity(
                        id = 1L,
                        sessionId = "agent-chat-hermes--streaming",
                        role = "user",
                        content = "Resume this chat",
                        timestamp = 1L,
                    ),
                    isConnecting = true,
                ),
            ),
            now = 2_000,
        )

        assertEquals(listOf("agent-chat-hermes--streaming"), rows.map { it.sessionId })
        assertEquals(listOf("Hermes Agent"), rows.map { it.agentName })
        assertEquals(listOf("Thinking"), rows.map { it.preview })
        assertEquals(listOf("thinking"), rows.map { it.liveState })
    }

    @Test
    fun dashboardMetricsCountLiveUnreadAndChats() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val rows = conversationInboxRows(
            listOf(
                session(id = "agent-chat-hermes--1", startedAt = 1_000, messageCount = 2, unreadCount = 3),
                session(id = "agent-chat-hermes--2", startedAt = 2_000, messageCount = 4),
                session(id = "agent-chat-hermes--3", startedAt = 3_000, messageCount = 5),
            ),
            agents,
            activeStreams = mapOf(
                "agent-chat-hermes--2" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--2",
                    assistantMessageId = 2L,
                    isConnecting = false,
                    isStreaming = true,
                ),
                "agent-chat-hermes--3" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--3",
                    assistantMessageId = 3L,
                    error = "Network issue",
                ),
            ),
        )

        assertEquals(HomeDashboardMetrics(liveCount = 2, unreadCount = 3, chatCount = 3), dashboardMetrics(rows))
    }

    @Test
    fun dashboardStateLabelPrioritizesLiveStatesOverUnread() {
        assertEquals("Issue", dashboardStateLabel("error", unreadCount = 4))
        assertEquals("Thinking", dashboardStateLabel("thinking", unreadCount = 4))
        assertEquals("Live", dashboardStateLabel("streaming", unreadCount = 4))
        assertEquals("Unread", dashboardStateLabel("unread", unreadCount = 4))
    }

    @Test
    fun dashboardContinueRowPrioritizesIssueLiveUnreadRecentThenLatest() {
        val agents = listOf(AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"))
        val sessions = listOf(
            session(id = "agent-chat-hermes--latest", startedAt = 5_000, messageCount = 2, localLastActivityAt = 5_000),
            session(id = "agent-chat-hermes--recent", startedAt = 3_000, messageCount = 2, localLastActivityAt = 199_500),
            session(id = "agent-chat-hermes--unread", startedAt = 2_000, messageCount = 2, unreadCount = 1),
            session(id = "agent-chat-hermes--live", startedAt = 1_000, messageCount = 2),
            session(id = "agent-chat-hermes--thinking", startedAt = 750, messageCount = 2),
            session(id = "agent-chat-hermes--error", startedAt = 500, messageCount = 2),
        )
        val rows = conversationInboxRows(
            sessions,
            agents,
            activeStreams = mapOf(
                "agent-chat-hermes--live" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--live",
                    assistantMessageId = 2L,
                    isConnecting = false,
                    isStreaming = true,
                ),
                "agent-chat-hermes--thinking" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--thinking",
                    assistantMessageId = 3L,
                    isConnecting = true,
                ),
                "agent-chat-hermes--error" to ChatStreamSnapshot(
                    sessionId = "agent-chat-hermes--error",
                    assistantMessageId = 4L,
                    error = "Tool failed",
                ),
            ),
            now = 200_000,
        )

        assertEquals("agent-chat-hermes--error", dashboardContinueRow(rows)?.sessionId)
        assertEquals("agent-chat-hermes--thinking", dashboardContinueRow(rows.filterNot { it.liveState == "error" })?.sessionId)
        assertEquals("agent-chat-hermes--live", dashboardContinueRow(rows.filterNot { it.liveState in setOf("error", "thinking") })?.sessionId)
        assertEquals("agent-chat-hermes--unread", dashboardContinueRow(rows.filterNot { it.liveState in setOf("error", "thinking", "streaming") })?.sessionId)
        assertEquals("agent-chat-hermes--recent", dashboardContinueRow(rows.filterNot { it.liveState in setOf("error", "thinking", "streaming", "unread") })?.sessionId)
        assertEquals("agent-chat-hermes--latest", dashboardContinueRow(rows.filter { it.liveState == "none" })?.sessionId)
    }

    @Test
    fun dashboardAgentRowsKeepDefaultFirstThenOrderByLatestActivity() {
        val agents = listOf(
            AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private", initial = "H"),
            AgentProfile(id = "research", name = "Research", subtitle = "Find sources", initial = "R"),
            AgentProfile(id = "code", name = "Code", subtitle = "Patch code", initial = "C"),
            AgentProfile(id = "draft", name = "Draft", subtitle = "Write copy", initial = "D"),
        )
        val rows = dashboardAgentRows(
            agents = agents,
            sessions = listOf(
                session(id = "agent-chat-code--1", startedAt = 1_000, messageCount = 2, localLastActivityAt = 8_000),
                session(id = "agent-chat-research--1", startedAt = 2_000, messageCount = 4, localLastActivityAt = 6_000),
                session(id = "agent-chat-hermes--1", startedAt = 3_000, messageCount = 6, localLastActivityAt = 4_000),
            ),
        )

        assertEquals(listOf("hermes", "code", "research", "draft"), rows.map { it.agent.id })
        assertEquals(listOf("agent-chat-hermes--1", "agent-chat-code--1", "agent-chat-research--1", null), rows.map { it.latestSessionId })
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
        lastMessagePreview: String? = null,
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
            lastMessagePreview = lastMessagePreview,
        )
    }
}
