package com.hermes.mobile.core.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenStoreTest {
    @Test
    fun connectionIdentityChangesWithServerOrApiKey() {
        val first = connectionIdentityFor("https://agent.example", "api-key")
        val changedServer = connectionIdentityFor("https://other.example", "api-key")
        val changedApiKey = connectionIdentityFor("https://agent.example", "other-key")

        assertNotEquals(first, changedServer)
        assertNotEquals(first, changedApiKey)
        assertFalse(first.contains("api-key"))
        assertFalse(first.contains("https://agent.example"))
    }

    @Test
    fun savedConnectionNeedsOnlyServerUrl() {
        assertTrue(hasSavedConnection("https://agent.example"))
        assertTrue(hasSavedConnection("\n https://agent.example\nignored "))
        assertFalse(hasSavedConnection(""))
        assertFalse(hasSavedConnection("   "))
    }

    @Test
    fun savedCredentialsUseFirstCleanLine() {
        assertEquals("https://agent.example", "\n https://agent.example\nignored ".cleanSavedCredentialLine())
        assertEquals("api-key", "\n api-key\nInjected: bad ".cleanSavedCredentialLine())
        assertEquals("", "\n\n".cleanSavedCredentialLine())
    }
}
