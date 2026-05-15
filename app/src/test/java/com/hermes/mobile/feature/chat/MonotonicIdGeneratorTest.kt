package com.hermes.mobile.feature.chat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MonotonicIdGeneratorTest {
    @Test
    fun advancesWhenClockReturnsSameMillis() {
        val ids = MonotonicIdGenerator { 1_000L }

        assertEquals(1_000L, ids.next())
        assertEquals(1_001L, ids.next())
        assertEquals(1_002L, ids.next())
    }

    @Test
    fun seedProtectsLoadedHistoryIds() {
        val ids = MonotonicIdGenerator { 1_000L }

        ids.seed(5_000L)

        assertEquals(5_001L, ids.next())
    }

    @Test
    fun laterClockValueWins() {
        val ids = MonotonicIdGenerator { 1_000L }

        assertEquals(1_000L, ids.next())
        assertEquals(2_000L, ids.next(2_000L))
    }
}
