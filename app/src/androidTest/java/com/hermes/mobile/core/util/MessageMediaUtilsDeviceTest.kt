package com.hermes.mobile.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMediaUtilsDeviceTest {
    @Test
    fun parsesPrettyPrintedStructuredAttachmentsOnAndroidRegexEngine() {
        val content = """
            Files are ready.

            ```json
            {
              "attachments": [
                {
                  "url": "https://cdn.example.com/reports/planning-report.pdf",
                  "name": "Planning report.pdf",
                  "mime_type": "application/pdf"
                }
              ]
            }
            ```
        """.trimIndent()

        val attachments = receivedAttachmentsFromMessage(content)

        assertEquals(listOf(ReceivedAttachmentKind.File), attachments.map { it.kind })
        assertEquals(listOf("Planning report.pdf"), attachments.map { it.label })
        assertEquals("Files are ready.", visibleReceivedAttachmentText(content))
    }
}
