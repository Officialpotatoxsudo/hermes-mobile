package com.hermes.mobile.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.ChatMessageDto
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.HermesSlashCommandAction
import com.hermes.mobile.core.model.TokenUsage
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.mapHermesSlashCommand
import com.hermes.mobile.core.network.SseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.util.UUID
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
    val responseTime: String? = null,
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
    private val savedStateHandle: SavedStateHandle,
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(
            sessionId = savedStateHandle["activeSessionId"],
            draft = savedStateHandle["draft"] ?: "",
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val messageStartTimes = mutableMapOf<String, Long>()
    private val sentTimestamps = ArrayDeque<Long>()
    private var streamJob: Job? = null
    private val resumeSessionId: String? = savedStateHandle["sessionId"] ?: savedStateHandle["activeSessionId"]

    init {
        syncModelOptions()
        resumeSessionId?.takeIf { it.isNotBlank() }?.let(::loadSession)
    }

    fun onDraftChanged(value: String) {
        _uiState.update { it.copy(draft = value) }
        savedStateHandle["draft"] = value
    }

    fun sendCurrentDraft() {
        val prompt = _uiState.value.draft.trim()
        val attachments = _uiState.value.attachments
        if (prompt.startsWith("/") && attachments.isEmpty()) {
            handleSlashCommand(prompt)
            return
        }
        if (prompt.isNotEmpty() || attachments.isNotEmpty()) {
            submit(buildPrompt(prompt, attachments))
        }
    }

    private fun handleSlashCommand(prompt: String) {
        when (val action = mapHermesSlashCommand(prompt)) {
            HermesSlashCommandAction.None -> submit(prompt)
            HermesSlashCommandAction.NewSession -> startNewSession()
            HermesSlashCommandAction.Retry -> {
                _uiState.update { it.copy(draft = "") }
                retryLast()
            }
            HermesSlashCommandAction.Undo -> undoLastExchange()
            HermesSlashCommandAction.Stop -> {
                _uiState.update { it.copy(draft = "") }
                stop()
            }
            HermesSlashCommandAction.ClearDraft -> _uiState.update { it.copy(draft = "", attachments = emptyList(), error = null) }
            is HermesSlashCommandAction.SelectModel -> insertCommand("/model ${action.requested}")
            is HermesSlashCommandAction.AgentPrompt -> submit(action.prompt)
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
                val draft = state.draft.trimEnd() + suffix + command + " "
                savedStateHandle["draft"] = draft
                state.copy(draft = draft)
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
        val persistedId = messageId.persistedMessageId()
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.id == messageId },
                pinnedMessageIds = state.pinnedMessageIds - messageId,
                replyTarget = state.replyTarget?.takeIf { it.id != messageId },
            )
        }
        persistedId?.let { deletePersistedMessages(listOf(it)) }
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
    }

    fun editMessage(messageId: String) {
        var persistedId: Long? = null
        _uiState.update { state ->
            val target = state.messages.firstOrNull { it.id == messageId && it.role == "user" } ?: return@update state
            persistedId = messageId.persistedMessageId()
            state.copy(
                draft = target.content,
                messages = state.messages.filterNot { it.id == messageId },
                pinnedMessageIds = state.pinnedMessageIds - messageId,
                replyTarget = null,
            )
        }
        persistedId?.let { deletePersistedMessages(listOf(it)) }
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
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

    private fun startNewSession() {
        streamJob?.cancel()
        savedStateHandle["activeSessionId"] = null
        savedStateHandle["draft"] = ""
        _uiState.update {
            it.copy(
                sessionId = null,
                messages = emptyList(),
                tools = emptyList(),
                draft = "",
                attachments = emptyList(),
                queuedPrompts = emptyList(),
                pinnedMessageIds = emptySet(),
                replyTarget = null,
                isConnecting = false,
                isStreaming = false,
                error = null,
                lastPrompt = null,
            )
        }
    }

    private fun undoLastExchange() {
        var removedPersistedIds = emptyList<Long>()
        _uiState.update { state ->
            val lastUserIndex = state.messages.indexOfLast { it.role == "user" }
            if (lastUserIndex < 0) {
                state.copy(draft = "", error = "No exchange to undo")
            } else {
                val removedMessages = state.messages.drop(lastUserIndex)
                val removedIds = removedMessages.map { it.id }.toSet()
                removedPersistedIds = removedMessages.mapNotNull { it.id.persistedMessageId() }
                state.copy(
                    draft = "",
                    messages = state.messages.take(lastUserIndex),
                    pinnedMessageIds = state.pinnedMessageIds - removedIds,
                    replyTarget = state.replyTarget?.takeIf { it.id !in removedIds },
                    error = null,
                )
            }
        }
        deletePersistedMessages(removedPersistedIds)
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
    }

    private fun submit(prompt: String) {
        val state = _uiState.value
        val rateLimitWait = rateLimitWaitSeconds()
        if (rateLimitWait > 0) {
            _uiState.update { it.copy(error = "Please wait $rateLimitWait seconds") }
            return
        }
        val replyText = state.replyTarget?.let(::replyPreviewText)
        val apiPrompt = withReplyContext(prompt, state.replyTarget)
        if (state.isConnecting || state.isStreaming) {
            if (state.queuedPrompts.size >= MAX_QUEUE_SIZE) {
                _uiState.update { it.copy(error = "Queue full. Please wait.") }
                return
            }
            recordSendAttempt()
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
        recordSendAttempt()
        send(apiPrompt, displayPrompt = prompt, replyTo = replyText)
    }

    private fun send(prompt: String, displayPrompt: String = prompt, replyTo: String? = null) {
        streamJob?.cancel()
        val nowMillis = System.currentTimeMillis()
        val now = currentTimeLabel(nowMillis)
        val sessionId = _uiState.value.sessionId ?: UUID.randomUUID().toString()
        val userMessageId = nowMillis
        val assistantMessageId = nowMillis + 1
        val userId = "user-$userMessageId"
        val assistantId = "assistant-$assistantMessageId"
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
                sessionId = sessionId,
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
        savedStateHandle["activeSessionId"] = sessionId
        savedStateHandle["draft"] = ""
        persistSessionSnapshot(sessionId)
        persistMessage(sessionId, userMessageId, "user", displayPrompt)
        messageStartTimes[assistantId] = System.currentTimeMillis()
        streamJob = viewModelScope.launch {
            var completed = false
            var lastError: Throwable? = null
            for (attempt in 0..MAX_RETRIES) {
                if (attempt > 0) {
                    _uiState.update { state ->
                        state.copy(
                            isConnecting = true,
                            isStreaming = false,
                            error = "Reconnecting... (attempt $attempt/$MAX_RETRIES)",
                        )
                    }
                    delay(RETRY_BACKOFF_MS[attempt - 1])
                }
                repository.streamChat(request, sessionId)
                    .catch { error ->
                        lastError = error
                    }
                    .collect { event ->
                        when (event) {
                            is SseEvent.Opened -> onOpened(event.sessionId, sessionId)
                            is SseEvent.Delta -> appendDelta(assistantId, event.text)
                            is SseEvent.Tool -> _uiState.update { it.copy(tools = it.tools + event.progress) }
                            is SseEvent.Usage -> setUsage(assistantId, event.usage)
                            SseEvent.Done -> {
                                completed = true
                                finishStream(assistantId)
                            }
                        }
                    }
                if (completed) return@launch
                if (attempt == MAX_RETRIES) break
            }
            val readable = ErrorMapper.userMessage(lastError, "Connection lost")
            _uiState.update { state ->
                state.copy(
                    isConnecting = false,
                    isStreaming = false,
                    error = readable,
                    messages = state.messages.map {
                        if (it.id == assistantId) it.copy(isStreaming = false, error = readable) else it
                    },
                )
            }
        }
    }

    private fun onOpened(remoteSessionId: String?, localSessionId: String) {
        val activeSessionId = remoteSessionId?.takeIf { it.isNotBlank() } ?: localSessionId
        _uiState.update { state ->
            state.copy(
                sessionId = activeSessionId,
                isConnecting = false,
                isStreaming = true,
            )
        }
        if (activeSessionId != localSessionId) {
            savedStateHandle["activeSessionId"] = activeSessionId
            migrateLocalSession(localSessionId, activeSessionId)
        } else {
            persistSessionSnapshot(activeSessionId)
        }
    }

    private fun appendDelta(messageId: String, delta: String) {
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = true,
                messages = state.messages.map {
                    if (it.id == messageId) {
                        val responseTime = it.responseTime ?: run {
                            val start = messageStartTimes[messageId]
                            if (start != null) {
                                val diff = (System.currentTimeMillis() - start) / 1000.0
                                "%.1fs".format(diff)
                            } else null
                        }
                        it.copy(content = it.content + delta, responseTime = responseTime)
                    } else it
                },
            )
        }
        persistStreamingAssistant(messageId)
    }

    private fun setUsage(messageId: String, usage: TokenUsage) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { if (it.id == messageId) it.copy(usage = usage) else it })
        }
        persistStreamingAssistant(messageId)
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
        persistStreamingAssistant(messageId)
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
        sendNextQueuedIfIdle()
    }

    private fun sendNextQueuedIfIdle() {
        val next = _uiState.value.queuedPrompts.firstOrNull() ?: return
        if (_uiState.value.isConnecting || _uiState.value.isStreaming) return
        _uiState.update { state -> state.copy(queuedPrompts = state.queuedPrompts.drop(1)) }
        send(next.text, displayPrompt = next.displayText, replyTo = next.replyTo)
    }

    private fun rateLimitWaitSeconds(now: Long = System.currentTimeMillis()): Long {
        while (sentTimestamps.isNotEmpty() && now - sentTimestamps.first() > RATE_LIMIT_WINDOW_MS) {
            sentTimestamps.removeFirst()
        }
        if (sentTimestamps.size < RATE_LIMIT_MAX_MESSAGES) return 0
        val waitMs = RATE_LIMIT_WINDOW_MS - (now - sentTimestamps.first())
        return ((waitMs + 999L) / 1000L).coerceAtLeast(1L)
    }

    private fun recordSendAttempt(now: Long = System.currentTimeMillis()) {
        while (sentTimestamps.isNotEmpty() && now - sentTimestamps.first() > RATE_LIMIT_WINDOW_MS) {
            sentTimestamps.removeFirst()
        }
        sentTimestamps.addLast(now)
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
                    _uiState.update { it.copy(error = ErrorMapper.userMessage(error, "Session sync failed")) }
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

    private fun persistSessionSnapshot(sessionId: String) {
        val state = _uiState.value
        val firstUser = state.messages.firstOrNull { it.role == "user" }?.content
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.saveLocalSession(
                SessionEntity(
                    id = sessionId,
                    title = firstUser?.take(72) ?: "Hermes chat",
                    source = "mobile",
                    startedAt = now,
                    endedAt = null,
                    messageCount = state.messages.count { it.content.isNotBlank() },
                    model = state.selectedModel.model,
                    lastSyncedAt = now,
                ),
            )
        }
    }

    private fun persistMessage(sessionId: String, id: Long, role: String, content: String) {
        viewModelScope.launch {
            repository.saveLocalMessage(
                MessageEntity(
                    id = id,
                    sessionId = sessionId,
                    role = role,
                    content = content,
                    timestamp = id,
                ),
            )
        }
    }

    private fun persistStreamingAssistant(messageId: String) {
        val sessionId = _uiState.value.sessionId ?: return
        val id = messageId.persistedMessageId() ?: return
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (message.content.isBlank()) return
        persistMessage(sessionId, id, "assistant", message.content)
    }

    private fun deletePersistedMessages(messageIds: List<Long>) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            repository.deleteLocalMessages(messageIds)
        }
    }

    private fun migrateLocalSession(localSessionId: String, remoteSessionId: String) {
        val state = _uiState.value
        val messages = state.messages.mapNotNull { message ->
            val id = message.id.substringAfter("-", "").toLongOrNull() ?: return@mapNotNull null
            MessageEntity(
                id = id,
                sessionId = remoteSessionId,
                role = message.role,
                content = message.content,
                timestamp = id,
            )
        }
        viewModelScope.launch {
            repository.saveLocalSession(
                SessionEntity(
                    id = remoteSessionId,
                    title = messages.firstOrNull { it.role == "user" }?.content?.take(72) ?: "Hermes chat",
                    source = "mobile",
                    startedAt = messages.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
                    endedAt = null,
                    messageCount = messages.count { it.content.isNotBlank() },
                    model = state.selectedModel.model,
                    lastSyncedAt = System.currentTimeMillis(),
                ),
            )
            repository.saveLocalMessages(messages)
            repository.deleteLocalSession(localSessionId)
        }
    }

    private fun currentTimeLabel(timestamp: Long = System.currentTimeMillis()): String {
        val now = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
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

private fun String.persistedMessageId(): Long? = substringAfterLast("-", missingDelimiterValue = "").toLongOrNull()

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

private const val MAX_QUEUE_SIZE = 10
private const val MAX_RETRIES = 3
private val RETRY_BACKOFF_MS = longArrayOf(2_000L, 4_000L, 8_000L)
private const val RATE_LIMIT_WINDOW_MS = 10_000L
private const val RATE_LIMIT_MAX_MESSAGES = 5
