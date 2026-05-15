package com.hermes.mobile.core.util

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

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
    return length > allowedPrefix.length
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
private val allowedDataImagePrefixes = listOf(
    "data:image/png;base64,",
    "data:image/jpeg;base64,",
    "data:image/webp;base64,",
)
private val legacyImageUriPattern = Regex("""(?i)(content://\S+|data:image/(?:png|jpeg|webp);base64,\S+)""")
private val wrappedUriTailPattern = Regex("""[A-Za-z0-9._~%+-]+""")
