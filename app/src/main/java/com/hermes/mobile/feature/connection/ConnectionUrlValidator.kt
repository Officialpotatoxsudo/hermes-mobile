package com.hermes.mobile.feature.connection

import java.net.URI

fun normalizeHermesServerUrl(rawUrl: String): String? {
    val trimmed = rawUrl.cleanUrlInput().trimEnd('/')
    if (!trimmed.hasScheme() && trimmed.hasAnyScheme()) return null
    val value = if (trimmed.hasScheme()) trimmed else trimmed.withDefaultScheme()
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    if (uri.rawUserInfo != null) return null
    if (uri.port > 65_535) return null
    val host = uri.host?.asLocalHostKey().orEmpty()
    val cleanValue = uri.withoutQueryOrFragment()
    return when {
        uri.scheme.equals("https", ignoreCase = true) && host.isNotBlank() -> cleanValue
        uri.scheme.equals("http", ignoreCase = true) && host in localHttpHosts -> cleanValue
        else -> null
    }
}

private fun String.cleanUrlInput(): String {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
}

private fun String.hasScheme(): Boolean = startsWith("http://", ignoreCase = true) ||
    startsWith("https://", ignoreCase = true)

private fun String.hasAnyScheme(): Boolean = schemePattern.containsMatchIn(this)

private fun String.withDefaultScheme(): String {
    if (this == "::1" || startsWith("::1:") || startsWith("::1/") || startsWith("::1?") || startsWith("::1#")) {
        return "http://[::1]${removePrefix("::1")}"
    }
    val host = hostToken().asLocalHostKey()
    return if (host in localHttpHosts) "http://$this" else "https://$this"
}

private fun String.hostToken(): String {
    if (startsWith("[")) return substringAfter("[").substringBefore("]")
    return substringBefore(":")
        .substringBefore("/")
        .substringBefore("?")
        .substringBefore("#")
}

private fun String.asLocalHostKey(): String = lowercase()
    .removePrefix("[")
    .removeSuffix("]")

private fun URI.withoutQueryOrFragment(): String {
    val authority = rawAuthority ?: return toString()
    val path = rawPath.orEmpty().trimEnd('/')
    return buildString {
        append(scheme.lowercase())
        append("://")
        append(authority)
        append(path)
    }
}

private val localHttpHosts = setOf(
    "localhost",
    "127.0.0.1",
    "10.0.2.2",
    "::1",
)

private val schemePattern = Regex("""^[A-Za-z][A-Za-z0-9+.-]*://""")
