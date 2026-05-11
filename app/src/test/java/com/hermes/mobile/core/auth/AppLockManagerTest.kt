package com.hermes.mobile.core.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppLockManagerTest {
    @Test
    fun unlockedSessionExpiresAfterTimeout() {
        var now = 1_000L
        val manager = AppLockManager()
        manager.setClock { now }
        manager.setLockTimeout(5_000L)

        manager.unlock()
        manager.onAppBackgrounded()
        now += 5_000L

        assertTrue(manager.isSessionExpired())
    }

    @Test
    fun foregroundClearsBackgroundTimestamp() {
        var now = 1_000L
        val manager = AppLockManager()
        manager.setClock { now }
        manager.setLockTimeout(5_000L)

        manager.unlock()
        manager.onAppBackgrounded()
        manager.onAppForegrounded()
        now += 10_000L

        assertFalse(manager.isSessionExpired())
    }
}
