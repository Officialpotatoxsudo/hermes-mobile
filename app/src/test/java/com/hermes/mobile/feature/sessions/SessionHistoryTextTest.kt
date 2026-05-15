package com.hermes.mobile.feature.sessions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionHistoryTextTest {
    @Test
    fun photoOnlyHistoryMessagesDoNotRenderExtraText() {
        assertEquals("", historyMessageText("", imageCount = 1))
        assertEquals("", historyMessageText("   ", imageCount = 2))
        assertEquals("", historyMessageText("Photo attached.", imageCount = 1))
    }

    @Test
    fun captionedHistoryMessagesKeepCaption() {
        assertEquals("Look here", historyMessageText("Look here", imageCount = 1))
    }

    @Test
    fun historyHeaderDoesNotExposeRawSessionId() {
        assertEquals(null, historyHeaderSubtitle("   "))
        assertEquals("Saved conversation", historyHeaderSubtitle("8ed3f6d3-2a7d-4e9a-b4d8-42dc9f0a6d12"))
        assertEquals("Saved conversation", historyHeaderSubtitle("\n session-1 \n ignored"))
    }

    @Test
    fun blankHistorySessionCannotContinueChat() {
        assertFalse(canContinueHistory("   "))
        assertTrue(canContinueHistory(" session-1 "))
    }

    @Test
    fun historyContinueUsesFirstUsefulSessionIdLine() {
        assertEquals(null, historyContinueSessionId("\n  \n "))
        assertEquals("session-1", historyContinueSessionId("\n session-1 \n ignored"))
    }

    @Test
    fun historyCountLabelUsesSyncAndEmptyStates() {
        assertEquals("Syncing messages", historyCountLabel(isSyncing = true, messageCount = 0))
        assertEquals("0 cached messages", historyCountLabel(isSyncing = false, messageCount = 0))
        assertEquals("1 cached message", historyCountLabel(isSyncing = false, messageCount = 1))
    }
}
