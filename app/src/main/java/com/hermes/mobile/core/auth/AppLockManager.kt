package com.hermes.mobile.core.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor() {
    companion object {
        const val DEFAULT_LOCK_TIMEOUT_MS: Long = 5 * 60 * 1000L
    }

    @Volatile
    private var unlocked = false

    @Volatile
    private var backgroundedAt = 0L

    @Volatile
    private var lockTimeoutMs = DEFAULT_LOCK_TIMEOUT_MS

    private var now: () -> Long = { System.currentTimeMillis() }

    val isUnlocked: Boolean
        get() = unlocked

    fun unlock() {
        unlocked = true
        backgroundedAt = 0L
    }

    fun lock() {
        unlocked = false
    }

    fun onAppBackgrounded() {
        if (backgroundedAt == 0L) {
            backgroundedAt = now()
        }
    }

    fun onAppForegrounded() {
        backgroundedAt = 0L
    }

    fun isSessionExpired(): Boolean {
        if (!unlocked) return true
        val lastBackgroundedAt = backgroundedAt
        if (lastBackgroundedAt == 0L) return false
        return now() - lastBackgroundedAt >= lockTimeoutMs
    }

    fun setLockTimeout(timeoutMs: Long) {
        lockTimeoutMs = timeoutMs.coerceAtLeast(0L)
    }

    internal fun setClock(clock: () -> Long) {
        now = clock
    }
}
