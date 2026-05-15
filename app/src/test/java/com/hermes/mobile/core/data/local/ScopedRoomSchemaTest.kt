package com.hermes.mobile.core.data.local

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScopedRoomSchemaTest {
    @Test
    fun duplicateRemoteIdsAreIsolatedAcrossAccountScopes() {
        val sessionA = session("scope-a")
        val sessionB = session("scope-b")
        val messageA = message("scope-a")
        val messageB = message("scope-b")

        assertEquals(sessionA.id, sessionB.id)
        assertEquals(messageA.id, messageB.id)
        assertEquals("scope-a", sessionA.accountScope)
        assertEquals("scope-b", sessionB.accountScope)
        assertEquals("scope-a", messageA.accountScope)
        assertEquals("scope-b", messageB.accountScope)
    }

    @Test
    fun entitiesUseScopedPrimaryKeysAndForeignKeys() {
        val sessionSource = File("src/main/java/com/hermes/mobile/core/data/local/SessionEntity.kt").readText()
        val messageSource = File("src/main/java/com/hermes/mobile/core/data/local/MessageEntity.kt").readText()

        assertTrue(sessionSource.contains("primaryKeys = [\"account_scope\", \"id\"]"))
        assertTrue(messageSource.contains("primaryKeys = [\"account_scope\", \"id\"]"))
        assertTrue(messageSource.contains("parentColumns = [\"account_scope\", \"id\"]"))
        assertTrue(messageSource.contains("childColumns = [\"account_scope\", \"session_id\"]"))
        assertTrue(messageSource.contains("onDelete = ForeignKey.CASCADE"))
    }

    private fun session(accountScope: String): SessionEntity {
        return SessionEntity(
            id = "remote-session-1",
            title = "Chat",
            source = "mobile",
            startedAt = 1L,
            endedAt = null,
            messageCount = 1,
            model = "hermes",
            accountScope = accountScope,
        )
    }

    private fun message(accountScope: String): MessageEntity {
        return MessageEntity(
            id = 10L,
            sessionId = "remote-session-1",
            role = "user",
            content = "hello",
            timestamp = 1L,
            accountScope = accountScope,
        )
    }
}
