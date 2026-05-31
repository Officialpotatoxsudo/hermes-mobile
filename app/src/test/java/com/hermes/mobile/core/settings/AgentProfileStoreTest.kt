package com.hermes.mobile.core.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class AgentProfileStoreTest {
    @Test
    fun buildsCleanCustomAgentProfile() {
        val profile = buildCustomAgentProfile(
            id = "\n agent-1\nagent-2 ",
            name = "\n Researcher\nIgnored ",
            subtitle = "\n Search and summarize\nIgnored ",
            instructions = "\n Use reliable sources.\nIgnored ",
        )

        assertEquals("agent-1", profile?.id)
        assertEquals("Researcher", profile?.name)
        assertEquals("Search and summarize", profile?.subtitle)
        assertEquals("Use reliable sources.", profile?.instructions)
        assertEquals("R", profile?.initial)
    }

    @Test
    fun cappedCustomAgentTextDoesNotKeepTrailingSpace() {
        val profile = buildCustomAgentProfile(
            id = "agent-1",
            name = "a".repeat(39) + " Name",
            subtitle = "b".repeat(95) + " Role",
        )

        assertEquals("a".repeat(39), profile?.name)
        assertEquals("b".repeat(95), profile?.subtitle)
    }

    @Test
    fun cappedCustomAgentNameUsesWordBoundary() {
        val profile = buildCustomAgentProfile(
            id = "agent-1",
            name = "Research assistant for mobile product support",
            subtitle = "Role",
        )

        assertEquals("Research assistant for mobile product", profile?.name)
    }

    @Test
    fun rejectsBlankAgentName() {
        assertNull(buildCustomAgentProfile("agent-1", "   ", "Role"))
        assertNull(buildCustomAgentProfile("agent-1", "\n\n", "Role"))
        assertNull(buildCustomAgentProfile("   ", "Agent", "Role"))
    }

    @Test
    fun agentInitialSkipsLeadingPunctuation() {
        assertEquals("R", buildCustomAgentProfile("agent-1", "-- Researcher", "Role")?.initial)
        assertEquals("A", buildCustomAgentProfile("agent-2", "!!!", "Role")?.initial)
    }

    @Test
    fun customAgentIdUsesUuid() {
        val uuid = UUID.fromString("00000000-0000-0000-0000-000000000123")

        assertEquals("agent-00000000-0000-0000-0000-000000000123", newCustomAgentId(uuid))
    }

    @Test
    fun storedAgentsAreSanitizedBeforeDisplay() {
        val default = AppPreferences.defaultAgents.first()
        val stored = listOf(
            default.copy(name = ""),
            AgentProfile("\n agent-1\nagent-2 ", "\n -- Researcher\nIgnored ", "\n Role\nIgnored ", "?"),
            AgentProfile("agent-2", "   ", "Hidden", "H"),
        )

        assertEquals(
            listOf(
                default,
                AgentProfile("agent-1", "-- Researcher", "Role", "R"),
            ),
            mergeStoredAgentProfiles(stored),
        )
    }

    @Test
    fun storedAgentsKeepAvatarUriAfterSanitizing() {
        val stored = listOf(
            AgentProfile(
                id = " agent-1 ",
                name = " Researcher ",
                subtitle = " Role ",
                initial = "R",
                avatarUri = "content://avatar/1",
                instructions = " Use project context ",
            ),
        )

        val sanitized = mergeStoredAgentProfiles(stored).last()

        assertEquals("content://avatar/1", sanitized.avatarUri)
        assertEquals("Use project context", sanitized.instructions)
    }

    @Test
    fun upsertsCustomAgentWithoutDuplicating() {
        val current = listOf(AgentProfile("agent-1", "Old", "Old role", "O"))
        val updated = buildCustomAgentProfile("agent-1", "New", "New role")!!

        assertEquals(listOf(updated), upsertCustomAgent(current, updated))
    }

    @Test
    fun updatesCustomAgentWithoutReorderingList() {
        val first = AgentProfile("agent-1", "Old", "Old role", "O")
        val second = AgentProfile("agent-2", "Second", "Role", "S")
        val updated = buildCustomAgentProfile("agent-1", "New", "New role")!!

        assertEquals(listOf(updated, second), upsertCustomAgent(listOf(first, second), updated))
    }

    @Test
    fun upsertPreservesExistingInstructionsWhenNotProvided() {
        val current = listOf(
            AgentProfile(
                id = "agent-1",
                name = "Old",
                subtitle = "Old role",
                initial = "O",
                instructions = "Keep this system prompt",
            ),
        )
        val updated = buildCustomAgentProfile("agent-1", "New", "New role")!!

        assertEquals("Keep this system prompt", upsertCustomAgent(current, updated).single().instructions)
    }

    @Test
    fun upsertSanitizesCurrentAgentsBeforeWriting() {
        val current = listOf(
            AppPreferences.defaultAgents.first().copy(name = ""),
            AgentProfile(" agent-1 ", " Old ", " ", "?"),
            AgentProfile("agent-2", "   ", "Hidden", "H"),
        )
        val updated = buildCustomAgentProfile("agent-1", "New", "New role")!!

        assertEquals(listOf(updated), upsertCustomAgent(current, updated))
    }

    @Test
    fun removesOnlyCustomAgents() {
        val custom = AgentProfile("agent-1", "Agent", "Role", "A")
        val default = AppPreferences.defaultAgents.first()

        assertEquals(emptyList<AgentProfile>(), removeCustomAgent(listOf(default, custom), "\n agent-1\nagent-2 "))
        assertEquals(listOf(custom), removeCustomAgent(listOf(default, custom), default.id))
    }
}
