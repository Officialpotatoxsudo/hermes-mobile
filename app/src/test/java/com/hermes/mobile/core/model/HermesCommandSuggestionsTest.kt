package com.hermes.mobile.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HermesCommandSuggestionsTest {
    @Test
    fun suggestionsExposeHermesControlsBeyondBasicChat() {
        val commands = hermesCommandSuggestions("hermes-agent", "Hermes")
            .map { it.command.substringBefore(" ") }
            .toSet()

        assertTrue(commands.contains("/approve"))
        assertTrue(commands.contains("/deny"))
        assertTrue(commands.contains("/background"))
        assertTrue(commands.contains("/tools"))
        assertTrue(commands.contains("/skills"))
        assertTrue(commands.contains("/reasoning"))
    }

    @Test
    fun suggestionsIncludeCurrentModelShortcut() {
        val suggestions = hermesCommandSuggestions("openrouter/auto", "Auto")

        assertTrue(suggestions.any { it.command == "/model openrouter/auto" && it.description.contains("Auto") })
    }

    @Test
    fun suggestionsCleanCurrentModelShortcutText() {
        val suggestions = hermesCommandSuggestions(
            selectedModel = "\n openrouter/auto \n ignored",
            selectedLabel = "\n Auto \n ignored",
        )

        assertTrue(suggestions.any { it.command == "/model openrouter/auto" && it.description == "Models: Use Auto" })
        assertFalse(suggestions.any { it.command.contains("\n") || it.description.contains("\n") })
    }

    @Test
    fun suggestionsSkipBlankCurrentModelShortcut() {
        val suggestions = hermesCommandSuggestions("   ", "   ")

        assertFalse(suggestions.any { it.command == "/model " })
        assertFalse(suggestions.any { it.description == "Models: Use " })
    }

    @Test
    fun suggestionsFilterByTypedCommand() {
        val suggestions = hermesCommandSuggestions(
            selectedModel = "hermes-agent",
            selectedLabel = "Hermes",
            query = "/tools",
        )

        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all { it.command.startsWith("/tools") })
        assertFalse(suggestions.any { it.command == "/skills" })
    }

    @Test
    fun unknownFilterReturnsHelpfulFallback() {
        val suggestions = hermesCommandSuggestions(
            selectedModel = "hermes-agent",
            selectedLabel = "Hermes",
            query = "/nope",
        )

        assertEquals(listOf(HermesCommandSuggestion("/nope", "Run /nope", "Send custom Hermes command")), suggestions)
    }

    @Test
    fun unknownFilterUsesFirstUsefulQueryLineForFallback() {
        val suggestions = hermesCommandSuggestions(
            selectedModel = "hermes-agent",
            selectedLabel = "Hermes",
            query = "\n /nope \n ignored",
        )

        assertEquals(listOf(HermesCommandSuggestion("/nope", "Run /nope", "Send custom Hermes command")), suggestions)
    }

    @Test
    fun unknownBareFilterStillReturnsSlashCommandFallback() {
        val suggestions = hermesCommandSuggestions(
            selectedModel = "hermes-agent",
            selectedLabel = "Hermes",
            query = "nope",
        )

        assertEquals(listOf(HermesCommandSuggestion("/nope", "Run /nope", "Send custom Hermes command")), suggestions)
    }
}
