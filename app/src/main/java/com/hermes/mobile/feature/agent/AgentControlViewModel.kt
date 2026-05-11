package com.hermes.mobile.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AgentEndpoint(val title: String, val path: String, val writable: Boolean) {
    Memory("MEMORY.md", "v1/agent/memory", true),
    User("USER.md", "v1/agent/user", true),
    Soul("SOUL.md", "v1/agent/soul", true),
    Profiles("Profiles", "v1/agent/profiles", true),
    Tools("Tools", "v1/tools", true),
    Skills("Skills", "v1/skills", true),
    Schedules("Schedules", "v1/schedules", true),
    Gateways("Gateways", "v1/gateways", true),
}

data class AgentControlUiState(
    val selected: AgentEndpoint = AgentEndpoint.Memory,
    val body: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AgentControlViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentControlUiState())
    val uiState: StateFlow<AgentControlUiState> = _uiState.asStateFlow()

    init {
        load(AgentEndpoint.Memory)
    }

    fun select(endpoint: AgentEndpoint) {
        load(endpoint)
    }

    fun onBodyChanged(value: String) {
        _uiState.update { it.copy(body = value, error = null) }
    }

    fun load(endpoint: AgentEndpoint = _uiState.value.selected) {
        viewModelScope.launch {
            _uiState.update { it.copy(selected = endpoint, isLoading = true, error = null, response = "") }
            repository.getText(endpoint.path)
                .onSuccess { text -> _uiState.update { it.copy(body = text, response = "Loaded ${endpoint.title}") } }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "Load failed", body = "") } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.selected.writable) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, response = "") }
            repository.putText(state.selected.path, state.body)
                .onSuccess { text -> _uiState.update { it.copy(response = text.ifBlank { "Saved ${state.selected.title}" }) } }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "Save failed") } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun submitPost() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, response = "") }
            repository.postText(state.selected.path, state.body)
                .onSuccess { text -> _uiState.update { it.copy(response = text.ifBlank { "Posted to ${state.selected.title}" }) } }
                .onFailure { error -> _uiState.update { it.copy(error = error.message ?: "Post failed") } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
