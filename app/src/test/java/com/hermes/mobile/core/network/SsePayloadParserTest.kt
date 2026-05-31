package com.hermes.mobile.core.network

import com.hermes.mobile.core.util.ReceivedAttachmentKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SsePayloadParserTest {
    private val parser = SsePayloadParser(Json { ignoreUnknownKeys = true })

    @Test
    fun parsesStreamingChunk() {
        val chunk = parser.parseChunk(
            """{"choices":[{"delta":{"content":"hi"}}],"usage":{"prompt_tokens":1,"completion_tokens":2,"total_tokens":3}}""",
        )

        assertEquals("hi", chunk?.choices?.first()?.delta?.content)
        assertEquals(3, chunk?.usage?.totalTokens)
    }

    @Test
    fun parsesReasoningFromStreamingChunk() {
        val chunk = parser.parseChunk(
            """{"choices":[{"delta":{"reasoning_content":"checked tools","content":"done"}}]}""",
        )

        assertEquals("checked tools", chunk?.choices?.first()?.delta?.reasoningText)
        assertEquals("done", chunk?.choices?.first()?.delta?.content)
    }

    @Test
    fun parsesStandaloneReasoningEventPayload() {
        assertEquals("thinking through files", parser.parseReasoning("""{"delta":"thinking through files"}"""))
        assertEquals("tool plan", parser.parseReasoning("""{"reasoning":{"message":"tool plan"}}"""))
    }

    @Test
    fun standaloneReasoningKeepsMultilineText() {
        val reasoning = parser.parseReasoning(
            """
                {"reasoning":"line one\nline two\n${"x".repeat(220)}"}
            """.trimIndent(),
        )

        assertEquals("line one\nline two\n${"x".repeat(220)}", reasoning)
    }

    @Test
    fun parsesStandaloneAttachmentEventPayload() {
        val attachment = parser.parseAttachment(
            """{"url":"https://cdn.example.com/report.pdf","name":"Report.pdf","mime_type":"application/pdf"}""",
        )

        assertEquals("https://cdn.example.com/report.pdf", attachment?.url)
        assertEquals("Report.pdf", attachment?.label)
        assertEquals(ReceivedAttachmentKind.File, attachment?.kind)
        assertEquals("application/pdf", attachment?.mimeType)
    }

    @Test
    fun parsesNestedAttachmentEventPayload() {
        val attachment = parser.parseAttachment(
            """{"attachments":[{"url":"https://cdn.example.com/report.pdf","name":"Report.pdf","mime_type":"application/pdf"}]}""",
        )

        assertEquals("https://cdn.example.com/report.pdf", attachment?.url)
        assertEquals("Report.pdf", attachment?.label)
    }

    @Test
    fun parsesNonStreamingCompletionFallback() {
        val completion = parser.parseCompletion(
            """{"choices":[{"message":{"role":"assistant","content":"hello"}}],"usage":{"prompt_tokens":4,"completion_tokens":5,"total_tokens":9}}""",
        )

        assertEquals("hello", completion?.choices?.first()?.message?.content)
        assertEquals(9, completion?.usage?.totalTokens)
    }

    @Test
    fun parsesNestedOpenAiErrorMessage() {
        val error = parser.parseError(
            """{"error":{"message":"model not available","type":"invalid_request_error"}}""",
        )

        assertEquals("model not available", error)
    }

    @Test
    fun skipsBlankNestedErrorMessage() {
        val error = parser.parseError(
            """{"error":{"message":"   ","detail":" retry later "}}""",
        )

        assertEquals("retry later", error)
    }

    @Test
    fun trimsRawErrorFallback() {
        assertEquals("upstream died", parser.parseError("   upstream died   "))
    }

    @Test
    fun rawErrorFallbackUsesFirstUsefulLine() {
        assertEquals("upstream died", parser.parseError("\n  upstream died  \nstack trace line"))
    }

    @Test
    fun jsonErrorMessageUsesFirstUsefulLine() {
        val error = parser.parseError(
            """{"error":{"message":"\n model not available \n details later"}}""",
        )

        assertEquals("model not available", error)
    }

    @Test
    fun rawErrorFallbackCapsLongText() {
        assertEquals(200, parser.parseError("x".repeat(240)).length)
    }

    @Test
    fun rawErrorFallbackCapsAtWordBoundary() {
        val error = parser.parseError("word ".repeat(39) + "tailword")

        assertEquals("word ".repeat(39).trim(), error)
    }

    @Test
    fun blankErrorFallbackIsReadable() {
        assertEquals("Unknown server error", parser.parseError("   "))
    }

    @Test
    fun parsesNestedToolProgressPayload() {
        val progress = parser.parseTool(
            """{"tool":{"name":"browser"},"status":{"message":"running"}}""",
        )

        assertEquals("browser", progress?.label)
        assertEquals("browser", progress?.tool)
        assertEquals("running", progress?.status)
    }

    @Test
    fun trimsToolProgressTextFields() {
        val progress = parser.parseTool(
            """{"label":"   ","message":" indexing ","status":{"message":" running "}}""",
        )

        assertEquals("indexing", progress?.label)
        assertEquals("running", progress?.status)
    }

    @Test
    fun toolProgressUsesFirstUsefulLine() {
        val progress = parser.parseTool(
            """{"message":"\n indexing \n ignored","status":{"message":"\n running \n ignored"}}""",
        )

        assertEquals("indexing", progress?.label)
        assertEquals("running", progress?.status)
    }
}
