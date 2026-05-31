package com.hermes.mobile.feature.agent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgentControlResultRendererTest {
    @Test
    fun structuredJsonObjectRendersAsFields() {
        val result = renderControlResult(
            """{"status":"ok","features":["chat","files"],"limits":{"max_files":4}}""",
        )

        assertTrue(result is RenderedControlResult.Structured)
        val structured = result as RenderedControlResult.Structured

        assertEquals("JSON response", structured.title)
        assertEquals(listOf("status", "features", "limits"), structured.fields.map { it.key })
        assertEquals("ok", structured.fields[0].value)
        assertEquals("chat, files", structured.fields[1].value)
        assertEquals("max_files: 4", structured.fields[2].value)
    }

    @Test
    fun plainTextControlResultRemainsReadableText() {
        val result = renderControlResult("ok")

        assertEquals(RenderedControlResult.Text("ok"), result)
    }
}
