package com.hermes.mobile.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HermesFeatureCatalogTest {
    @Test
    fun catalogCoversRequestedHermesControlAreas() {
        val categories = hermesFeatureCatalog.map { it.id }.toSet()

        assertEquals(
            setOf(
                "chat",
                "memory",
                "runs",
                "skills",
                "tools",
                "automations",
                "platforms",
                "models",
                "security",
            ),
            categories,
        )
    }

    @Test
    fun everyActionUsesActualHermesAgentApiOrSlashCommand() {
        val actions = hermesFeatureCatalog.flatMap { it.actions }
        val actualApiPrefixes = setOf(
            "health",
            "health/detailed",
            "v1/capabilities",
            "v1/models",
            "v1/chat/completions",
            "v1/responses",
            "v1/runs",
            "api/jobs",
        )

        assertTrue(actions.size >= 22)
        actions.forEach { action ->
            assertFalse(action.title.contains("mock", ignoreCase = true), action.title)
            assertTrue(action.target.isNotBlank(), action.title)
            assertTrue(
                action.kind == HermesFeatureActionKind.Command ||
                    actualApiPrefixes.any { action.target == it || action.target.startsWith("$it?") },
                "${action.title} target ${action.target}",
            )
            assertFalse(action.target.contains("{"), "placeholder route must not be executed directly: ${action.target}")
        }
    }

    @Test
    fun createActionsHaveExecutableJsonTemplates() {
        hermesFeatureCatalog.flatMap { it.actions }
            .filter { it.kind == HermesFeatureActionKind.Create }
            .forEach { action ->
                assertTrue(action.bodyTemplate.trim().startsWith("{"), action.id)
                assertTrue(action.bodyTemplate.trim().endsWith("}"), action.id)
            }
    }
}
