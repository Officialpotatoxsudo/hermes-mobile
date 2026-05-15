package com.hermes.mobile.feature.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.network.safeHeaderLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionSetupUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val isEditingExistingConnection: Boolean = false,
    val isChecking: Boolean = false,
    val isHealthy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ConnectionSetupViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionSetupUiState())
    val uiState: StateFlow<ConnectionSetupUiState> = _uiState.asStateFlow()
    private var existingApiKey: String = ""

    init {
        viewModelScope.launch {
            val savedConnection = tokenStore.savedConnectionOnce()
            if (savedConnection.serverUrl.isNotBlank()) {
                existingApiKey = savedConnection.apiKey
                _uiState.update { state ->
                    if (state.serverUrl.isBlank() && state.apiKey.isBlank()) {
                        state.copy(
                            serverUrl = savedConnection.serverUrl,
                            isEditingExistingConnection = true,
                        )
                    } else {
                        state.copy(isEditingExistingConnection = true)
                    }
                }
            }
        }
    }

    fun onServerUrlChanged(value: String) {
        _uiState.update { it.copy(serverUrl = value, error = null, isChecking = false, isHealthy = false) }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.update { it.copy(apiKey = value, error = null, isChecking = false, isHealthy = false) }
    }

    fun checkAndSave(onSaved: () -> Unit) {
        val state = _uiState.value
        val normalizedUrl = normalizeHermesServerUrl(state.serverUrl)
        when {
            normalizedUrl == null -> {
                _uiState.update { it.copy(error = "Enter HTTPS URL, or local HTTP URL for emulator") }
            }
            else -> {
                viewModelScope.launch {
                    val cleanApiKeyInput = state.apiKey.safeHeaderLine()
                    val effectiveApiKey = when {
                        cleanApiKeyInput.isNotBlank() -> cleanApiKeyInput
                        state.isEditingExistingConnection && existingApiKey.isNotBlank() -> existingApiKey
                        else -> {
                            _uiState.update { it.copy(error = "Enter API key", isChecking = false, isHealthy = false) }
                            return@launch
                        }
                    }
                    _uiState.update {
                        it.copy(
                            serverUrl = normalizedUrl,
                            apiKey = cleanApiKeyInput,
                            isChecking = true,
                            error = null,
                            isHealthy = false,
                        )
                    }
                    repository.checkHealth(normalizedUrl, effectiveApiKey)
                        .onSuccess {
                            if (!isLatestConnectionAttempt(normalizedUrl, cleanApiKeyInput)) return@onSuccess
                            tokenStore.saveCredentials(normalizedUrl, effectiveApiKey)
                            existingApiKey = effectiveApiKey
                            _uiState.update { it.copy(isChecking = false, isHealthy = true) }
                            onSaved()
                        }
                        .onFailure { error ->
                            if (!isLatestConnectionAttempt(normalizedUrl, cleanApiKeyInput)) return@onFailure
                            _uiState.update {
                                it.copy(
                                    isChecking = false,
                                    error = ErrorMapper.userMessage(error, "Connection failed"),
                                )
                            }
                        }
                }
            }
        }
    }

    private fun isLatestConnectionAttempt(serverUrl: String, apiKey: String): Boolean {
        val state = _uiState.value
        return normalizeHermesServerUrl(state.serverUrl) == serverUrl &&
            state.apiKey.safeHeaderLine() == apiKey
    }
}
