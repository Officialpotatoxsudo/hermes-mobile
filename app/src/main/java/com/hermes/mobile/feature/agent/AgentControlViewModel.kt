package com.hermes.mobile.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.error.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentControlAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val target: String,
)

data class AgentControlUiState(
    val actions: List<AgentControlAction> = systemControlActions,
    val selectedAction: AgentControlAction? = systemControlActions.firstOrNull(),
    val resultText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AgentControlViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentControlUiState())
    val uiState: StateFlow<AgentControlUiState> = _uiState.asStateFlow()
    private var latestRequestId = 0L

    init {
        systemControlActions.firstOrNull()?.let(::selectAction)
    }

    fun selectAction(action: AgentControlAction) {
        val current = _uiState.value
        if (current.isLoading && current.selectedAction?.id == action.id) return
        val requestId = ++latestRequestId

        _uiState.update {
            it.copy(
                selectedAction = action,
                resultText = "",
                error = null,
                isLoading = true,
            )
        }
        viewModelScope.launch {
            repository.getText(action.target)
                .onSuccess { text ->
                    _uiState.update { state ->
                        if (!state.isCurrentControlRequest(action, requestId)) return@update state
                        state.copy(
                            resultText = text.ifBlank { "${action.title} is available." },
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        if (!state.isCurrentControlRequest(action, requestId)) return@update state
                        state.copy(error = ErrorMapper.userMessage(error, "${action.title} unavailable"))
                    }
                }
            _uiState.update { state ->
                if (state.isCurrentControlRequest(action, requestId)) {
                    state.copy(isLoading = false)
                } else {
                    state
                }
            }
        }
    }

    fun refreshSelected() {
        _uiState.value.selectedAction?.let(::selectAction)
    }

    private fun AgentControlUiState.isCurrentControlRequest(
        action: AgentControlAction,
        requestId: Long,
    ): Boolean {
        return selectedAction?.id == action.id && requestId == latestRequestId
    }
}

val systemControlActions = listOf(
    AgentControlAction(
        id = "server.health",
        title = "Server Health",
        subtitle = "Connection and API heartbeat",
        target = "health",
    ),
    AgentControlAction(
        id = "server.capabilities",
        title = "Capabilities",
        subtitle = "Available API features",
        target = "v1/capabilities",
    ),
    AgentControlAction(
        id = "server.models",
        title = "Models",
        subtitle = "Model list from the connected server",
        target = "v1/models",
    ),
    AgentControlAction(
        id = "server.details",
        title = "Details",
        subtitle = "Detailed server diagnostics",
        target = "health/detailed",
    ),
)
