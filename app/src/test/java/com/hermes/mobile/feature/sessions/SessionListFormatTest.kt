package com.hermes.mobile.feature.sessions

import com.hermes.mobile.core.data.local.SessionEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionListFormatTest {
    @Test
    fun blankSessionTitleUsesFallback() {
        assertEquals("Untitled session", sessionDisplayTitle(null))
        assertEquals("Untitled session", sessionDisplayTitle("   "))
        assertEquals("Planning", sessionDisplayTitle("  Planning  "))
        assertEquals("Planning", sessionDisplayTitle("\n Planning\nignored "))
    }

    @Test
    fun longSessionTitleUsesCompactPreview() {
        val title = "Plan mobile chat polish with agent list routing, cached history, media restore, and model switching"

        assertEquals(
            "Plan mobile chat polish with agent list routing, cached history, media...",
            sessionDisplayTitle(title),
        )
    }

    @Test
    fun longSessionTitleCompactsAtWordBoundary() {
        val title = "Summarize onboarding plan for mobile product reliability improvementstogether across chat session history"

        assertEquals(
            "Summarize onboarding plan for mobile product reliability...",
            sessionDisplayTitle(title),
        )
    }

    @Test
    fun metadataOmitsEmptyModelDetail() {
        assertEquals("1 message", sessionMetadataLine(messageCount = 1, model = "", source = "  "))
        assertEquals("2 messages • hermes-agent", sessionMetadataLine(messageCount = 2, model = " hermes-agent ", source = "mobile"))
        assertEquals("3 messages • mobile", sessionMetadataLine(messageCount = 3, model = " ", source = " mobile "))
        assertEquals(
            "4 messages • hermes-agent",
            sessionMetadataLine(messageCount = 4, model = "\n hermes-agent\nignored ", source = "\n mobile\nignored "),
        )
    }

    @Test
    fun longMetadataDetailUsesCompactPreview() {
        assertEquals(
            "2 messages • openrouter/very-long-model-name-with-extra-ro...",
            sessionMetadataLine(
                messageCount = 2,
                model = "openrouter/very-long-model-name-with-extra-routing-and-diagnostic-suffix",
                source = "mobile",
            ),
        )
    }

    @Test
    fun sessionSearchMatchesVisibleFallbackTitle() {
        val session = session(title = "   ", model = "hermes-agent", source = "mobile")

        assertTrue(sessionMatchesQuery(session, "untitled"))
        assertTrue(sessionMatchesQuery(session, "Hermes"))
        assertTrue(sessionMatchesQuery(session, "mobile"))
        assertFalse(sessionMatchesQuery(session, "missing"))
    }

    @Test
    fun sessionSearchUsesFirstUsefulQueryLine() {
        val session = session(title = "Planning", model = "hermes-agent", source = "mobile")

        assertTrue(sessionMatchesQuery(session, "\n plan \n ignored"))
        assertTrue(sessionMatchesQuery(session, "\n  \n "))
    }

    @Test
    fun sessionSearchMatchesFullTitleBeyondCompactPreview() {
        val session = session(
            title = "Plan mobile chat polish with agent list routing, cached history, media restore, and model switching",
            model = "hermes-agent",
            source = "mobile",
        )

        assertTrue(sessionMatchesQuery(session, "switching"))
    }

    @Test
    fun agentSessionFilterKeepsOnlyMatchingAgentThreads() {
        val sessions = listOf(
            session(id = "agent-chat-hermes--1", title = "Hermes", model = "hermes-agent", source = "mobile"),
            session(id = "agent-chat-research--2", title = "Research", model = "hermes-agent", source = "mobile"),
            session(id = "plain-session", title = "Plain", model = "hermes-agent", source = "mobile"),
        )

        assertEquals(
            listOf("agent-chat-research--2"),
            filterSessionsForAgent(sessions, " research ").map { it.id },
        )
        assertEquals(
            listOf("agent-chat-hermes--1", "plain-session"),
            filterSessionsForAgent(sessions, " hermes ").map { it.id },
        )
        assertEquals(sessions.map { it.id }, filterSessionsForAgent(sessions, "   ").map { it.id })
    }

    private fun session(
        id: String = "session-1",
        title: String?,
        model: String,
        source: String,
    ): SessionEntity {
        return SessionEntity(
            id = id,
            title = title,
            source = source,
            startedAt = 1_700_000_000_000,
            endedAt = null,
            messageCount = 1,
            model = model,
        )
    }
}
