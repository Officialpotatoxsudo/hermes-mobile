package com.hermes.mobile.feature.chat

import com.hermes.mobile.core.network.ConnectionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ChatPromptFormatterTest {
    @Test
    fun imageAttachmentDoesNotPolluteDisplayedMessage() {
        val payload = buildChatPromptPayload(
            prompt = "What is in this photo?",
            attachments = listOf(
                ChatAttachment(
                    id = "image-1",
                    label = "Photo",
                    uri = "content://media/picker/0/photo",
                    kind = "image",
                ),
            ),
        )

        assertEquals("What is in this photo?", payload.displayPrompt)
        assertFalse(payload.displayPrompt.contains("Attachments:"))
        assertFalse(payload.displayPrompt.contains("content://"))
        assertFalse(payload.apiPrompt.contains("content://"))
    }

    @Test
    fun photoOnlyMessageDisplaysOnlyPhoto() {
        val payload = buildChatPromptPayload(
            prompt = "",
            attachments = listOf(
                ChatAttachment(
                    id = "image-1",
                    label = "Photo",
                    uri = "content://media/picker/0/photo",
                    kind = "image",
                ),
            ),
        )

        assertEquals("", payload.displayPrompt)
        assertEquals("Photo attached.", payload.apiPrompt)
    }

    @Test
    fun imageMimeAttachmentDisplaysOnlyPhoto() {
        val payload = buildChatPromptPayload(
            prompt = "",
            attachments = listOf(
                ChatAttachment(
                    id = "image-1",
                    label = "Photo",
                    uri = "content://media/picker/0/photo",
                    kind = "image/png",
                ),
            ),
        )

        assertEquals("", payload.displayPrompt)
        assertEquals("Photo attached.", payload.apiPrompt)
    }

    @Test
    fun voiceOnlyMessageDisplaysCompactVoiceLabel() {
        val recording = File.createTempFile("voice-note", ".m4a")
        try {
            recording.writeBytes(byteArrayOf(1, 2, 3))

            val payload = buildChatPromptPayload(
                prompt = "",
                attachments = listOf(
                    ChatAttachment(
                        id = "voice-1",
                        label = "Voice note",
                        uri = "file://${recording.absolutePath}",
                        kind = "voice",
                    ),
                ),
            )

            assertEquals("Voice note", payload.displayPrompt)
            assertTrue(payload.apiPrompt.startsWith("Voice note attached: data:audio/mp4;base64,"))
            assertTrue(payload.apiPrompt.endsWith("AQID"))
            assertFalse(payload.apiPrompt.contains(recording.absolutePath))
        } finally {
            recording.delete()
        }
    }

    @Test
    fun fileAttachmentLabelTrimsForDisplayAndPrompt() {
        val payload = buildChatPromptPayload(
            prompt = "",
            attachments = listOf(
                ChatAttachment(
                    id = "file-1",
                    label = "  report.pdf  ",
                    uri = "content://docs/report",
                    kind = "file",
                ),
            ),
        )

        assertEquals("report.pdf", payload.displayPrompt)
        assertEquals("report.pdf attached.", payload.apiPrompt)
    }

    @Test
    fun fileAttachmentLabelUsesFirstUsefulLineForDisplayAndPrompt() {
        val payload = buildChatPromptPayload(
            prompt = "",
            attachments = listOf(
                ChatAttachment(
                    id = "file-1",
                    label = "\n report.pdf \n ignored",
                    uri = "content://docs/report",
                    kind = "file",
                ),
            ),
        )

        assertEquals("report.pdf", payload.displayPrompt)
        assertEquals("report.pdf attached.", payload.apiPrompt)
    }

    @Test
    fun longFileAttachmentLabelUsesCompactDisplayAndPrompt() {
        val payload = buildChatPromptPayload(
            prompt = "",
            attachments = listOf(
                ChatAttachment(
                    id = "file-1",
                    label = "Quarterly planning report with annotations and screen captures.pdf",
                    uri = "content://docs/report",
                    kind = "file",
                ),
            ),
        )

        assertEquals("Quarterly planning report with annotations...", payload.displayPrompt)
        assertEquals("Quarterly planning report with annotations... attached.", payload.apiPrompt)
    }

    @Test
    fun invalidChatTimestampUsesPlaceholder() {
        assertEquals("--:--", formatChatClockTime(0))
        assertEquals("--:--", formatChatClockTime(-1))
        assertEquals("--:--", formatChatClockTime(Long.MAX_VALUE))
        assertEquals("--:--", formatChatClockTime(253_402_300_800_000L))
    }

    @Test
    fun chatStatusUsesActivityBeforeConnectionLabel() {
        assertEquals(
            "researching",
            chatStatusLabel(ConnectionState.Connecting, isConnecting = true, isStreaming = false, queuedCount = 0, activity = "researching"),
        )
        assertEquals(
            "using browser · running",
            chatStatusLabel(ConnectionState.Connecting, isConnecting = true, isStreaming = false, queuedCount = 0, activity = "using browser · running"),
        )
    }

    @Test
    fun photoOnlySessionTitleUsesPhotoChat() {
        val photoMessage = ChatUiMessage(
            id = "user-1",
            role = "user",
            content = "Photo attached.",
            time = "10:00",
            imageUris = listOf("content://media/picker/0/photo/1"),
        )

        assertEquals("Photo chat", deriveChatSessionTitle("Hermes", null, listOf(photoMessage)))
        assertEquals("Caption", deriveChatSessionTitle("Hermes", null, listOf(photoMessage.copy(content = "Caption"))))
    }

    @Test
    fun longSessionTitleUsesWordBoundaryPreview() {
        val message = ChatUiMessage(
            id = "user-1",
            role = "user",
            content = "Summarize onboarding plan for mobile product reliability improvementstogether across chat session history",
            time = "10:00",
        )

        assertEquals(
            "Summarize onboarding plan for mobile product reliability...",
            deriveChatSessionTitle("Hermes", null, listOf(message)),
        )
    }

    @Test
    fun multilineSessionTitleUsesFirstUsefulLine() {
        val message = ChatUiMessage(
            id = "user-1",
            role = "user",
            content = "\n Plan launch follow-up\nInclude design notes and QA backlog",
            time = "10:00",
        )

        assertEquals("Plan launch follow-up", deriveChatSessionTitle("Hermes", null, listOf(message)))
    }

    @Test
    fun restoredPhotoAttachmentsDeduplicateUris() {
        val attachments = restoredPhotoAttachments(
            listOf(" content://media/picker/0/photo/1 ", "content://media/picker/0/photo/1", "", "content://media/picker/0/photo/2"),
        )

        assertEquals(listOf("content://media/picker/0/photo/1", "content://media/picker/0/photo/2"), attachments.map { it.uri })
        assertEquals(listOf("Photo 1", "Photo 2"), attachments.map { it.label })
        assertEquals(listOf("image", "image"), attachments.map { it.kind })
    }

    @Test
    fun mediaOnlyMessagesHaveReadableFallbackText() {
        val photoMessage = ChatUiMessage(
            id = "user-1",
            role = "user",
            content = "",
            time = "10:00",
            imageUris = listOf("content://media/picker/0/photo/1"),
        )
        val photoQueueItem = PendingPrompt(
            id = "queued-1",
            text = "Photo attached.",
            displayText = "",
            imageUris = listOf("content://media/picker/0/photo/1", "content://media/picker/0/photo/2"),
        )

        assertEquals("Photo", photoMessage.readableContent())
        assertEquals("Photo", photoMessage.previewContent())
        assertEquals("Photo", photoMessage.copy(content = "Photo attached.").readableContent())
        assertEquals("2 photos", photoQueueItem.readableContent())
        assertEquals("2 photos", photoQueueItem.copy(displayText = "Photo attached.").readableContent())
        assertEquals("Caption", photoMessage.copy(content = "Caption").readableContent())
        assertEquals(
            "Streaming...",
            photoMessage.copy(content = "", imageUris = emptyList()).previewContent(),
        )
    }

    @Test
    fun legacyAttachmentDetailsAreHiddenWithoutImageMetadata() {
        val message = ChatUiMessage(
            id = "history-1",
            role = "user",
            content = """
                What is in this photo?

                Attachments:
                • image: Photo:
                content://media/picker/0/photo
            """.trimIndent(),
            time = "10:00",
        )

        assertEquals("What is in this photo?", message.readableContent())
        assertFalse(message.readableContent().contains("content://"))
    }
}
