package com.hermes.mobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.LockTimeout
import com.hermes.mobile.core.settings.ThemeMode
import com.hermes.mobile.core.settings.VisualStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val visualStyle: VisualStyle = VisualStyle.Normal,
    val liquidGlassConfig: LiquidGlassConfig = LiquidGlassConfig(),
    val appLockEnabled: Boolean = true,
    val lockTimeout: LockTimeout = LockTimeout.FiveMinutes,
    val hideChatPreviews: Boolean = true,
    val serverUrl: String = "",
    val showLogoutConfirm: Boolean = false,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val appPreferences: AppPreferences,
    private val repository: HermesRepository,
) : ViewModel() {
    private val controls = kotlinx.coroutines.flow.MutableStateFlow(SettingsUiState())
    private val appearanceState = combine(
        appPreferences.themeMode,
        appPreferences.visualStyle,
        appPreferences.liquidGlassConfig,
    ) { themeMode, visualStyle, liquidGlassConfig ->
        SettingsUiState(
            themeMode = themeMode,
            visualStyle = visualStyle,
            liquidGlassConfig = liquidGlassConfig.coerced(),
        )
    }
    private val securityState = combine(
        appPreferences.appLockEnabled,
        appPreferences.lockTimeout,
        appPreferences.hideChatPreviews,
    ) { appLockEnabled, lockTimeout, hideChatPreviews ->
        SettingsUiState(
            appLockEnabled = appLockEnabled,
            lockTimeout = lockTimeout,
            hideChatPreviews = hideChatPreviews,
        )
    }
    private val preferenceState = combine(
        appearanceState,
        securityState,
    ) { appearance, security ->
        appearance.copy(
            appLockEnabled = security.appLockEnabled,
            lockTimeout = security.lockTimeout,
            hideChatPreviews = security.hideChatPreviews,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferenceState,
        tokenStore.savedConnection,
        controls,
    ) { preferences, savedConnection, state ->
        state.copy(
            themeMode = preferences.themeMode,
            visualStyle = preferences.visualStyle,
            liquidGlassConfig = preferences.liquidGlassConfig,
            appLockEnabled = preferences.appLockEnabled,
            lockTimeout = preferences.lockTimeout,
            hideChatPreviews = preferences.hideChatPreviews,
            serverUrl = savedConnection.serverUrl,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun confirmLogout() {
        controls.update { it.copy(showLogoutConfirm = true, logoutError = null) }
    }

    fun dismissLogout() {
        controls.update { it.copy(showLogoutConfirm = false, logoutError = null) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
        }
    }

    fun setVisualStyle(style: VisualStyle) {
        viewModelScope.launch {
            appPreferences.setVisualStyle(style)
        }
    }

    fun updateLiquidGlassConfig(transform: (LiquidGlassConfig) -> LiquidGlassConfig) {
        viewModelScope.launch {
            appPreferences.setLiquidGlassConfig(transform(uiState.value.liquidGlassConfig).coerced())
        }
    }

    fun resetLiquidGlassConfig() {
        viewModelScope.launch {
            appPreferences.resetLiquidGlassConfig()
        }
    }

    fun setLockTimeout(timeout: LockTimeout) {
        viewModelScope.launch {
            appPreferences.setLockTimeout(timeout)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAppLockEnabled(enabled)
        }
    }

    fun setHideChatPreviews(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setHideChatPreviews(enabled)
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        if (controls.value.isLoggingOut) return
        controls.update { it.copy(isLoggingOut = true, logoutError = null) }
        viewModelScope.launch {
            runCatching {
                repository.clearLocalDataForActiveConnection()
                tokenStore.clearCredentials()
            }.onSuccess {
                controls.update { it.copy(showLogoutConfirm = false, isLoggingOut = false, logoutError = null) }
                onComplete()
            }.onFailure { error ->
                controls.update {
                    it.copy(
                        isLoggingOut = false,
                        logoutError = error.message ?: "Could not log out. Try again.",
                    )
                }
            }
        }
    }
}
