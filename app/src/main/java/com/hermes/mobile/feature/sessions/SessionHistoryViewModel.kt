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
import kotlinx.coroutines.flow.flowOf
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
    private val sessionId: String = savedStateHandle.get<String>("sessionId").cleanSessionId()
    private val controls = MutableStateFlow(
        SessionHistoryUiState(
            sessionId = sessionId,
            error = if (sessionId.isBlank()) SESSION_NOT_FOUND_ERROR else null,
        ),
    )
    private val messageFlow = if (sessionId.isBlank()) {
        flowOf(emptyList<MessageEntity>())
    } else {
        repository.messages(sessionId)
    }

    val uiState: StateFlow<SessionHistoryUiState> = messageFlow
        .combine(controls) { messages, current -> current.copy(messages = messages) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), controls.value)

    init {
        sync()
    }

    fun sync() {
        if (sessionId.isBlank()) {
            controls.update { it.copy(isSyncing = false, error = SESSION_NOT_FOUND_ERROR) }
            return
        }

        viewModelScope.launch {
            controls.update { it.copy(isSyncing = true, error = null) }
            repository.syncMessages(sessionId)
                .onFailure { error ->
                    val msg = ErrorMapper.userMessage(error, "Sync failed")
                    if (!ErrorMapper.isEndpointNotFound(error)) {
                        controls.update { it.copy(error = msg) }
                    }
                }
            controls.update { it.copy(isSyncing = false) }
        }
    }

    private companion object {
        const val SESSION_NOT_FOUND_ERROR = "Session not found"
    }
}

private fun String?.cleanSessionId(): String {
    return this
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        .orEmpty()
}
