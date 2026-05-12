package com.hermes.mobile.feature.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.error.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject

data class ConnectionSetupUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
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

    fun onServerUrlChanged(value: String) {
        _uiState.update { it.copy(serverUrl = value, error = null) }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.update { it.copy(apiKey = value, error = null) }
    }

    fun checkAndSave(onSaved: () -> Unit) {
        val state = _uiState.value
        val normalizedUrl = state.serverUrl.trim()
        val uri = runCatching { URI(normalizedUrl) }.getOrNull()
        when {
            uri?.scheme != "https" || uri.host.isNullOrBlank() -> {
                _uiState.update { it.copy(error = "Enter a valid HTTPS server URL") }
            }
            state.apiKey.isBlank() -> {
                _uiState.update { it.copy(error = "API key required") }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isChecking = true, error = null, isHealthy = false) }
                    repository.checkHealth(normalizedUrl, state.apiKey.trim())
                        .onSuccess {
                            tokenStore.saveCredentials(normalizedUrl, state.apiKey.trim())
                            _uiState.update { it.copy(isChecking = false, isHealthy = true) }
                            onSaved()
                        }
                        .onFailure { error ->
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
}
