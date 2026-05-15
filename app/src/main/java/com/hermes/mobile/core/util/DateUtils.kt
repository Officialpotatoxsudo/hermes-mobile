package com.hermes.mobile.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatTimestamp(timestamp: Long, pattern: String = "MMM d"): String {
    if (timestamp <= 0L) return UNKNOWN_DATE
    val millis = if (timestamp < SECONDS_TO_MILLIS_THRESHOLD) timestamp * 1000 else timestamp
    if (millis !in 1..MAX_REASONABLE_TIMESTAMP_MILLIS) return UNKNOWN_DATE
    return runCatching {
        DateTimeFormatter.ofPattern(pattern)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(millis))
    }.getOrDefault(UNKNOWN_DATE)
}

fun formatMessageCount(count: Int, qualifier: String = ""): String {
    val safeCount = count.coerceAtLeast(0)
    val label = if (safeCount == 1) "message" else "messages"
    val prefix = qualifier.cleanQualifier()?.let { "$it " }.orEmpty()
    return "$safeCount $prefix$label"
}

private fun String.cleanQualifier(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}

private const val UNKNOWN_DATE = "Unknown date"
private const val SECONDS_TO_MILLIS_THRESHOLD = 10_000_000_000L
private const val MAX_REASONABLE_TIMESTAMP_MILLIS = 253_402_300_799_999L
