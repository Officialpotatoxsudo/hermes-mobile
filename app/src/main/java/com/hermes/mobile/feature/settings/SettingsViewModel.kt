package com.hermes.mobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.settings.LockTimeout
import com.hermes.mobile.core.settings.ThemeMode
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
    val lockTimeout: LockTimeout = LockTimeout.FiveMinutes,
    val hideChatPreviews: Boolean = true,
    val serverUrl: String = "",
    val showLogoutConfirm: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val appPreferences: AppPreferences,
    private val repository: HermesRepository,
) : ViewModel() {
    private val controls = kotlinx.coroutines.flow.MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = combine(
        appPreferences.themeMode,
        appPreferences.lockTimeout,
        appPreferences.hideChatPreviews,
        tokenStore.savedConnection,
        controls,
    ) { themeMode, lockTimeout, hideChatPreviews, savedConnection, state ->
        state.copy(
            themeMode = themeMode,
            lockTimeout = lockTimeout,
            hideChatPreviews = hideChatPreviews,
            serverUrl = savedConnection.serverUrl,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun confirmLogout() {
        controls.update { it.copy(showLogoutConfirm = true) }
    }

    fun dismissLogout() {
        controls.update { it.copy(showLogoutConfirm = false) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
        }
    }

    fun setLockTimeout(timeout: LockTimeout) {
        viewModelScope.launch {
            appPreferences.setLockTimeout(timeout)
        }
    }

    fun setHideChatPreviews(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setHideChatPreviews(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.clearLocalDataForActiveConnection()
            tokenStore.clearCredentials()
            controls.update { it.copy(showLogoutConfirm = false) }
        }
    }
}
