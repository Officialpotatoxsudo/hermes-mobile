package com.hermes.mobile.core.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenStoreTest {
    @Test
    fun connectionIdentityFollowsAgentKeyAcrossServerUrls() {
        val first = connectionIdentityFor("https://agent.example", "api-key")
        val changedServer = connectionIdentityFor("https://other.example", "api-key")
        val changedApiKey = connectionIdentityFor("https://agent.example", "other-key")

        assertEquals(first, changedServer)
        assertNotEquals(first, changedApiKey)
        assertFalse(first.contains("api-key"))
        assertFalse(first.contains("https://agent.example"))
    }

    @Test
    fun savedConnectionNeedsServerUrlAndApiKey() {
        assertTrue(hasSavedConnection("https://agent.example", "api-key"))
        assertTrue(hasSavedConnection("\n https://agent.example\nignored ", "\n api-key\nignored "))
        assertFalse(hasSavedConnection("", "api-key"))
        assertFalse(hasSavedConnection("   ", "api-key"))
        assertFalse(hasSavedConnection("https://agent.example", ""))
        assertFalse(hasSavedConnection("https://agent.example", "   "))
    }

    @Test
    fun savedCredentialsUseFirstCleanLine() {
        assertEquals("https://agent.example", "\n https://agent.example\nignored ".cleanSavedCredentialLine())
        assertEquals("api-key", "\n api-key\nInjected: bad ".cleanSavedCredentialLine())
        assertEquals("", "\n\n".cleanSavedCredentialLine())
    }
}
