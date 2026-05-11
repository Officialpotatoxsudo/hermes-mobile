package com.hermes.mobile.feature.lock

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import com.hermes.mobile.core.auth.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AppLockUiState(
    val errorMessage: String? = null,
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    fun onAuthSuccess() {
        appLockManager.unlock()
        _uiState.value = AppLockUiState()
    }

    fun onAuthError(errorCode: Int, message: CharSequence) {
        val error = when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> null
            BiometricPrompt.ERROR_LOCKOUT -> "Too many attempts. Use device PIN."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometric locked. Use device PIN."
            else -> "Authentication failed: $message"
        }
        _uiState.update { it.copy(errorMessage = error) }
    }
}
