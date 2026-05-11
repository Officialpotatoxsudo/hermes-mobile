package com.hermes.mobile.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.SessionEntity
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
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val controls = MutableStateFlow(SessionListUiState())

    val uiState: StateFlow<SessionListUiState> = repository.sessions("")
        .combine(controls) { sessions, current ->
            val query = current.query.trim()
            val filtered = if (query.isBlank()) {
                sessions
            } else {
                sessions.filter { session ->
                    session.title.orEmpty().contains(query, ignoreCase = true) ||
                        session.source.contains(query, ignoreCase = true) ||
                        session.model.contains(query, ignoreCase = true)
                }
            }
            current.copy(sessions = filtered)
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
                    val msg = error.message ?: "Sync failed"
                    // Gracefully ignore 404 — backend may not support Hermes session endpoints
                    if (!msg.contains("404")) {
                        controls.update { it.copy(error = msg) }
                    }
                }
            controls.update { it.copy(isSyncing = false) }
        }
    }
}
