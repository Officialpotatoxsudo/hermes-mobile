package com.hermes.mobile.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.ChatMessageDto
import com.hermes.mobile.core.model.TokenUsage
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.network.SseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiMessage(
    val id: String,
    val role: String,
    val content: String,
    val time: String,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val usage: TokenUsage? = null,
)

data class ChatUiState(
    val sessionId: String? = null,
    val messages: List<ChatUiMessage> = emptyList(),
    val tools: List<ToolProgress> = emptyList(),
    val draft: String = "",
    val isConnecting: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val lastPrompt: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    fun onDraftChanged(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendCurrentDraft() {
        val prompt = _uiState.value.draft.trim()
        if (prompt.isNotEmpty()) send(prompt)
    }

    fun retryLast() {
        _uiState.value.lastPrompt?.let(::send)
    }

    fun appendVoiceText(text: String) {
        _uiState.update { state ->
            val separator = if (state.draft.isBlank()) "" else " "
            state.copy(draft = state.draft + separator + text.trim())
        }
    }

    fun shareText(): String {
        return _uiState.value.messages.joinToString("\n\n") { message ->
            "${message.role.uppercase()}:\n${message.content}"
        }
    }

    fun stop() {
        streamJob?.cancel()
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = false,
                messages = state.messages.map {
                    if (it.isStreaming) it.copy(isStreaming = false, error = "Stopped") else it
                },
            )
        }
    }

    private fun send(prompt: String) {
        streamJob?.cancel()
        val request = ChatCompletionRequest(
            messages = _uiState.value.messages
                .filter { it.error == null && !it.isStreaming }
                .map { ChatMessageDto(role = it.role, content = it.content) } + ChatMessageDto("user", prompt),
        )
        _uiState.update {
            it.copy(
                draft = "",
                isConnecting = true,
                isStreaming = false,
                error = null,
                tools = emptyList(),
                lastPrompt = prompt,
            )
        }
        streamJob = viewModelScope.launch {
            val assistantId = "assistant-${System.currentTimeMillis()}"
            repository.streamChat(request)
                .catch { error ->
                    _uiState.update { state ->
                        state.copy(
                            isConnecting = false,
                            isStreaming = false,
                            error = error.message ?: "Stream failed",
                            messages = state.messages.map {
                                if (it.id == assistantId) it.copy(isStreaming = false, error = "Connection lost") else it
                            },
                        )
                    }
                }
                .collect { event ->
                    when (event) {
                        is SseEvent.Opened -> onOpened(event.sessionId, prompt, assistantId)
                        is SseEvent.Delta -> appendDelta(assistantId, event.text)
                        is SseEvent.Tool -> _uiState.update { it.copy(tools = it.tools + event.progress) }
                        is SseEvent.Usage -> setUsage(assistantId, event.usage)
                        SseEvent.Done -> finishStream(assistantId)
                    }
                }
        }
    }

    private fun onOpened(sessionId: String?, prompt: String, assistantId: String) {
        val now = currentTimeLabel()
        _uiState.update { state ->
            state.copy(
                sessionId = sessionId ?: state.sessionId,
                isConnecting = false,
                isStreaming = true,
                messages = state.messages +
                    ChatUiMessage("user-${System.currentTimeMillis()}", "user", prompt, now) +
                    ChatUiMessage(assistantId, "assistant", "", now, isStreaming = true),
            )
        }
    }

    private fun appendDelta(messageId: String, delta: String) {
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = true,
                messages = state.messages.map {
                    if (it.id == messageId) it.copy(content = it.content + delta) else it
                },
            )
        }
    }

    private fun setUsage(messageId: String, usage: TokenUsage) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { if (it.id == messageId) it.copy(usage = usage) else it })
        }
    }

    private fun finishStream(messageId: String) {
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = false,
                messages = state.messages.map {
                    if (it.id == messageId) it.copy(isStreaming = false) else it
                },
            )
        }
    }

    private fun currentTimeLabel(): String {
        val now = java.time.LocalTime.now()
        return "%02d:%02d".format(now.hour, now.minute)
    }
}
