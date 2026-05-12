package com.hermes.mobile.feature.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.error.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionHistoryUiState(
    val sessionId: String = "",
    val isSyncing: Boolean = false,
    val error: String? = null,
    val messages: List<MessageEntity> = emptyList(),
)

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HermesRepository,
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])
    private val controls = MutableStateFlow(SessionHistoryUiState(sessionId = sessionId))

    val uiState: StateFlow<SessionHistoryUiState> = repository.messages(sessionId)
        .combine(controls) { messages, current -> current.copy(messages = messages) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionHistoryUiState(sessionId = sessionId))

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            controls.update { it.copy(isSyncing = true, error = null) }
            repository.syncMessages(sessionId)
                .onFailure { error ->
                    val msg = ErrorMapper.userMessage(error, "Sync failed")
                    if (!msg.contains("404")) {
                        controls.update { it.copy(error = msg) }
                    }
                }
            controls.update { it.copy(isSyncing = false) }
        }
    }
}
