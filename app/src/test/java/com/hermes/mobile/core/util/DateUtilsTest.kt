package com.hermes.mobile.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateUtilsTest {
    @Test
    fun formatsMissingTimestampAsUnknownDate() {
        assertEquals("Unknown date", formatTimestamp(0))
        assertEquals("Unknown date", formatTimestamp(-1))
    }

    @Test
    fun formatsUnreasonableFutureTimestampAsUnknownDate() {
        assertEquals("Unknown date", formatTimestamp(Long.MAX_VALUE))
        assertEquals("Unknown date", formatTimestamp(253_402_300_800_000L))
    }

    @Test
    fun formatsMessageCountWithSingularPluralLabel() {
        assertEquals("0 messages", formatMessageCount(0))
        assertEquals("0 messages", formatMessageCount(-1))
        assertEquals("1 message", formatMessageCount(1))
        assertEquals("2 messages", formatMessageCount(2))
        assertEquals("1 cached message", formatMessageCount(1, qualifier = "cached"))
        assertEquals("2 cached messages", formatMessageCount(2, qualifier = "cached"))
    }

    @Test
    fun formatsMessageCountWithFirstUsefulQualifierLine() {
        assertEquals("2 cached messages", formatMessageCount(2, qualifier = "\n cached \nignored"))
        assertEquals("2 messages", formatMessageCount(2, qualifier = "\n  \n "))
    }
}
