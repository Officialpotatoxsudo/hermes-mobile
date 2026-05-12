package com.hermes.mobile.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.ChatMessageDto
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.TokenUsage
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.network.SseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class ChatUiMessage(
    val id: String,
    val role: String,
    val content: String,
    val time: String,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val usage: TokenUsage? = null,
    val replyTo: String? = null,
)

data class ChatAttachment(
    val id: String,
    val label: String,
    val uri: String,
    val kind: String = "file",
)

data class PendingPrompt(
    val id: String,
    val text: String,
    val displayText: String = text,
    val replyTo: String? = null,
)

data class ChatModelOption(
    val provider: String,
    val model: String,
    val label: String,
)

data class ChatUiState(
    val sessionId: String? = null,
    val messages: List<ChatUiMessage> = emptyList(),
    val tools: List<ToolProgress> = emptyList(),
    val draft: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val queuedPrompts: List<PendingPrompt> = emptyList(),
    val pinnedMessageIds: Set<String> = emptySet(),
    val replyTarget: ChatUiMessage? = null,
    val modelOptions: List<ChatModelOption> = defaultChatModelOptions,
    val selectedModel: ChatModelOption = defaultChatModelOptions.first(),
    val isConnecting: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val lastPrompt: String? = null,
)

val defaultChatModelOptions = listOf(
    ChatModelOption(provider = "auto", model = "hermes-agent", label = "Hermes"),
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private val resumeSessionId: String? = savedStateHandle["sessionId"]

    init {
        syncModelOptions()
        resumeSessionId?.takeIf { it.isNotBlank() }?.let(::loadSession)
    }

    fun onDraftChanged(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendCurrentDraft() {
        val prompt = _uiState.value.draft.trim()
        val attachments = _uiState.value.attachments
        if (prompt.isNotEmpty() || attachments.isNotEmpty()) {
            submit(buildPrompt(prompt, attachments))
        }
    }

    fun retryLast() {
        _uiState.value.lastPrompt?.let(::submit)
    }

    fun addAttachment(uri: String, label: String) {
        _uiState.update { state ->
            state.copy(
                attachments = state.attachments + ChatAttachment(
                    id = "attachment-${System.currentTimeMillis()}",
                    label = label.ifBlank { "Attachment" },
                    uri = uri,
                ),
            )
        }
    }

    fun addVoiceRecording(uri: String, label: String) {
        _uiState.update { state ->
            state.copy(
                attachments = state.attachments + ChatAttachment(
                    id = "voice-${System.currentTimeMillis()}",
                    label = label.ifBlank { "Voice note" },
                    uri = uri,
                    kind = "voice",
                ),
            )
        }
    }

    fun removeAttachment(id: String) {
        _uiState.update { state -> state.copy(attachments = state.attachments.filterNot { it.id == id }) }
    }

    fun insertCommand(command: String) {
        _uiState.update { state ->
            if (command.startsWith("/model ")) {
                val requested = command.removePrefix("/model ").trim()
                val match = state.modelOptions.firstOrNull {
                    it.model.equals(requested, ignoreCase = true) ||
                        it.label.equals(requested, ignoreCase = true) ||
                        "${it.provider}:${it.model}".equals(requested, ignoreCase = true) ||
                        "${it.model} --provider ${it.provider}".equals(requested, ignoreCase = true)
                }
                state.copy(
                    selectedModel = match ?: state.selectedModel,
                    draft = "",
                    error = if (match == null) "Unknown model: $requested" else null,
                )
            } else {
                val suffix = if (state.draft.endsWith(" ") || state.draft.isBlank()) "" else " "
                state.copy(draft = state.draft.trimEnd() + suffix + command + " ")
            }
        }
    }

    fun selectModel(option: ChatModelOption) {
        _uiState.update { it.copy(selectedModel = option, error = null) }
    }

    fun refreshModelOptions() {
        syncModelOptions()
    }

    fun replyTo(messageId: String) {
        _uiState.update { state ->
            state.copy(replyTarget = state.messages.firstOrNull { it.id == messageId })
        }
    }

    fun clearReply() {
        _uiState.update { it.copy(replyTarget = null) }
    }

    fun togglePin(messageId: String) {
        _uiState.update { state ->
            val pinned = if (messageId in state.pinnedMessageIds) {
                state.pinnedMessageIds - messageId
            } else {
                state.pinnedMessageIds + messageId
            }
            state.copy(pinnedMessageIds = pinned)
        }
    }

    fun deleteMessage(messageId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.id == messageId },
                pinnedMessageIds = state.pinnedMessageIds - messageId,
                replyTarget = state.replyTarget?.takeIf { it.id != messageId },
            )
        }
    }

    fun editMessage(messageId: String) {
        _uiState.update { state ->
            val target = state.messages.firstOrNull { it.id == messageId && it.role == "user" } ?: return@update state
            state.copy(
                draft = target.content,
                messages = state.messages.filterNot { it.id == messageId },
                pinnedMessageIds = state.pinnedMessageIds - messageId,
                replyTarget = null,
            )
        }
    }

    fun cancelQueuedPrompt(id: String) {
        _uiState.update { state ->
            state.copy(queuedPrompts = state.queuedPrompts.filterNot { it.id == id })
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
        sendNextQueuedIfIdle()
    }

    private fun submit(prompt: String) {
        val state = _uiState.value
        val replyText = state.replyTarget?.let(::replyPreviewText)
        val apiPrompt = withReplyContext(prompt, state.replyTarget)
        if (state.isConnecting || state.isStreaming) {
            _uiState.update {
                it.copy(
                    draft = "",
                    attachments = emptyList(),
                    queuedPrompts = it.queuedPrompts + PendingPrompt(
                        id = "queued-${System.currentTimeMillis()}",
                        text = apiPrompt,
                        displayText = prompt,
                        replyTo = replyText,
                    ),
                    replyTarget = null,
                )
            }
            return
        }
        send(apiPrompt, displayPrompt = prompt, replyTo = replyText)
    }

    private fun send(prompt: String, displayPrompt: String = prompt, replyTo: String? = null) {
        streamJob?.cancel()
        val now = currentTimeLabel()
        val userId = "user-${System.currentTimeMillis()}"
        val assistantId = "assistant-${System.currentTimeMillis()}"
        val request = ChatCompletionRequest(
            model = _uiState.value.selectedModel.model.ifBlank { "hermes-agent" },
            messages = _uiState.value.messages
                .filter { it.error == null && !it.isStreaming }
                .map { ChatMessageDto(role = it.role, content = it.content) } + ChatMessageDto("user", prompt),
        )
        _uiState.update {
            it.copy(
                draft = "",
                attachments = emptyList(),
                isConnecting = true,
                isStreaming = false,
                error = null,
                tools = emptyList(),
                replyTarget = null,
                lastPrompt = prompt,
                messages = it.messages +
                    ChatUiMessage(
                        id = userId,
                        role = "user",
                        content = displayPrompt,
                        time = now,
                        replyTo = replyTo,
                    ) +
                    ChatUiMessage(assistantId, "assistant", "", now, isStreaming = true),
            )
        }
        streamJob = viewModelScope.launch {
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
                        is SseEvent.Opened -> onOpened(event.sessionId)
                        is SseEvent.Delta -> appendDelta(assistantId, event.text)
                        is SseEvent.Tool -> _uiState.update { it.copy(tools = it.tools + event.progress) }
                        is SseEvent.Usage -> setUsage(assistantId, event.usage)
                        SseEvent.Done -> finishStream(assistantId)
                    }
                }
        }
    }

    private fun onOpened(sessionId: String?) {
        _uiState.update { state ->
            state.copy(
                sessionId = sessionId ?: state.sessionId,
                isConnecting = false,
                isStreaming = true,
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
        sendNextQueuedIfIdle()
    }

    private fun sendNextQueuedIfIdle() {
        val next = _uiState.value.queuedPrompts.firstOrNull() ?: return
        if (_uiState.value.isConnecting || _uiState.value.isStreaming) return
        _uiState.update { state -> state.copy(queuedPrompts = state.queuedPrompts.drop(1)) }
        send(next.text, displayPrompt = next.displayText, replyTo = next.replyTo)
    }

    private fun syncModelOptions() {
        viewModelScope.launch {
            repository.fetchModelOptions().onSuccess(::applyModelOptions)
        }
    }

    private fun applyModelOptions(response: DashboardModelOptionsResponse) {
        val options = response.providers
            .flatMap { provider ->
                provider.models.take(40).map { model ->
                    ChatModelOption(
                        provider = provider.slug,
                        model = model,
                        label = model.substringAfterLast('/').ifBlank { model },
                    )
                }
            }
            .ifEmpty { defaultChatModelOptions }
        val selected = options.firstOrNull {
            it.provider == response.provider && it.model == response.model
        } ?: options.firstOrNull { it.model == response.model } ?: options.first()
        _uiState.update { it.copy(modelOptions = options, selectedModel = selected) }
    }

    private fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(sessionId = sessionId, isConnecting = true, error = null) }
            repository.syncMessages(sessionId)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Session sync failed") }
                }
            val messages = repository.messages(sessionId).first().map(MessageEntity::toChatUiMessage)
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    messages = messages,
                    isConnecting = false,
                )
            }
        }
    }

    private fun currentTimeLabel(): String {
        val now = java.time.LocalTime.now()
        return "%02d:%02d".format(now.hour, now.minute)
    }

    private fun buildPrompt(prompt: String, attachments: List<ChatAttachment>): String {
        if (attachments.isEmpty()) return prompt
        val attachmentText = attachments.joinToString("\n", transform = ::formatAttachmentForPrompt)
        return listOf(
            prompt.takeIf { it.isNotBlank() },
            "Attachments:\n$attachmentText",
        ).filterNotNull().joinToString("\n\n")
    }

    private fun withReplyContext(prompt: String, replyTarget: ChatUiMessage?): String {
        if (replyTarget == null) return prompt
        return "Replying to ${replyTarget.role}: ${replyPreviewText(replyTarget)}\n\n$prompt"
    }

    private fun replyPreviewText(message: ChatUiMessage): String {
        return message.content.lineSequence().firstOrNull().orEmpty().take(180)
    }

    private fun formatAttachmentForPrompt(attachment: ChatAttachment): String {
        if (attachment.kind != "voice" || !attachment.uri.startsWith("file://")) {
            return "- ${attachment.kind}: ${attachment.label}: ${attachment.uri}"
        }
        return runCatching {
            val file = File(java.net.URI.create(attachment.uri))
            val payload = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
            "- voice: ${attachment.label}: data:audio/mp4;base64,$payload"
        }.getOrElse {
            "- voice: ${attachment.label}: ${attachment.uri}"
        }
    }
}

private fun MessageEntity.toChatUiMessage(): ChatUiMessage {
    val time = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    return ChatUiMessage(
        id = "history-$id",
        role = role,
        content = content,
        time = "%02d:%02d".format(time.hour, time.minute),
    )
}
