package com.hermes.mobile.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.util.agentIdFromChatSessionId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionListUiState(
    val query: String = "",
    val isSyncing: Boolean = false,
    val error: String? = null,
    val sessions: List<SessionEntity> = emptyList(),
    val agents: List<AgentProfile> = AppPreferences.defaultAgents,
    val agentId: String? = null,
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HermesRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val routeAgentId = savedStateHandle.cleanString("agentId")
    private val controls = MutableStateFlow(SessionListUiState(agentId = routeAgentId))

    val uiState: StateFlow<SessionListUiState> = combine(
        repository.sessions(""),
        appPreferences.agents,
        controls,
    ) { sessions, agents, current ->
            val query = current.query.trim()
            val agentSessions = filterSessionsForAgent(sessions, current.agentId)
            val filtered = if (query.isBlank()) {
                agentSessions
            } else {
                agentSessions.filter { session -> sessionMatchesQuery(session, query) }
            }
            current.copy(sessions = filtered, agents = agents)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionListUiState())

    init {
        sync()
    }

    fun onQueryChanged(value: String) {
        controls.update { it.copy(query = value) }
    }

    fun sync() {
        viewModelScope.launch {
            controls.update { it.copy(isSyncing = true, error = null) }
            repository.syncSessions()
                .onFailure { error ->
                    val msg = ErrorMapper.userMessage(error, "Sync failed")
                    // Gracefully ignore 404 — backend may not support Hermes session endpoints
                    if (!ErrorMapper.isEndpointNotFound(error)) {
                        controls.update { it.copy(error = msg) }
                    }
                }
            controls.update { it.copy(isSyncing = false) }
        }
    }

    fun addAgent(name: String, subtitle: String) {
        viewModelScope.launch {
            appPreferences.addAgent(name, subtitle)
        }
    }

    fun updateAgent(id: String, name: String, subtitle: String) {
        viewModelScope.launch {
            appPreferences.updateAgent(id, name, subtitle)
        }
    }

    fun removeAgent(id: String) {
        viewModelScope.launch {
            appPreferences.removeAgent(id)
        }
    }
}

internal fun filterSessionsForAgent(sessions: List<SessionEntity>, agentId: String?): List<SessionEntity> {
    val cleanAgentId = agentId.cleanRouteFilter()
    if (cleanAgentId == null) return sessions
    return sessions.filter { session -> agentIdFromChatSessionId(session.id) == cleanAgentId }
}

private fun SavedStateHandle.cleanString(key: String): String? {
    return get<String>(key).cleanRouteFilter()
}

private fun String?.cleanRouteFilter(): String? {
    return this
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}
