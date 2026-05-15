package com.hermes.mobile.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class HermesSlashCommandMapperTest {
    @Test
    fun mapsSessionCommandsToLocalActions() {
        assertInstanceOf(HermesSlashCommandAction.NewSession::class.java, mapHermesSlashCommand("/new"))
        assertInstanceOf(HermesSlashCommandAction.Retry::class.java, mapHermesSlashCommand("/retry"))
        assertInstanceOf(HermesSlashCommandAction.Undo::class.java, mapHermesSlashCommand("/undo"))
        assertInstanceOf(HermesSlashCommandAction.Stop::class.java, mapHermesSlashCommand("/stop"))
    }

    @Test
    fun mapsModelCommandToLocalModelSelection() {
        val action = mapHermesSlashCommand("/model openrouter/auto") as HermesSlashCommandAction.SelectModel

        assertEquals("openrouter/auto", action.requested)
    }

    @Test
    fun mapsModelCommandWithCaseAndFlexibleWhitespace() {
        val action = mapHermesSlashCommand("/MODEL\tHermes") as HermesSlashCommandAction.SelectModel

        assertEquals("Hermes", action.requested)
    }

    @Test
    fun mapsOnlyFirstUsefulCommandLine() {
        val action = mapHermesSlashCommand("\n /MODEL Hermes \n ignored") as HermesSlashCommandAction.SelectModel

        assertEquals("Hermes", action.requested)
    }

    @Test
    fun mapsDesktopInfoCommandsToAgentPrompt() {
        val action = mapHermesSlashCommand("/tools list") as HermesSlashCommandAction.AgentPrompt

        assertEquals(
            "Hermes native command requested from mobile: /tools list\nRun the equivalent Hermes action if available, then return concise output.",
            action.prompt,
        )
    }

    @Test
    fun agentPromptOmitsPastedExtraLines() {
        val action = mapHermesSlashCommand("\n /tools list \n ignored") as HermesSlashCommandAction.AgentPrompt

        assertEquals(
            "Hermes native command requested from mobile: /tools list\nRun the equivalent Hermes action if available, then return concise output.",
            action.prompt,
        )
    }

    @Test
    fun ignoresPlainMessages() {
        assertInstanceOf(HermesSlashCommandAction.None::class.java, mapHermesSlashCommand("hello"))
    }
}
