package com.hermes.mobile.core.util

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import java.net.URLDecoder
import java.util.Locale

@Serializable
data class ReceivedAttachment(
    val url: String,
    val label: String,
    val kind: ReceivedAttachmentKind,
    val mimeType: String? = null,
)

@Serializable
enum class ReceivedAttachmentKind {
    Image,
    Video,
    File,
}

fun messageImageUrisFromJson(value: String): List<String> {
    val cleanValue = value.trim()
    if (cleanValue.isBlank()) return emptyList()

    val decoded = runCatching { Json.decodeFromString<List<String>>(cleanValue) }.getOrNull()
    val uris = decoded
        ?: legacyImageUrisFromText(cleanValue).ifEmpty { listOfNotNull(cleanValue.takeIf(::isAllowedMessageImageUri)) }
    return uris.map(::cleanImageUriCandidate).filter(::isAllowedMessageImageUri).distinct()
}

fun legacyImageUrisFromText(content: String): List<String> {
    val attachmentLineIndex = content.legacyAttachmentLineIndex()
    if (attachmentLineIndex < 0) return emptyList()

    val attachmentLines = content.lineSequence()
        .drop(attachmentLineIndex + 1)
        .toList()

    val stitchedUris = attachmentLines.stitchedLegacyImageUris()
    val directUris = attachmentLines
        .flatMap { line -> legacyImageUriPattern.findAll(line).map { it.value } }
        .filterNot { directUri -> stitchedUris.any { stitchedUri -> stitchedUri != directUri && stitchedUri.startsWith(directUri) } }
    return (directUris + stitchedUris)
        .map(::cleanImageUriCandidate)
        .filter(::isAllowedMessageImageUri)
        .distinct()
}

fun formatPhotoSummary(count: Int): String {
    val safeCount = count.coerceAtLeast(0)
    return when (safeCount) {
        0 -> ""
        1 -> "Photo"
        else -> "$safeCount photos"
    }
}

fun visibleMessageText(content: String, imageCount: Int): String {
    val cleanContent = content.trim()
    if (cleanContent.isBlank()) return ""
    val displayContent = cleanContent.withoutLegacyAttachmentDetails()
    if (imageCount > 0 && isGeneratedPhotoPrompt(displayContent)) return ""
    return displayContent
}

fun readableMessageText(content: String, imageCount: Int): String {
    return visibleMessageText(content, imageCount).ifBlank { formatPhotoSummary(imageCount) }
}

fun receivedAttachmentsFromMessage(content: String): List<ReceivedAttachment> {
    if (content.isBlank()) return emptyList()

    val attachments = structuredReceivedAttachmentsFromMessage(content).toMutableList()
    val consumedRanges = mutableListOf<IntRange>()

    markdownImagePattern.findAll(content).forEach { match ->
        val label = match.groupValues.getOrNull(1).orEmpty()
        val url = match.groupValues.getOrNull(2).orEmpty()
        buildReceivedAttachment(url, label, forcedKind = ReceivedAttachmentKind.Image)?.let {
            attachments += it
            consumedRanges += match.range
        }
    }

    markdownLinkPattern.findAll(content).forEach { match ->
        if (consumedRanges.any { match.range.first in it }) return@forEach
        val label = match.groupValues.getOrNull(1).orEmpty()
        val url = match.groupValues.getOrNull(2).orEmpty()
        buildReceivedAttachment(url, label)?.let {
            attachments += it
            consumedRanges += match.range
        }
    }

    bareHttpUrlPattern.findAll(content).forEach { match ->
        if (consumedRanges.any { match.range.first in it }) return@forEach
        buildReceivedAttachment(match.value, labelHint = "")?.let { attachments += it }
    }

    return attachments.distinctBy { it.url }
}

fun receivedAttachmentFromRemoteFile(
    rawUrl: String,
    labelHint: String = "",
    mimeType: String? = null,
): ReceivedAttachment? {
    return buildReceivedAttachment(rawUrl, labelHint, mimeTypeHint = mimeType)
}

fun visibleReceivedAttachmentText(content: String): String {
    val attachments = receivedAttachmentsFromMessage(content)
    if (attachments.isEmpty()) return content.trim()

    val attachmentUrls = attachments.map { it.url }.toSet()
    var cleanContent = content
    listOf(markdownImagePattern, markdownLinkPattern).forEach { pattern ->
        cleanContent = pattern.replace(cleanContent) { match ->
            val url = match.groupValues.getOrNull(2).orEmpty().cleanReceivedAttachmentUrl()
            if (url in attachmentUrls) "" else match.value
        }
    }
    cleanContent = cleanContent.withoutStructuredAttachmentPayloads()
    cleanContent = bareHttpUrlPattern.replace(cleanContent) { match ->
        val url = match.value.cleanReceivedAttachmentUrl()
        if (url in attachmentUrls) "" else match.value
    }

    return cleanContent.lineSequence()
        .map { line ->
            val trimmed = line.trim()
            val bulletless = trimmed.trimStart('-', '*', '•').trim()
            when {
                trimmed.isStructuredAttachmentPayloadLine() -> ""
                trimmed.cleanReceivedAttachmentUrl() in attachmentUrls -> ""
                bulletless.cleanReceivedAttachmentUrl() in attachmentUrls -> ""
                else -> line
            }
        }
        .joinToString("\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

fun isAllowedMessageImageUri(value: String): Boolean {
    val cleanValue = value.trim()
    if (cleanValue.isBlank()) return false
    if (cleanValue.isAllowedDataImageUri()) return true

    if (!cleanValue.startsWith("content://", ignoreCase = true)) return false
    val authority = cleanValue.contentUriAuthority()
    return authority == HERMES_FILE_PROVIDER_AUTHORITY || authority == MEDIA_PROVIDER_AUTHORITY
}

fun deleteHermesMediaDirectory(context: Context) {
    hermesMediaDirectory(context).deleteRecursively()
}

fun deleteAppOwnedMessageMedia(context: Context, uris: Collection<String>): Int {
    val mediaDirectory = hermesMediaDirectory(context).safeCanonicalFile() ?: return 0
    var deleted = 0
    uris.forEach { rawUri ->
        val target = rawUri.appOwnedMediaFile(mediaDirectory) ?: return@forEach
        if (target.exists() && target.delete()) {
            deleted += 1
        }
    }
    return deleted
}

private fun cleanImageUriCandidate(value: String): String {
    return value
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .trimEnd(',', ';', '.', ')', ']', '}', '>')
}

private fun buildReceivedAttachment(
    rawUrl: String,
    labelHint: String,
    forcedKind: ReceivedAttachmentKind? = null,
    mimeTypeHint: String? = null,
): ReceivedAttachment? {
    val url = rawUrl.cleanReceivedAttachmentUrl()
    if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) return null
    val kind = forcedKind ?: inferReceivedAttachmentKind(url, labelHint, mimeTypeHint) ?: return null
    val label = labelHint.cleanReceivedAttachmentLabel()
        ?: url.fileNameFromUrl()
        ?: kind.defaultReceivedAttachmentLabel()
    return ReceivedAttachment(
        url = url,
        label = label,
        kind = kind,
        mimeType = mimeTypeHint.cleanMimeType() ?: mimeTypeForReceivedAttachment(kind, url),
    )
}

private fun inferReceivedAttachmentKind(url: String, labelHint: String, mimeTypeHint: String? = null): ReceivedAttachmentKind? {
    val mimeType = mimeTypeHint.cleanMimeType()?.lowercase(Locale.US)
    when {
        mimeType?.startsWith("image/") == true -> return ReceivedAttachmentKind.Image
        mimeType?.startsWith("video/") == true -> return ReceivedAttachmentKind.Video
        mimeType?.startsWith("audio/") == true -> return ReceivedAttachmentKind.File
        mimeType?.startsWith("text/") == true -> return ReceivedAttachmentKind.File
        mimeType?.startsWith("application/") == true -> return ReceivedAttachmentKind.File
    }
    val extension = (url.extensionFromUrl() ?: labelHint.extensionFromLabel()).orEmpty()
    return when (extension.lowercase(Locale.US)) {
        in imageAttachmentExtensions -> ReceivedAttachmentKind.Image
        in videoAttachmentExtensions -> ReceivedAttachmentKind.Video
        in fileAttachmentExtensions -> ReceivedAttachmentKind.File
        else -> null
    }
}

private fun structuredReceivedAttachmentsFromMessage(content: String): List<ReceivedAttachment> {
    return content.structuredAttachmentPayloadCandidates()
        .flatMap { candidate ->
            runCatching {
                Json.parseToJsonElement(candidate.payload).receivedAttachmentsFromJsonPayload()
            }.getOrDefault(emptyList())
        }
}

private fun String.isStructuredAttachmentPayloadLine(): Boolean {
    val line = trim()
    if (!line.startsWith("{") && !line.startsWith("[")) return false
    return runCatching {
        Json.parseToJsonElement(line).receivedAttachmentsFromJsonPayload().isNotEmpty()
    }.getOrDefault(false)
}

private fun String.withoutStructuredAttachmentPayloads(): String {
    val removableRanges = structuredAttachmentPayloadCandidates()
        .filter { candidate ->
            runCatching {
                Json.parseToJsonElement(candidate.payload).receivedAttachmentsFromJsonPayload().isNotEmpty()
            }.getOrDefault(false)
        }
        .map { it.range }
        .distinct()
        .sortedByDescending { it.first }
    if (removableRanges.isEmpty()) return this
    val builder = StringBuilder(this)
    removableRanges.forEach { range ->
        builder.replace(range.first, range.last + 1, "")
    }
    return builder.toString()
}

private data class StructuredPayloadCandidate(
    val payload: String,
    val range: IntRange,
)

private fun String.structuredAttachmentPayloadCandidates(): List<StructuredPayloadCandidate> {
    val candidates = mutableListOf<StructuredPayloadCandidate>()
    fencedJsonPattern.findAll(this).forEach { match ->
        val payload = match.groupValues.getOrNull(1).orEmpty().trim()
        if (payload.isNotBlank()) {
            candidates += StructuredPayloadCandidate(payload, match.range)
        }
    }
    lineSequenceWithRanges()
        .filter { it.text.trim().let { line -> line.startsWith("{") || line.startsWith("[") } }
        .forEach { line ->
            candidates += StructuredPayloadCandidate(line.text.trim(), line.range)
        }
    candidates += balancedJsonCandidates()
    return candidates.distinctBy { it.range }
}

private data class RangedLine(
    val text: String,
    val range: IntRange,
)

private fun String.lineSequenceWithRanges(): Sequence<RangedLine> = sequence {
    var start = 0
    while (start <= length) {
        val newline = indexOf('\n', start).takeIf { it >= 0 } ?: length
        val endExclusive = if (newline > start && this@lineSequenceWithRanges[newline - 1] == '\r') newline - 1 else newline
        if (endExclusive > start) {
            yield(RangedLine(substring(start, endExclusive), start until endExclusive))
        }
        if (newline == length) break
        start = newline + 1
    }
}

private fun String.balancedJsonCandidates(): List<StructuredPayloadCandidate> {
    val candidates = mutableListOf<StructuredPayloadCandidate>()
    var index = 0
    while (index < length) {
        val opener = this[index]
        if (opener != '{' && opener != '[') {
            index += 1
            continue
        }
        val closer = if (opener == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        var cursor = index
        while (cursor < length) {
            val char = this[cursor]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == opener -> depth += 1
                !inString && char == closer -> {
                    depth -= 1
                    if (depth == 0) {
                        val range = index..cursor
                        candidates += StructuredPayloadCandidate(substring(range).trim(), range)
                        index = cursor
                        break
                    }
                }
            }
            cursor += 1
        }
        index += 1
    }
    return candidates
}

private fun JsonElement.receivedAttachmentsFromJsonPayload(): List<ReceivedAttachment> {
    return when (this) {
        is JsonArray -> flatMap { it.receivedAttachmentsFromJsonPayload() }
        is JsonObject -> {
            val nested = listOfNotNull(
                this["attachments"],
                this["files"],
                this["artifacts"],
                this["data"],
            ).flatMap { it.receivedAttachmentsFromJsonPayload() }
            val direct = toReceivedAttachmentFromJson()?.let(::listOf).orEmpty()
            direct + nested
        }
        else -> emptyList()
    }
}

private fun JsonObject.toReceivedAttachmentFromJson(): ReceivedAttachment? {
    val url = firstJsonText("url", "href", "download_url", "downloadUrl", "file_url", "fileUrl", "uri")
        ?: return null
    val label = firstJsonText("name", "filename", "file_name", "fileName", "label", "title").orEmpty()
    val mimeType = firstJsonText("mime_type", "mimeType", "content_type", "contentType")
    return receivedAttachmentFromRemoteFile(url, label, mimeType)
}

private fun JsonObject.firstJsonText(vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)
            ?.contentOrNull
            ?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotBlank() }
    }
}

private fun String?.cleanMimeType(): String? {
    return this
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.contains("/") && it.length <= MAX_MIME_TYPE_LENGTH }
}

private fun String.cleanReceivedAttachmentUrl(): String {
    return trim()
        .trimEnd(',', ';', '.', ')', ']', '}', '>', '"', '\'')
}

private fun String.cleanReceivedAttachmentLabel(): String? {
    val cleanLabel = lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.trim('[', ']', '(', ')')
        ?.trim()
        ?: return null
    if (cleanLabel.startsWith("http://", ignoreCase = true) || cleanLabel.startsWith("https://", ignoreCase = true)) return null
    return cleanLabel.take(MAX_RECEIVED_ATTACHMENT_LABEL_LENGTH)
}

private fun String.fileNameFromUrl(): String? {
    val path = substringBefore('?').substringBefore('#').substringAfterLast('/')
    if (path.isBlank() || path == this) return null
    return runCatching { URLDecoder.decode(path, Charsets.UTF_8.name()) }
        .getOrDefault(path)
        .cleanReceivedAttachmentLabel()
}

private fun String.extensionFromUrl(): String? {
    return substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('/', "")
        .substringAfterLast('.', "")
        .takeIf { it.length in 2..8 && it.all(Char::isLetterOrDigit) }
}

private fun String.extensionFromLabel(): String? {
    return substringAfterLast('.', "")
        .takeIf { it.length in 2..8 && it.all(Char::isLetterOrDigit) }
}

private fun ReceivedAttachmentKind.defaultReceivedAttachmentLabel(): String {
    return when (this) {
        ReceivedAttachmentKind.Image -> "Image"
        ReceivedAttachmentKind.Video -> "Video"
        ReceivedAttachmentKind.File -> "File"
    }
}

private fun mimeTypeForReceivedAttachment(kind: ReceivedAttachmentKind, url: String): String? {
    val extension = url.extensionFromUrl()?.lowercase(Locale.US)
    return when {
        extension == "jpg" || extension == "jpeg" -> "image/jpeg"
        extension == "png" -> "image/png"
        extension == "gif" -> "image/gif"
        extension == "webp" -> "image/webp"
        extension == "mp4" || extension == "m4v" -> "video/mp4"
        extension == "mov" -> "video/quicktime"
        extension == "webm" -> "video/webm"
        extension == "pdf" -> "application/pdf"
        extension == "json" -> "application/json"
        extension == "txt" || extension == "md" -> "text/plain"
        kind == ReceivedAttachmentKind.Image -> "image/*"
        kind == ReceivedAttachmentKind.Video -> "video/*"
        else -> null
    }
}

private fun isGeneratedPhotoPrompt(value: String): Boolean {
    return value.equals("Photo attached.", ignoreCase = true) ||
        value.equals("Photo attached", ignoreCase = true)
}

private fun String.withoutLegacyAttachmentDetails(): String {
    val attachmentLineIndex = legacyAttachmentLineIndex()
    if (attachmentLineIndex < 0) return this

    return lineSequence()
        .take(attachmentLineIndex)
        .joinToString("\n")
        .trim()
}

private fun String.legacyAttachmentLineIndex(): Int {
    return lineSequence()
        .indexOfFirst { it.trimStart().isLegacyAttachmentHeader() }
}

private fun String.isLegacyAttachmentHeader(): Boolean {
    val normalized = filterNot { it.isWhitespace() }
    return normalized.equals("Attachment:", ignoreCase = true) || normalized.equals("Attachments:", ignoreCase = true)
}

private fun List<String>.stitchedLegacyImageUris(): List<String> {
    val stitched = mutableListOf<String>()
    var current: String? = null

    fun flushCurrent() {
        current?.let(stitched::add)
        current = null
    }

    forEach { rawLine ->
        val line = rawLine.trim()
        val match = legacyImageUriPattern.find(line)
        when {
            match != null -> {
                flushCurrent()
                current = match.value
            }
            current != null && line.isUriContinuation(current.orEmpty()) -> {
                current += line
            }
            else -> flushCurrent()
        }
    }
    flushCurrent()

    return stitched
}

private fun String.isUriContinuation(current: String): Boolean {
    return startsWith("/") ||
        startsWith(".") ||
        startsWith("?") ||
        startsWith("&") ||
        (current.endsWith("/") && matches(wrappedUriTailPattern))
}

private fun String.isAllowedDataImageUri(): Boolean {
    val lower = lowercase(Locale.US)
    val allowedPrefix = allowedDataImagePrefixes.firstOrNull { lower.startsWith(it) } ?: return false
    val payload = substring(allowedPrefix.length).trim()
    return payload.isNotBlank() && payload.length <= MAX_DATA_IMAGE_BASE64_CHARS
}

private fun hermesMediaDirectory(context: Context): File {
    return File(context.filesDir, HERMES_MEDIA_DIRECTORY)
}

private fun String.appOwnedMediaFile(mediaDirectory: File): File? {
    val cleanValue = trim()
    val lower = cleanValue.lowercase(Locale.US)
    return when {
        lower.startsWith("content://") && cleanValue.contentUriAuthority() == HERMES_FILE_PROVIDER_AUTHORITY -> {
            val fileName = cleanValue
                .substringAfter("://", "")
                .substringAfter("/", "")
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
                ?: return null
            File(mediaDirectory, fileName).safeCanonicalFile()
        }
        lower.startsWith("file://") -> {
            val path = cleanValue.substringAfter("://", "").takeIf { it.isNotBlank() } ?: return null
            File(path).safeCanonicalFile()
        }
        else -> null
    }?.takeIf { it.isInside(mediaDirectory) }
}

private fun String.contentUriAuthority(): String {
    return substringAfter("://", "")
        .substringBefore("/")
        .lowercase(Locale.US)
}

private fun File.safeCanonicalFile(): File? {
    return runCatching { canonicalFile }.getOrNull()
}

private fun File.isInside(directory: File): Boolean {
    val childPath = path
    val parentPath = directory.path
    return childPath == parentPath || childPath.startsWith(parentPath + File.separator)
}

private const val HERMES_MEDIA_DIRECTORY = "hermes-media"
private const val HERMES_FILE_PROVIDER_AUTHORITY = "com.hermes.mobile.fileprovider"
private const val MEDIA_PROVIDER_AUTHORITY = "media"
private const val MAX_DATA_IMAGE_BASE64_CHARS = 12 * 1024 * 1024
private val allowedDataImagePrefixes = listOf(
    "data:image/png;base64,",
    "data:image/jpeg;base64,",
    "data:image/webp;base64,",
)
private val legacyImageUriPattern = Regex("""(?i)(content://\S+|data:image/(?:png|jpeg|webp);base64,\S+)""")
private val wrappedUriTailPattern = Regex("""[A-Za-z0-9._~%+-]+""")
private const val MAX_RECEIVED_ATTACHMENT_LABEL_LENGTH = 80
private const val MAX_MIME_TYPE_LENGTH = 120
private val fencedJsonPattern = Regex("""(?is)```(?:json)?\s*(\{.*?\}|\[.*?\])\s*```""")
private val markdownImagePattern = Regex("""!\[([^\]]*)\]\((https?://[^)\s]+)\)""", RegexOption.IGNORE_CASE)
private val markdownLinkPattern = Regex("""(?<!!)\[([^\]]+)\]\((https?://[^)\s]+)\)""", RegexOption.IGNORE_CASE)
private val bareHttpUrlPattern = Regex("""https?://[^\s)>\]]+""", RegexOption.IGNORE_CASE)
private val imageAttachmentExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "avif")
private val videoAttachmentExtensions = setOf("mp4", "m4v", "mov", "webm", "mkv")
private val fileAttachmentExtensions = setOf(
    "pdf",
    "txt",
    "md",
    "csv",
    "json",
    "zip",
    "doc",
    "docx",
    "xls",
    "xlsx",
    "ppt",
    "pptx",
    "mp3",
    "m4a",
    "wav",
    "ogg",
)
