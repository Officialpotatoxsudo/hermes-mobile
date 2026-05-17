package com.hermes.mobile.feature.sessions

import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.util.formatMessageCount

internal fun sessionDisplayTitle(title: String?): String {
    return title.cleanSessionTextLine()?.compactSessionTitle() ?: "Untitled session"
}

internal fun sessionMetadataLine(messageCount: Int, model: String, source: String): String {
    val count = formatMessageCount(messageCount)
    val detail = (model.cleanSessionTextLine() ?: source.cleanSessionTextLine())?.compactMetadataDetail()
    return detail?.let { "$count • $it" } ?: count
}

internal fun sessionMatchesQuery(session: SessionEntity, query: String): Boolean {
    val term = query.cleanSessionTextLine() ?: return true
    return listOf(
        sessionDisplayTitle(session.title),
        session.title.cleanSessionTextLine().orEmpty(),
        session.lastMessagePreview.cleanSessionTextLine().orEmpty(),
        session.source.cleanSessionTextLine().orEmpty(),
        session.model.cleanSessionTextLine().orEmpty(),
    ).any { it.contains(term, ignoreCase = true) }
}

private fun String?.cleanSessionTextLine(): String? {
    return this
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun String.compactSessionTitle(): String {
    return compactSessionText(MAX_SESSION_TITLE_LENGTH)
}

private fun String.compactMetadataDetail(): String {
    return compactSessionText(MAX_METADATA_DETAIL_LENGTH)
}

private fun String.compactSessionText(maxLength: Int): String {
    if (length <= maxLength) return this
    val head = take(maxLength - 3).trimEnd()
    if (getOrNull(head.length)?.isWhitespace() == true) return "$head..."
    val boundary = head.lastIndexOf(' ').takeIf { it >= MIN_COMPACT_WORD_BOUNDARY }
    return (boundary?.let { head.take(it) } ?: head) + "..."
}

private const val MAX_SESSION_TITLE_LENGTH = 73
private const val MAX_METADATA_DETAIL_LENGTH = 48
private const val MIN_COMPACT_WORD_BOUNDARY = 24
