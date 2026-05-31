package com.hermes.mobile.feature.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.HermesSlashCommandAction
import com.hermes.mobile.core.model.TokenUsage
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.chatRequestMessage
import com.hermes.mobile.core.network.ConnectionState
import com.hermes.mobile.core.model.mapHermesSlashCommand
import com.hermes.mobile.core.network.SseEvent
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.util.agentIdFromChatSessionId
import com.hermes.mobile.core.util.deleteAppOwnedMessageMedia
import com.hermes.mobile.core.util.formatPhotoSummary
import com.hermes.mobile.core.util.isAllowedMessageImageUri
import com.hermes.mobile.core.util.legacyImageUrisFromText
import com.hermes.mobile.core.util.messageImageUrisFromJson
import com.hermes.mobile.core.util.newAgentChatSessionId
import com.hermes.mobile.core.util.readableMessageText
import com.hermes.mobile.core.util.ReceivedAttachment
import com.hermes.mobile.core.util.receivedAttachmentsFromMessage
import com.hermes.mobile.core.util.resolveOpenedChatSessionId
import com.hermes.mobile.core.util.visibleMessageText
import com.hermes.mobile.core.util.visibleReceivedAttachmentText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ChatUiMessage(
    val id: String,
    val role: String,
    val content: String,
    val time: String,
    val reasoning: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val usage: TokenUsage? = null,
    val replyTo: String? = null,
    val responseTime: String? = null,
    val imageUris: List<String> = emptyList(),
    val receivedAttachments: List<ReceivedAttachment> = emptyList(),
)

data class ChatAttachment(
    val id: String,
    val label: String,
    val uri: String,
    val kind: String = "file",
)

data class ChatPromptPayload(
    val apiPrompt: String,
    val displayPrompt: String,
)

data class PendingPrompt(
    val id: String,
    val text: String,
    val displayText: String = text,
    val replyTo: String? = null,
    val imageUris: List<String> = emptyList(),
)

data class ChatModelOption(
    val provider: String,
    val model: String,
    val label: String,
)

data class ChatUiState(
    val sessionId: String? = null,
    val agentName: String = "Hermes",
    val agentAvatarUri: String? = null,
    val agents: List<AgentProfile> = AppPreferences.defaultAgents,
    val messages: List<ChatUiMessage> = emptyList(),
    val tools: List<ToolProgress> = emptyList(),
    val draft: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val queuedPrompts: List<PendingPrompt> = emptyList(),
    val pinnedMessageIds: Set<String> = emptySet(),
    val replyTarget: ChatUiMessage? = null,
    val modelOptions: List<ChatModelOption> = defaultChatModelOptions,
    val selectedModel: ChatModelOption = defaultChatModelOptions.first(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isConnecting: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val lastPrompt: PendingPrompt? = null,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val matchedMessageIds: Set<String> = emptySet(),
    val searchScrollTargetIndex: Int = -1,
)

val defaultChatModelOptions = listOf(
    ChatModelOption(provider = "auto", model = "hermes-agent", label = "Hermes"),
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: HermesRepository,
    private val appPreferences: AppPreferences,
    @param:ApplicationContext private val appContext: Context,
    private val streamCoordinator: ChatStreamCoordinator = ChatStreamCoordinator(repository),
) : ViewModel() {
    private val explicitAgentName = savedStateHandle.cleanString("agentName")
    private val routeSessionId = savedStateHandle.cleanString("sessionId")
    private val activeSessionId = savedStateHandle.cleanString("activeSessionId")
    private val resolvedFromSessionId = savedStateHandle.cleanString("resolvedFromSessionId")
    private val initialSessionId = when {
        routeSessionId != null && activeSessionId != null && resolvedFromSessionId == routeSessionId ->
            resolveOpenedChatSessionId(routeSessionId, activeSessionId)
        routeSessionId != null -> routeSessionId
        else -> activeSessionId
    }
    private val _uiState = MutableStateFlow(
        ChatUiState(
            sessionId = initialSessionId,
            agentName = agentNameForSession(initialSessionId, AppPreferences.defaultAgents, explicitAgentName ?: "Hermes"),
            draft = savedStateHandle["draft"] ?: "",
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val messageStartTimes = mutableMapOf<String, Long>()
    private val sentTimestamps = ArrayDeque<Long>()
    private val messageIds = MonotonicIdGenerator()
    private var streamJob: Job? = null
    private var visibleSessionId: String? = null
    private var isCleared = false
    private val resumeSessionId: String? = initialSessionId

    init {
        observeAgentProfileNames()
        observeActiveStreams()
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
            val payload = buildChatPromptPayload(prompt, attachments)
            val accepted = submit(
                prompt = payload.apiPrompt,
                displayPrompt = payload.displayPrompt,
                imageUris = attachments.previewImageUris(),
            )
            if (accepted) {
                cleanupAppOwnedMedia(attachments.sentAttachmentCleanupUris())
            }
        }
    }

    private fun handleSlashCommand(prompt: String) {
        when (val action = mapHermesSlashCommand(prompt)) {
            HermesSlashCommandAction.None -> submit(prompt)
            HermesSlashCommandAction.NewSession -> startNewSession()
            HermesSlashCommandAction.Retry -> {
                savedStateHandle["draft"] = ""
                _uiState.update { it.copy(draft = "") }
                retryLast()
            }
            HermesSlashCommandAction.Undo -> undoLastExchange()
            HermesSlashCommandAction.Stop -> {
                savedStateHandle["draft"] = ""
                _uiState.update { it.copy(draft = "") }
                stop()
            }
            HermesSlashCommandAction.ClearDraft -> startNewSession()
            is HermesSlashCommandAction.SelectModel -> insertCommand("/model ${action.requested}")
            is HermesSlashCommandAction.AgentPrompt -> submit(action.prompt)
        }
    }

    fun startNewChat() {
        startNewSession()
    }

    fun retryLast() {
        _uiState.value.lastPrompt?.let(::submitPreparedPrompt)
    }

    fun addAttachment(uri: String, label: String, kind: String = "file") {
        val cleanUri = uri.firstNonBlankLine() ?: return
        val cleanKind = kind.cleanAttachmentKind("file")
        if (cleanKind == "image" && !isAllowedMessageImageUri(cleanUri)) return
        _uiState.update { state ->
            state.withAddedAttachment(
                ChatAttachment(
                    id = "attachment-${System.currentTimeMillis()}",
                    label = label.cleanAttachmentLabel("Attachment"),
                    uri = cleanUri,
                    kind = cleanKind,
                ),
            )
        }
    }

    fun addVoiceRecording(uri: String, label: String) {
        val cleanUri = uri.firstNonBlankLine() ?: return
        _uiState.update { state ->
            state.withAddedAttachment(
                ChatAttachment(
                    id = "voice-${System.currentTimeMillis()}",
                    label = label.cleanAttachmentLabel("Voice note"),
                    uri = cleanUri,
                    kind = "voice",
                ),
            )
        }
    }

    fun removeAttachment(id: String) {
        val removed = _uiState.value.attachments.firstOrNull { it.id == id }
        _uiState.update { state -> state.copy(attachments = state.attachments.filterNot { it.id == id }) }
        removed?.let { cleanupAppOwnedMedia(listOf(it.uri)) }
    }

    fun insertCommand(command: String) {
        _uiState.update { state ->
            val modelRequest = command.modelCommandRequest()
            if (modelRequest != null) {
                val requested = modelRequest
                val match = state.modelOptions.firstOrNull {
                    it.model.equals(requested, ignoreCase = true) ||
                        it.label.equals(requested, ignoreCase = true) ||
                        "${it.provider}:${it.model}".equals(requested, ignoreCase = true) ||
                        "${it.model} --provider ${it.provider}".equals(requested, ignoreCase = true)
                }
                savedStateHandle["draft"] = ""
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

    fun openSearch() {
        _uiState.update { it.copy(isSearchOpen = true, searchQuery = "", matchedMessageIds = emptySet(), searchScrollTargetIndex = -1) }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchOpen = false, searchQuery = "", matchedMessageIds = emptySet(), searchScrollTargetIndex = -1) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val matches = if (query.isBlank()) {
                emptySet()
            } else {
                val lowerQuery = query.lowercase()
                state.messages
                    .filter { it.content.lowercase().contains(lowerQuery) }
                    .map { it.id }
                    .toSet()
            }
            val firstMatchIndex = state.messages.indexOfFirst { it.id in matches }
            state.copy(searchQuery = query, matchedMessageIds = matches, searchScrollTargetIndex = firstMatchIndex)
        }
    }

    fun scrollToNextMatch() {
        val state = _uiState.value
        if (state.matchedMessageIds.isEmpty()) return
        val matchedIndices = state.messages
            .mapIndexedNotNull { index, msg -> if (msg.id in state.matchedMessageIds) index else null }
        val currentIndex = matchedIndices.indexOfFirst { it >= (state.searchScrollTargetIndex.takeIf { it >= 0 } ?: -1) }
        val nextIndex = if (currentIndex < 0 || currentIndex >= matchedIndices.size - 1) {
            matchedIndices.first()
        } else {
            matchedIndices[currentIndex + 1]
        }
        _uiState.update { it.copy(searchScrollTargetIndex = nextIndex) }
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
        var removedPersistedIds = emptyList<Long>()
        var removedMediaUris = emptyList<String>()
        _uiState.update { state ->
            val targetIndex = state.messages.indexOfFirst { it.id == messageId }
            if (targetIndex < 0) return@update state

            val exchangeEnd = if (state.messages[targetIndex].role == "user") {
                state.messages.exchangeEndAfterUser(targetIndex)
            } else {
                targetIndex + 1
            }
            val removedMessages = state.messages.subList(targetIndex, exchangeEnd)
            val removedIds = removedMessages.map { it.id }.toSet()
            removedPersistedIds = removedMessages.mapNotNull { it.id.persistedMessageId() }
            removedMediaUris = removedMessages.flatMap { it.imageUris }
            state.copy(
                messages = state.messages.take(targetIndex) + state.messages.drop(exchangeEnd),
                pinnedMessageIds = state.pinnedMessageIds - removedIds,
                replyTarget = state.replyTarget?.takeIf { it.id !in removedIds },
                lastPrompt = state.lastPrompt.takeUnlessRemoved(removedIds),
            )
        }
        cleanupAppOwnedMedia(removedMediaUris)
        deletePersistedMessages(removedPersistedIds)
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
    }

    fun editMessage(messageId: String) {
        var removedPersistedIds = emptyList<Long>()
        var restoredDraft: String? = null
        _uiState.update { state ->
            val targetIndex = state.messages.indexOfFirst { it.id == messageId && it.role == "user" }
            if (targetIndex < 0) return@update state

            val target = state.messages[targetIndex]
            val draft = target.editableContent()
            restoredDraft = draft
            val exchangeEnd = state.messages.exchangeEndAfterUser(targetIndex)
            val removedMessages = state.messages.subList(targetIndex, exchangeEnd)
            val removedIds = removedMessages.map { it.id }.toSet()
            removedPersistedIds = removedMessages.mapNotNull { it.id.persistedMessageId() }
            state.copy(
                draft = draft,
                attachments = target.imageUris.toPhotoAttachments(),
                messages = state.messages.take(targetIndex) + state.messages.drop(exchangeEnd),
                pinnedMessageIds = state.pinnedMessageIds - removedIds,
                replyTarget = state.replyTarget?.takeIf { it.id !in removedIds },
                lastPrompt = state.lastPrompt.takeUnlessRemoved(removedIds),
            )
        }
        restoredDraft?.let { savedStateHandle["draft"] = it }
        deletePersistedMessages(removedPersistedIds)
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
    }

    fun cancelQueuedPrompt(id: String) {
        val removedMediaUris = _uiState.value.queuedPrompts
            .firstOrNull { it.id == id }
            ?.imageUris
            .orEmpty()
        _uiState.update { state ->
            state.copy(queuedPrompts = state.queuedPrompts.filterNot { it.id == id })
        }
        cleanupAppOwnedMedia(removedMediaUris)
    }

    fun shareText(): String {
        return _uiState.value.messages.joinToString("\n\n") { message ->
            "${message.role.uppercase()}:\n${message.readableContent()}"
        }
    }

    fun exportConversation(format: ExportFormat): String {
        val state = _uiState.value
        val agentName = state.agentName
        val sessionId = state.sessionId.orEmpty()
        val now = java.time.Instant.now().atZone(java.time.ZoneId.systemDefault())
        val nowStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        val totalUsage = state.messages.mapNotNull { it.usage }.reduceOrNull { acc, u ->
            TokenUsage(
                promptTokens = acc.promptTokens + u.promptTokens,
                completionTokens = acc.completionTokens + u.completionTokens,
                totalTokens = acc.totalTokens + u.totalTokens,
            )
        }

        return when (format) {
            ExportFormat.Markdown -> buildMarkdownExport(state, agentName, sessionId, nowStr, totalUsage)
            ExportFormat.PlainText -> buildPlainTextExport(state, agentName, sessionId, nowStr, totalUsage)
            ExportFormat.Json -> buildJsonExport(state, agentName, sessionId, nowStr, totalUsage)
        }
    }

    private fun buildMarkdownExport(
        state: ChatUiState,
        agentName: String,
        sessionId: String,
        nowStr: String,
        totalUsage: TokenUsage?,
    ): String {
        val builder = StringBuilder()
        builder.append("# Conversation with $agentName\n\n")
        builder.append("## Session: ${state.sessionTitle()}\n")
        builder.append("- **Started:** ${formatExportTimestamp(state.messages.firstOrNull()?.time)}\n")
        builder.append("- **Messages:** ${state.messages.size}\n")
        builder.append("- **Model:** ${state.selectedModel.model}\n")
        if (totalUsage != null) {
            builder.append("- **Token Usage:** ${totalUsage.promptTokens} prompt, ${totalUsage.completionTokens} completion, ${totalUsage.totalTokens} total\n")
        }
        builder.append("- **Exported:** $nowStr\n\n")
        builder.append("---\n\n")

        state.messages.forEach { message ->
            val role = if (message.role == "user") "You" else agentName
            builder.append("### ${message.time} — $role\n\n")
            builder.append(message.readableContent())
            if (message.usage != null) {
                builder.append("\n\n*Tokens: ${message.usage.totalTokens}*")
            }
            builder.append("\n\n---\n\n")
        }

        builder.append("*Exported from Hermes Mobile on $nowStr*")
        return builder.toString()
    }

    private fun buildPlainTextExport(
        state: ChatUiState,
        agentName: String,
        sessionId: String,
        nowStr: String,
        totalUsage: TokenUsage?,
    ): String {
        val builder = StringBuilder()
        builder.append("Conversation with $agentName\n")
        builder.append("Session: ${state.sessionTitle()}\n")
        builder.append("Started: ${formatExportTimestamp(state.messages.firstOrNull()?.time)}\n")
        builder.append("Messages: ${state.messages.size}\n")
        builder.append("Model: ${state.selectedModel.model}\n")
        if (totalUsage != null) {
            builder.append("Token Usage: ${totalUsage.promptTokens} prompt, ${totalUsage.completionTokens} completion, ${totalUsage.totalTokens} total\n")
        }
        builder.append("========================================\n\n")

        state.messages.forEach { message ->
            val role = if (message.role == "user") "You" else agentName
            builder.append("${message.time} — $role:\n")
            builder.append(message.readableContent())
            if (message.usage != null) {
                builder.append("\n[Tokens: ${message.usage.totalTokens}]")
            }
            builder.append("\n\n")
        }

        builder.append("========================================\n")
        builder.append("Exported from Hermes Mobile on $nowStr")
        return builder.toString()
    }

    private fun buildJsonExport(
        state: ChatUiState,
        agentName: String,
        sessionId: String,
        nowStr: String,
        totalUsage: TokenUsage?,
    ): String {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val exportData = ExportData(
            session = ExportSession(
                id = sessionId,
                title = state.sessionTitle(),
                agent = agentName,
                startedAt = formatExportTimestamp(state.messages.firstOrNull()?.time),
                model = state.selectedModel.model,
                messageCount = state.messages.size,
            ),
            usage = totalUsage?.let {
                ExportUsage(it.promptTokens, it.completionTokens, it.totalTokens)
            },
            messages = state.messages.map { msg ->
                ExportMessage(
                    role = msg.role,
                    content = msg.readableContent(),
                    timestamp = msg.time,
                    usage = msg.usage?.let { ExportUsage(it.promptTokens, it.completionTokens, it.totalTokens) },
                )
            },
            exportedAt = nowStr,
        )
        return json.encodeToString(ExportData.serializer(), exportData)
    }

    private fun formatExportTimestamp(time: String?): String {
        return time ?: "Unknown"
    }

    fun stop() {
        _uiState.value.sessionId?.let(streamCoordinator::stop)
        streamJob?.cancel()
        val abandonedMediaUris = _uiState.value.queuedPrompts.flatMap { it.imageUris }
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = false,
                connectionState = state.connectionState.afterStop(),
                queuedPrompts = emptyList(),
                messages = state.messages.map {
                    if (it.isStreaming) it.copy(isStreaming = false, error = "Stopped") else it
                },
            )
        }
        cleanupAppOwnedMedia(abandonedMediaUris)
    }

    private fun ConnectionState.afterStop(): ConnectionState {
        return when (this) {
            ConnectionState.Connected -> ConnectionState.Connected
            is ConnectionState.Error -> this
            ConnectionState.Connecting,
            ConnectionState.Disconnected -> ConnectionState.Disconnected
        }
    }

    private fun startNewSession() {
        _uiState.value.sessionId?.let(streamCoordinator::stop)
        streamJob?.cancel()
        val abandonedMediaUris = _uiState.value.attachments.map { it.uri } +
            _uiState.value.queuedPrompts.flatMap { it.imageUris }
        val nextSessionId = agentIdFromChatSessionId(_uiState.value.sessionId)
            ?.let(::newAgentChatSessionId)
        savedStateHandle["activeSessionId"] = nextSessionId
        savedStateHandle.remove<String>("resolvedFromSessionId")
        savedStateHandle["draft"] = ""
        _uiState.update {
            it.copy(
                sessionId = nextSessionId,
                messages = emptyList(),
                tools = emptyList(),
                draft = "",
                attachments = emptyList(),
                queuedPrompts = emptyList(),
                pinnedMessageIds = emptySet(),
                replyTarget = null,
                isConnecting = false,
                isStreaming = false,
                connectionState = if (nextSessionId == null) ConnectionState.Disconnected else ConnectionState.Connected,
                error = null,
                lastPrompt = null,
            )
        }
        cleanupAppOwnedMedia(abandonedMediaUris)
        visibleSessionId?.let(streamCoordinator::markSessionHidden)
        visibleSessionId = null
        nextSessionId?.let {
            rememberOpenedSession(it)
            markSessionVisible(it)
        }
    }

    private fun undoLastExchange() {
        var removedPersistedIds = emptyList<Long>()
        var removedMediaUris = emptyList<String>()
        savedStateHandle["draft"] = ""
        _uiState.update { state ->
            val lastUserIndex = state.messages.indexOfLast { it.role == "user" }
            if (lastUserIndex < 0) {
                state.copy(draft = "", error = "No exchange to undo")
            } else {
                val removedMessages = state.messages.drop(lastUserIndex)
                val removedIds = removedMessages.map { it.id }.toSet()
                removedPersistedIds = removedMessages.mapNotNull { it.id.persistedMessageId() }
                removedMediaUris = removedMessages.flatMap { it.imageUris }
                state.copy(
                    draft = "",
                    messages = state.messages.take(lastUserIndex),
                    pinnedMessageIds = state.pinnedMessageIds - removedIds,
                    replyTarget = state.replyTarget?.takeIf { it.id !in removedIds },
                    lastPrompt = state.lastPrompt.takeUnlessRemoved(removedIds),
                    error = null,
                )
            }
        }
        cleanupAppOwnedMedia(removedMediaUris)
        deletePersistedMessages(removedPersistedIds)
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
    }

    private fun submit(
        prompt: String,
        displayPrompt: String = prompt,
        imageUris: List<String> = _uiState.value.attachments.previewImageUris(),
    ): Boolean {
        val state = _uiState.value
        val replyText = state.replyTarget?.let(::replyPreviewText)
        val apiPrompt = withReplyContext(prompt, state.replyTarget)
        return submitPreparedPrompt(
            PendingPrompt(
                id = "pending-${messageIds.next()}",
                text = apiPrompt,
                displayText = displayPrompt,
                replyTo = replyText,
                imageUris = imageUris,
            ),
        )
    }

    private fun submitPreparedPrompt(prompt: PendingPrompt): Boolean {
        val state = _uiState.value
        val rateLimitWait = rateLimitWaitSeconds()
        if (rateLimitWait > 0) {
            _uiState.update { it.copy(error = "Please wait $rateLimitWait seconds") }
            return false
        }
        if (state.isConnecting || state.isStreaming) {
            if (state.queuedPrompts.size >= MAX_QUEUE_SIZE) {
                _uiState.update { it.copy(error = "Queue full. Please wait.") }
                return false
            }
            recordSendAttempt()
            savedStateHandle["draft"] = ""
            _uiState.update {
                it.copy(
                    draft = "",
                    attachments = emptyList(),
                    queuedPrompts = it.queuedPrompts + PendingPrompt(
                        id = "queued-${messageIds.next()}",
                        text = prompt.text,
                        displayText = prompt.displayText,
                        replyTo = prompt.replyTo,
                        imageUris = prompt.imageUris,
                    ),
                    replyTarget = null,
                )
            }
            return true
        }
        recordSendAttempt()
        send(prompt.text, displayPrompt = prompt.displayText, replyTo = prompt.replyTo, imageUris = prompt.imageUris)
        return true
    }

    private fun send(
        prompt: String,
        displayPrompt: String = prompt,
        replyTo: String? = null,
        imageUris: List<String> = _uiState.value.attachments.previewImageUris(),
    ) {
        streamJob?.cancel()
        val state = _uiState.value
        val nowMillis = System.currentTimeMillis()
        val now = currentTimeLabel(nowMillis)
        val sessionId = state.sessionId ?: UUID.randomUUID().toString()
        val userMessageId = messageIds.next(nowMillis)
        val assistantMessageId = messageIds.next(nowMillis + 1)
        val userId = "user-$userMessageId"
        val assistantId = "assistant-$assistantMessageId"
        val model = state.selectedModel.model.ifBlank { "hermes-agent" }
        val priorMessages = state.messages.filter { it.error == null && !it.isStreaming }
        val agentInstructions = agentInstructionsForSession(sessionId, state.agents)
        _uiState.update {
            it.copy(
                draft = "",
                attachments = emptyList(),
                isConnecting = true,
                isStreaming = false,
                connectionState = ConnectionState.Connecting,
                error = null,
                tools = emptyList(),
                replyTarget = null,
                lastPrompt = PendingPrompt(
                    id = "retry-$userMessageId",
                    text = prompt,
                    displayText = displayPrompt,
                    replyTo = replyTo,
                    imageUris = imageUris,
                ),
                sessionId = sessionId,
                messages = it.messages +
                    ChatUiMessage(
                        id = userId,
                        role = "user",
                        content = displayPrompt,
                        time = now,
                        replyTo = replyTo,
                        imageUris = imageUris,
                    ) +
                    ChatUiMessage(assistantId, "assistant", "", now, isStreaming = true),
            )
        }
        savedStateHandle["activeSessionId"] = sessionId
        savedStateHandle.remove<String>("resolvedFromSessionId")
        savedStateHandle["draft"] = ""
        rememberOpenedSession(sessionId)
        if (!isCleared) {
            markSessionVisible(sessionId)
        }
        val userMessage = MessageEntity(
            id = userMessageId,
            sessionId = sessionId,
            role = "user",
            content = displayPrompt,
            timestamp = userMessageId,
            imageUrisJson = imageUris.toMessageImageJson(),
        )
        viewModelScope.launch {
            saveSessionSnapshot(sessionId)
            repository.saveLocalMessage(userMessage)
        }
        messageStartTimes[assistantId] = System.currentTimeMillis()
        val sessionSnapshot = _uiState.value.toSessionEntity(sessionId, nowMillis)
        streamJob = streamCoordinator.start(
            ChatStreamCommand(
                session = sessionSnapshot,
                userMessage = userMessage,
                assistantMessageId = assistantMessageId,
                requestBuilder = {
                    if (imageUris.isEmpty() && priorMessages.none { it.imageUris.isNotEmpty() }) {
                        buildCompletionRequest(
                            model = model,
                            priorMessages = priorMessages,
                            prompt = prompt,
                            imageUris = imageUris,
                            systemInstructions = agentInstructions,
                        )
                    } else {
                        withContext(Dispatchers.IO) {
                            buildCompletionRequest(
                                model = model,
                                priorMessages = priorMessages,
                                prompt = prompt,
                                imageUris = imageUris,
                                systemInstructions = agentInstructions,
                            )
                        }
                    }
                },
                onSessionResolved = { resolvedSessionId ->
                    handleResolvedStreamSession(sessionId, resolvedSessionId)
                },
                onFinished = {
                    _uiState.update { state ->
                        state.copy(
                            isConnecting = false,
                            isStreaming = false,
                            connectionState = ConnectionState.Connected,
                            messages = state.messages.map { message ->
                                if (message.id.persistedMessageId() == assistantMessageId) {
                                    message.copy(isStreaming = false)
                                } else {
                                    message
                                }
                            },
                        )
                    }
                    streamJob = null
                    sendNextQueuedIfIdle()
                },
                onFailed = { readable ->
                    failStream(assistantId, readable)
                },
            ),
        )
    }

    private fun ChatUiState.toSessionEntity(sessionId: String, nowMillis: Long): SessionEntity {
        return SessionEntity(
            id = sessionId,
            title = copy(sessionId = sessionId).sessionTitle(),
            source = "mobile",
            startedAt = nowMillis,
            endedAt = null,
            messageCount = messages.count { it.content.isNotBlank() || it.imageUris.isNotEmpty() },
            model = selectedModel.model,
            lastSyncedAt = nowMillis,
            localLastActivityAt = nowMillis,
            lastMessagePreview = messages.lastStablePreview()?.take(200),
            unreadCount = 0,
            lastReadAt = nowMillis,
        )
    }

    private fun observeActiveStreams() {
        viewModelScope.launch {
            streamCoordinator.streams.collect { streams ->
                val sessionId = _uiState.value.sessionId ?: return@collect
                val snapshot = streams[sessionId] ?: return@collect
                var abandonedMediaUris = emptyList<String>()
                _uiState.update { state ->
                    if (state.sessionId != sessionId) return@update state
                    val shouldClearQueue = snapshot.shouldAbandonQueuedPrompts() && state.queuedPrompts.isNotEmpty()
                    if (shouldClearQueue) {
                        abandonedMediaUris = state.queuedPrompts.flatMap { it.imageUris }
                    }
                    val next = state.withStreamSnapshot(snapshot)
                    if (shouldClearQueue) next.copy(queuedPrompts = emptyList()) else next
                }
                cleanupAppOwnedMedia(abandonedMediaUris)
            }
        }
    }

    private fun ChatUiState.withActiveStreamSnapshotIfAny(sessionId: String): ChatUiState {
        val snapshot = streamCoordinator.streams.value[sessionId]
        return if (snapshot == null) this else withStreamSnapshot(snapshot)
    }

    private fun ChatUiState.withStreamSnapshot(snapshot: ChatStreamSnapshot): ChatUiState {
        val userMessage = snapshot.userMessage?.toChatUiMessage()
        val assistantId = "assistant-${snapshot.assistantMessageId}"
        val assistantTime = currentTimeLabel(snapshot.assistantMessageId)
        val messagesWithUser = if (userMessage != null && messages.none { it.id.persistedMessageId() == snapshot.userMessage.id }) {
            messages + userMessage
        } else {
            messages
        }
        val messageIndex = messagesWithUser.indexOfFirst { it.id.persistedMessageId() == snapshot.assistantMessageId }
        val updatedMessages = if (messageIndex >= 0) {
            messagesWithUser.mapIndexed { index, message ->
                if (index == messageIndex) {
                    val nextContent = snapshot.content.ifBlank { message.content }
                    val nextAttachments = (snapshot.receivedAttachments + receivedAttachmentsFromMessage(nextContent))
                        .distinctBy { it.url }
                    message.copy(
                        content = nextContent,
                        reasoning = snapshot.reasoning.ifBlank { message.reasoning },
                        isStreaming = snapshot.isStreaming || snapshot.isConnecting,
                        error = snapshot.error,
                        usage = snapshot.usage ?: message.usage,
                        receivedAttachments = nextAttachments,
                    )
                } else {
                    message
                }
            }
        } else {
            messagesWithUser + ChatUiMessage(
                id = assistantId,
                role = "assistant",
                content = snapshot.content,
                time = assistantTime,
                reasoning = snapshot.reasoning,
                isStreaming = snapshot.isStreaming || snapshot.isConnecting,
                error = snapshot.error,
                usage = snapshot.usage,
                receivedAttachments = (snapshot.receivedAttachments + receivedAttachmentsFromMessage(snapshot.content))
                    .distinctBy { it.url },
            )
        }
        return copy(
            messages = updatedMessages,
            tools = snapshot.tools,
            isConnecting = snapshot.isConnecting,
            isStreaming = snapshot.isStreaming,
            error = snapshot.error,
            connectionState = when {
                snapshot.error != null -> ConnectionState.Error(snapshot.error)
                snapshot.isConnecting -> ConnectionState.Connecting
                else -> ConnectionState.Connected
            },
        )
    }

    private fun markSessionVisible(sessionId: String) {
        if (isCleared) return
        if (visibleSessionId == sessionId) {
            streamCoordinator.markSessionVisible(sessionId)
            return
        }
        visibleSessionId?.let(streamCoordinator::markSessionHidden)
        visibleSessionId = sessionId
        streamCoordinator.markSessionVisible(sessionId)
    }

    override fun onCleared() {
        isCleared = true
        visibleSessionId?.let(streamCoordinator::markSessionHidden)
        visibleSessionId = null
        super.onCleared()
    }

    private fun buildCompletionRequest(
        model: String,
        priorMessages: List<ChatUiMessage>,
        prompt: String,
        imageUris: List<String>,
        systemInstructions: String = "",
    ): ChatCompletionRequest {
        val systemMessages = systemInstructions.takeIf { it.isNotBlank() }
            ?.let { listOf(chatRequestMessage(role = "system", text = it)) }
            .orEmpty()
        return ChatCompletionRequest(
            model = model,
            messages = systemMessages + priorMessages.map { message ->
                chatRequestMessage(
                    role = message.role,
                    text = message.visibleContent(),
                    imageDataUrls = message.imageUris.toImageUrls(),
                )
            } + chatRequestMessage(
                role = "user",
                text = prompt,
                imageDataUrls = imageUris.toImageUrls(),
            ),
        )
    }

    private fun List<String>.toImageUrls(): List<String> = map { it.toImageUrl() }

    private fun String.toImageUrl(): String {
        val cleanUri = trim()
        if (!isAllowedMessageImageUri(cleanUri)) error("Unsupported image source")
        if (cleanUri.startsWith("data:image/", ignoreCase = true)) {
            if (!cleanUri.hasAllowedInlineDataSize(MAX_INLINE_ATTACHMENT_BYTES)) {
                error("Selected photo is too large")
            }
            return cleanUri
        }
        val uri = Uri.parse(cleanUri)
        val mime = appContext.contentResolver.getType(uri)
            ?.takeIf { it.startsWith("image/", ignoreCase = true) }
            ?: "image/jpeg"
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytesWithLimit(MAX_INLINE_ATTACHMENT_BYTES) }
            ?: error("Could not open selected photo")
        return "data:$mime;base64,${Base64.getEncoder().encodeToString(bytes)}"
    }

    private fun handleResolvedStreamSession(localSessionId: String, resolvedSessionId: String) {
        val activeSessionId = resolveOpenedChatSessionId(localSessionId, resolvedSessionId)
        val currentSessionId = _uiState.value.sessionId
        if (currentSessionId != localSessionId && currentSessionId != activeSessionId) return
        _uiState.update { state ->
            state.copy(
                sessionId = activeSessionId,
                isConnecting = false,
                isStreaming = true,
                connectionState = ConnectionState.Connected,
            ).withActiveStreamSnapshotIfAny(activeSessionId)
        }
        rememberOpenedSession(activeSessionId)
        if (!isCleared) {
            markSessionVisible(activeSessionId)
        }
        if (activeSessionId != localSessionId) {
            savedStateHandle["activeSessionId"] = activeSessionId
            savedStateHandle["resolvedFromSessionId"] = localSessionId
            migrateLocalSession(localSessionId, activeSessionId)
        } else {
            savedStateHandle.remove<String>("resolvedFromSessionId")
            persistSessionSnapshot(activeSessionId)
        }
    }

    private fun appendDelta(messageId: String, delta: String) {
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = true,
                connectionState = ConnectionState.Connected,
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
                connectionState = ConnectionState.Connected,
                messages = state.messages.map {
                    if (it.id == messageId) it.copy(isStreaming = false) else it
                },
            )
        }
        persistStreamingAssistant(messageId)
        // Push assistant message to remote
        pushAssistantMessageRemote(messageId)
        _uiState.value.sessionId?.let(::persistSessionSnapshot)
        streamJob = null
        sendNextQueuedIfIdle()
    }

    private fun pushAssistantMessageRemote(messageId: String) {
        val sessionId = _uiState.value.sessionId ?: return
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (message.content.isBlank()) return
        val assistantId = messageId.persistedMessageId() ?: return
        viewModelScope.launch {
            repository.pushMessageToRemote(sessionId, "assistant", message.content, assistantId)
        }
    }

    private fun failStream(messageId: String, readable: String) {
        val abandonedMediaUris = _uiState.value.queuedPrompts.flatMap { it.imageUris }
        _uiState.update { state ->
            state.copy(
                isConnecting = false,
                isStreaming = false,
                connectionState = ConnectionState.Error(readable),
                error = readable,
                queuedPrompts = emptyList(),
                messages = state.messages.map {
                    if (it.id == messageId) it.copy(isStreaming = false, error = readable) else it
                },
            )
        }
        cleanupAppOwnedMedia(abandonedMediaUris)
        streamJob = null
    }

    private fun sendNextQueuedIfIdle() {
        if (isCleared) return
        val next = _uiState.value.queuedPrompts.firstOrNull() ?: return
        if (_uiState.value.isConnecting || _uiState.value.isStreaming) return
        _uiState.update { state -> state.copy(queuedPrompts = state.queuedPrompts.drop(1)) }
        send(next.text, displayPrompt = next.displayText, replyTo = next.replyTo, imageUris = next.imageUris)
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
        val responseProvider = response.provider.firstNonBlankLine().orEmpty()
        val responseModel = response.model.firstNonBlankLine().orEmpty()
        val options = response.providers
            .flatMap { provider ->
                val cleanProvider = provider.slug.firstNonBlankLine() ?: return@flatMap emptyList()
                provider.models.take(40).mapNotNull { model ->
                    val cleanModel = model.firstNonBlankLine() ?: return@mapNotNull null
                    ChatModelOption(
                        provider = cleanProvider,
                        model = cleanModel,
                        label = cleanModel.substringAfterLast('/').ifBlank { cleanModel },
                    )
                }
            }
            .ifEmpty { defaultChatModelOptions }
        val selected = options.firstOrNull {
            it.provider == responseProvider && it.model == responseModel
        } ?: options.firstOrNull { it.model == responseModel } ?: options.first()
        _uiState.update { it.copy(modelOptions = options, selectedModel = selected) }
    }

    private fun loadSession(sessionId: String) {
        rememberOpenedSession(sessionId)
        markSessionVisible(sessionId)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sessionId = sessionId,
                    agentName = agentNameForSession(sessionId, AppPreferences.defaultAgents, explicitAgentName ?: it.agentName),
                    isConnecting = false,
                    error = null,
                    connectionState = ConnectionState.Connected,
                ).withActiveStreamSnapshotIfAny(sessionId)
            }
            val cachedEntities = cachedMessagesForSession(sessionId)
            messageIds.seed(cachedEntities.maxOfOrNull { it.id } ?: 0L)
            _uiState.update {
                val cachedMessages = cachedEntities.map(MessageEntity::toChatUiMessage)
                it.copy(
                    sessionId = sessionId,
                    messages = cachedMessages.ifEmpty { it.messages },
                    isConnecting = false,
                    connectionState = ConnectionState.Connected,
                ).withActiveStreamSnapshotIfAny(sessionId)
            }

            var syncError: String? = null
            repository.syncMessages(sessionId)
                .onFailure { error ->
                    if (!ErrorMapper.isEndpointNotFound(error)) {
                        syncError = ErrorMapper.userMessage(error, "Session sync failed")
                    }
                }
            val syncedEntities = cachedMessagesForSession(sessionId)
            val entities = restoreMessagesMissingAfterSync(sessionId, cachedEntities, syncedEntities)
            messageIds.seed(entities.maxOfOrNull { it.id } ?: 0L)
            val messages = entities.map(MessageEntity::toChatUiMessage)
            _uiState.update {
                val next = it.copy(
                    sessionId = sessionId,
                    messages = messages.ifEmpty { it.messages },
                    isConnecting = false,
                    connectionState = syncError?.let(ConnectionState::Error) ?: ConnectionState.Connected,
                    error = syncError,
                )
                next.withActiveStreamSnapshotIfAny(sessionId)
            }
        }
    }

    private suspend fun cachedMessagesForSession(sessionId: String): List<MessageEntity> {
        return runCatching { repository.cachedMessages(sessionId) }.getOrDefault(emptyList())
    }

    private suspend fun restoreMessagesMissingAfterSync(
        sessionId: String,
        beforeSync: List<MessageEntity>,
        afterSync: List<MessageEntity>,
    ): List<MessageEntity> {
        if (beforeSync.isEmpty()) return afterSync
        val afterKeys = afterSync.mapTo(mutableSetOf()) { it.sessionId to it.id }
        val missing = beforeSync.filter { (it.sessionId to it.id) !in afterKeys }
        if (missing.isNotEmpty()) {
            repository.saveLocalMessages(missing.map { it.copy(sessionId = sessionId) })
        }
        return (afterSync + missing)
            .distinctBy { it.sessionId to it.id }
            .sortedWith(compareBy<MessageEntity> { it.timestamp }.thenBy { it.id })
    }

    private fun observeAgentProfileNames() {
        viewModelScope.launch {
            val agentFlow = runCatching { appPreferences.agents }.getOrNull() ?: return@launch
            agentFlow.catch { emit(AppPreferences.defaultAgents) }.collect { agents ->
                _uiState.update { state ->
                    val resolved = agentNameForSession(
                        sessionId = state.sessionId,
                        agents = agents,
                        fallback = explicitAgentName ?: state.agentName,
                    )
                    val avatarUri = agentAvatarUriForSession(
                        sessionId = state.sessionId,
                        agents = agents,
                    )
                    state.copy(
                        agents = agents,
                        agentName = resolved,
                        agentAvatarUri = avatarUri,
                    )
                }
            }
        }
    }

    private fun agentAvatarUriForSession(sessionId: String?, agents: List<AgentProfile>): String? {
        val agentId = agentIdFromChatSessionId(sessionId) ?: return null
        return agents.firstOrNull { it.id == agentId }?.avatarUri
    }

    fun switchAgent(agentId: String) {
        _uiState.value.sessionId?.let(streamCoordinator::stop)
        streamJob?.cancel()
        val agents = _uiState.value.agents
        val agent = agents.firstOrNull { it.id == agentId } ?: return
        val sessionId = newAgentChatSessionId(agentId)
        val abandonedMediaUris = _uiState.value.attachments.map { it.uri } +
            _uiState.value.queuedPrompts.flatMap { it.imageUris }
        savedStateHandle["activeSessionId"] = sessionId
        savedStateHandle.remove<String>("resolvedFromSessionId")
        savedStateHandle["draft"] = ""
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                agentName = agent.name,
                agentAvatarUri = agent.avatarUri,
                messages = emptyList(),
                tools = emptyList(),
                draft = "",
                attachments = emptyList(),
                queuedPrompts = emptyList(),
                pinnedMessageIds = emptySet(),
                replyTarget = null,
                isConnecting = false,
                isStreaming = false,
                connectionState = ConnectionState.Connected,
                error = null,
                lastPrompt = null,
            )
        }
        cleanupAppOwnedMedia(abandonedMediaUris)
        rememberOpenedSession(sessionId)
        markSessionVisible(sessionId)
    }

    private fun persistSessionSnapshot(sessionId: String) {
        viewModelScope.launch { saveSessionSnapshot(sessionId) }
    }

    private suspend fun saveSessionSnapshot(sessionId: String) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val existing = runCatching { repository.cachedSession(sessionId) }.getOrNull()
        val lastMessagePreview = state.messages.lastStablePreview()?.take(200)
        repository.saveLocalSession(
            SessionEntity(
                id = sessionId,
                title = state.copy(sessionId = sessionId).sessionTitle(),
                source = existing?.source ?: "mobile",
                startedAt = existing?.startedAt ?: now,
                endedAt = existing?.endedAt,
                messageCount = state.messages.count { it.content.isNotBlank() || it.imageUris.isNotEmpty() },
                model = existing?.model ?: state.selectedModel.model,
                lastSyncedAt = existing?.lastSyncedAt ?: now,
                localLastActivityAt = now,
                lastMessagePreview = lastMessagePreview,
                unreadCount = if (visibleSessionId == sessionId) 0 else existing?.unreadCount ?: 0,
                lastReadAt = if (visibleSessionId == sessionId) now else existing?.lastReadAt,
            ),
        )
    }

    private fun persistMessage(
        sessionId: String,
        id: Long,
        role: String,
        content: String,
        reasoning: String = "",
        imageUris: List<String> = emptyList(),
        receivedAttachments: List<ReceivedAttachment> = emptyList(),
    ) {
        viewModelScope.launch {
            repository.saveLocalMessage(
                MessageEntity(
                    id = id,
                    sessionId = sessionId,
                    role = role,
                    content = content,
                    reasoning = reasoning,
                    timestamp = id,
                    imageUrisJson = imageUris.toMessageImageJson(),
                    receivedAttachmentsJson = receivedAttachments.toReceivedAttachmentsJson(),
                ),
            )
        }
    }

    private fun persistStreamingAssistant(messageId: String) {
        val sessionId = _uiState.value.sessionId ?: return
        val id = messageId.persistedMessageId() ?: return
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (message.content.isBlank() && message.reasoningContent().isBlank() && message.receivedAttachments.isEmpty()) return
        persistMessage(
            sessionId = sessionId,
            id = id,
            role = "assistant",
            content = message.content,
            reasoning = message.reasoningContent(),
            receivedAttachments = message.receivedAttachments,
        )
    }

    private fun deletePersistedMessages(messageIds: List<Long>) {
        val sessionId = _uiState.value.sessionId ?: return
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            repository.deleteLocalMessages(sessionId, messageIds)
        }
    }

    private fun cleanupAppOwnedMedia(uris: Collection<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            deleteAppOwnedMessageMedia(appContext, uris)
        }
    }

    private fun rememberOpenedSession(sessionId: String) {
        viewModelScope.launch {
            appPreferences.setLastOpenedChatSessionId(sessionId)
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
                reasoning = message.reasoning,
                timestamp = id,
                imageUrisJson = message.imageUris.toMessageImageJson(),
                receivedAttachmentsJson = message.receivedAttachments.toReceivedAttachmentsJson(),
            )
        }
        val lastMessagePreview = state.messages.lastStablePreview()?.take(200)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.saveLocalSession(
                SessionEntity(
                    id = remoteSessionId,
                    title = state.sessionTitle(),
                    source = "mobile",
                    startedAt = messages.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
                    endedAt = null,
                    messageCount = state.messages.count { it.content.isNotBlank() || it.imageUris.isNotEmpty() },
                    model = state.selectedModel.model,
                    lastSyncedAt = now,
                    localLastActivityAt = now,
                    lastMessagePreview = lastMessagePreview,
                    unreadCount = 0,
                    lastReadAt = now,
                ),
            )
            repository.saveLocalMessages(messages)
            repository.deleteLocalSession(localSessionId)
        }
    }

    private fun currentTimeLabel(timestamp: Long = System.currentTimeMillis()): String {
        return formatChatClockTime(timestamp)
    }

    private fun withReplyContext(prompt: String, replyTarget: ChatUiMessage?): String {
        if (replyTarget == null) return prompt
        return "Replying to ${replyTarget.role}: ${replyPreviewText(replyTarget)}\n\n$prompt"
    }

    private fun replyPreviewText(message: ChatUiMessage): String {
        return message.readableContent().lineSequence().firstOrNull().orEmpty().take(180)
    }

}

internal fun ChatUiMessage.readableContent(): String {
    val visibleContent = visibleContent()
    return readableMessageText(visibleContent, imageUris.size).ifBlank { receivedAttachmentSummary() }
}

internal fun ChatUiMessage.previewContent(): String {
    return readableContent().ifBlank { STREAMING_PREVIEW }
}

internal fun ChatUiMessage.editableContent(): String {
    return visibleContent()
}

internal fun ChatUiMessage.visibleContent(): String {
    return visibleReceivedAttachmentText(visibleMessageText(splitReasoningFromContent(content).content, imageUris.size))
}

internal fun ChatUiMessage.reasoningContent(): String {
    val extracted = splitReasoningFromContent(content).reasoning
    return listOf(reasoning, extracted)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n\n")
}

internal data class ReasoningSplit(
    val content: String,
    val reasoning: String,
)

internal fun splitReasoningFromContent(content: String): ReasoningSplit {
    if (content.isBlank()) return ReasoningSplit(content = "", reasoning = "")
    val visibleBuilder = StringBuilder()
    val reasoningBlocks = mutableListOf<String>()
    var cursor = 0
    var inReasoning = false
    while (cursor < content.length) {
        if (inReasoning) {
            val close = thinkCloseTagPattern.find(content, cursor)
            if (close == null) {
                reasoningBlocks += content.substring(cursor)
                cursor = content.length
            } else {
                reasoningBlocks += content.substring(cursor, close.range.first)
                cursor = close.range.last + 1
                inReasoning = false
            }
        } else {
            val open = thinkOpenTagPattern.find(content, cursor)
            if (open == null) {
                visibleBuilder.append(content.substring(cursor))
                cursor = content.length
            } else {
                visibleBuilder.append(content.substring(cursor, open.range.first))
                cursor = open.range.last + 1
                inReasoning = true
            }
        }
    }
    val reasoning = reasoningBlocks
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
    val visible = visibleBuilder.toString()
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
    return ReasoningSplit(
        content = visible,
        reasoning = reasoning,
    )
}

internal fun PendingPrompt.readableContent(): String {
    return readableMessageText(displayText, imageUris.size)
}

private fun List<ChatUiMessage>.exchangeEndAfterUser(userIndex: Int): Int {
    return drop(userIndex + 1)
        .indexOfFirst { it.role == "user" }
        .let { if (it < 0) size else userIndex + 1 + it }
}

private fun PendingPrompt?.takeUnlessRemoved(removedMessageIds: Set<String>): PendingPrompt? {
    return this?.takeUnless { it.userMessageId() in removedMessageIds }
}

private fun PendingPrompt.userMessageId(): String? {
    val sentAt = id.removePrefix("retry-")
    return if (sentAt == id) null else "user-$sentAt"
}

private fun ChatUiMessage.receivedAttachmentSummary(): String {
    return when (receivedAttachments.size) {
        0 -> ""
        1 -> receivedAttachments.first().kind.name
        else -> "${receivedAttachments.size} attachments"
    }
}

internal fun buildChatPromptPayload(
    prompt: String,
    attachments: List<ChatAttachment>,
): ChatPromptPayload {
    if (attachments.isEmpty()) return ChatPromptPayload(apiPrompt = prompt, displayPrompt = prompt)

    val hasImage = attachments.any { it.normalizedKind() == "image" }
    val attachmentText = attachments
        .filterNot { it.normalizedKind() == "image" }
        .joinToString("\n", transform = ::formatAttachmentForPrompt)
    val promptText = prompt.ifBlank { if (hasImage) "Photo attached." else "" }
    val apiPrompt = listOf(
        promptText.takeIf { it.isNotBlank() },
        attachmentText.takeIf { it.isNotBlank() },
    ).filterNotNull().joinToString("\n\n")

    return ChatPromptPayload(
        apiPrompt = apiPrompt,
        displayPrompt = prompt.ifBlank { attachments.displayLabelForBlankPrompt() },
    )
}

private fun List<ChatAttachment>.displayLabelForBlankPrompt(): String {
    val visibleAttachments = filterNot { it.normalizedKind() == "image" }
    if (visibleAttachments.isEmpty()) return ""

    if (visibleAttachments.size == 1) {
        val attachment = visibleAttachments.first()
        return when (attachment.normalizedKind()) {
            "voice" -> "Voice note"
            else -> attachment.displayLabel("Attachment")
        }
    }

    return "${visibleAttachments.size} attachments"
}

private fun List<ChatAttachment>.previewImageUris(): List<String> {
    return filter { it.normalizedKind() == "image" }
        .map { it.uri.trim() }
        .filter(::isAllowedMessageImageUri)
        .distinct()
}

private fun List<ChatAttachment>.sentAttachmentCleanupUris(): List<String> {
    return filterNot { it.normalizedKind() == "image" }
        .map { it.uri.trim() }
        .filter { it.isNotBlank() }
}

private fun List<String>.toPhotoAttachments(): List<ChatAttachment> {
    return restoredPhotoAttachments(this)
}

internal fun restoredPhotoAttachments(imageUris: List<String>): List<ChatAttachment> {
    val cleanUris = imageUris.map { it.trim() }.filter(::isAllowedMessageImageUri).distinct()
    return cleanUris.mapIndexed { index, uri ->
        ChatAttachment(
            id = "image-edit-${System.currentTimeMillis()}-$index",
            label = if (cleanUris.size == 1) "Photo" else "Photo ${index + 1}",
            uri = uri,
            kind = "image",
        )
    }
}

private fun List<String>.photoSummary(): String {
    return formatPhotoSummary(size)
}

private fun formatAttachmentForPrompt(attachment: ChatAttachment): String {
    val kind = attachment.normalizedKind()
    return when {
        kind == "image" -> "Photo attached."
        kind == "voice" && attachment.uri.startsWith("file://") -> {
            runCatching {
                val file = attachment.uri.toLocalFile()
                if (file.length() > MAX_INLINE_ATTACHMENT_BYTES) error("Voice note is too large")
                val payload = file.inputStream().use { it.readBytesWithLimit(MAX_INLINE_ATTACHMENT_BYTES) }
                    .let { Base64.getEncoder().encodeToString(it) }
                "Voice note attached: data:audio/mp4;base64,$payload"
            }.getOrDefault("Voice note attached.")
        }
        kind == "voice" -> "Voice note attached."
        else -> "${attachment.displayLabel("File")} attached."
    }
}

private fun ChatAttachment.displayLabel(fallback: String): String {
    return label.cleanAttachmentLabel(fallback)
}

private fun String.cleanAttachmentKind(fallback: String): String {
    val cleanKind = firstNonBlankLine()?.lowercase(Locale.US) ?: fallback
    return when {
        cleanKind == "photo" || cleanKind.startsWith("image/") -> "image"
        cleanKind == "audio" || cleanKind == "voice note" || cleanKind == "voice-note" || cleanKind.startsWith("audio/") -> "voice"
        else -> cleanKind
    }
}

private fun ChatAttachment.normalizedKind(): String {
    return kind.cleanAttachmentKind("file")
}

private fun ChatStreamSnapshot.shouldAbandonQueuedPrompts(): Boolean {
    return error != null &&
        !isStreaming &&
        error != STREAM_NON_STREAMING_FALLBACK_MESSAGE
}

private fun String.toLocalFile(): File {
    return runCatching { File(java.net.URI.create(this)) }
        .getOrElse {
            File(removePrefix("file://").removePrefix("file:"))
        }
}

private fun InputStream.readBytesWithLimit(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_ATTACHMENT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) error("Attachment is too large")
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun String.hasAllowedInlineDataSize(maxBytes: Int): Boolean {
    val payload = substringAfter(',', missingDelimiterValue = "")
    if (payload.isBlank()) return false
    val padding = payload.takeLastWhile { it == '=' }.length.coerceAtMost(2)
    val approximateBytes = (payload.length * 3 / 4) - padding
    return approximateBytes in 1..maxBytes
}

private fun String.cleanAttachmentLabel(fallback: String): String {
    return (firstNonBlankLine() ?: fallback).compactAttachmentLabel()
}

private fun String.compactAttachmentLabel(): String {
    if (length <= MAX_ATTACHMENT_LABEL_LENGTH) return this
    val head = take(MAX_ATTACHMENT_LABEL_LENGTH - 3).trimEnd()
    val wordBoundary = head.lastIndexOf(' ').takeIf { it >= MIN_ATTACHMENT_LABEL_WORD_BOUNDARY }
    return (wordBoundary?.let { head.take(it) } ?: head) + "..."
}

private fun String.firstNonBlankLine(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}

private fun List<ChatUiMessage>.lastStablePreview(): String? {
    for (message in asReversed()) {
        if (message.isStreaming && message.content.isBlank() && message.imageUris.isEmpty() && message.receivedAttachments.isEmpty()) continue
        val preview = message.readableContent().firstNonBlankLine() ?: continue
        if (preview == STREAMING_PREVIEW) continue
        return preview
    }
    return null
}

private fun ChatUiState.withAddedAttachment(attachment: ChatAttachment): ChatUiState {
    val duplicate = attachments.any { it.normalizedKind() == attachment.normalizedKind() && it.uri == attachment.uri }
    return if (duplicate) this else copy(attachments = attachments + attachment)
}

private fun String.modelCommandRequest(): String? {
    val cleanCommand = firstNonBlankLine() ?: return null
    val verbEnd = cleanCommand.indexOfFirst { it.isWhitespace() }.takeIf { it >= 0 } ?: return null
    val verb = cleanCommand.take(verbEnd)
    if (!verb.equals("/model", ignoreCase = true)) return null
    return cleanCommand.drop(verbEnd).trim().takeIf { it.isNotBlank() }
}

private fun SavedStateHandle.cleanString(key: String): String? {
    return get<String>(key)
        ?.trim()
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

internal fun formatChatClockTime(timestamp: Long): String {
    if (timestamp !in 1..MAX_REASONABLE_CHAT_TIMESTAMP_MILLIS) return CHAT_CLOCK_PLACEHOLDER
    return runCatching {
        val time = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        "%02d:%02d".format(time.hour, time.minute)
    }.getOrDefault(CHAT_CLOCK_PLACEHOLDER)
}

private fun String.persistedMessageId(): Long? = substringAfterLast("-", missingDelimiterValue = "").toLongOrNull()

internal class MonotonicIdGenerator(
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private var last: Long = 0L

    fun next(candidate: Long = now()): Long {
        val value = if (candidate > last) candidate else last + 1
        last = value
        return value
    }

    fun seed(id: Long) {
        if (id > last) last = id
    }
}

private fun MessageEntity.toChatUiMessage(): ChatUiMessage {
    val imageUris = imageUrisJson.toMessageImageUris().ifEmpty { legacyImageUrisFromText(content) }
    val storedAttachments = receivedAttachmentsJson.toReceivedAttachments()
    val receivedAttachments = if (role == "user") {
        emptyList()
    } else {
        (storedAttachments + receivedAttachmentsFromMessage(content)).distinctBy { it.url }
    }
    return ChatUiMessage(
        id = "history-$id",
        role = role,
        content = content,
        reasoning = reasoning,
        time = formatChatClockTime(timestamp),
        imageUris = imageUris,
        receivedAttachments = receivedAttachments,
    )
}

private fun ChatUiState.sessionTitle(): String {
    return deriveChatSessionTitle(agentName, sessionId, messages)
}

internal fun deriveChatSessionTitle(
    agentName: String,
    sessionId: String?,
    messages: List<ChatUiMessage>,
): String {
    val firstUser = messages.firstOrNull { it.role == "user" }
    val firstUserText = firstUser?.readableContent()?.firstNonBlankLine().orEmpty()
    val photoOnlyTitle = firstUser?.imageUris?.isNotEmpty() == true &&
        (firstUserText.isBlank() || firstUserText == formatPhotoSummary(firstUser.imageUris.size))
    return when {
        agentIdFromChatSessionId(sessionId) != null -> agentName
        photoOnlyTitle -> "Photo chat"
        firstUserText.isNotBlank() -> firstUserText.compactGeneratedSessionTitle()
        else -> "Hermes chat"
    }
}

internal fun agentNameForSession(
    sessionId: String?,
    agents: List<AgentProfile>,
    fallback: String,
): String {
    val agentId = agentIdFromChatSessionId(sessionId) ?: return fallback
    return agents
        .firstOrNull { it.id == agentId }
        ?.name
        ?.firstNonBlankLine()
        ?: fallback
}

private fun String.compactGeneratedSessionTitle(): String {
    if (length <= MAX_GENERATED_SESSION_TITLE_LENGTH) return this
    val head = take(MAX_GENERATED_SESSION_TITLE_LENGTH - 3).trimEnd()
    val boundary = head.lastIndexOf(' ').takeIf { it >= MIN_GENERATED_SESSION_TITLE_WORD_BOUNDARY }
    return (boundary?.let { head.take(it) } ?: head) + "..."
}

private fun List<String>.toMessageImageJson(): String {
    return Json.encodeToString(this)
}

private fun String.toMessageImageUris(): List<String> {
    return messageImageUrisFromJson(this)
}

private fun List<ReceivedAttachment>.toReceivedAttachmentsJson(): String {
    return Json.encodeToString(this)
}

private fun String.toReceivedAttachments(): List<ReceivedAttachment> {
    return runCatching { Json.decodeFromString<List<ReceivedAttachment>>(this) }.getOrDefault(emptyList())
}

private fun agentInstructionsForSession(sessionId: String?, agents: List<AgentProfile>): String {
    val agentId = agentIdFromChatSessionId(sessionId) ?: return ""
    return agents
        .firstOrNull { it.id == agentId }
        ?.instructions
        ?.trim()
        .orEmpty()
}

private const val MAX_QUEUE_SIZE = 10
private const val MAX_ATTACHMENT_LABEL_LENGTH = 48
private const val MIN_ATTACHMENT_LABEL_WORD_BOUNDARY = 24
private const val MAX_GENERATED_SESSION_TITLE_LENGTH = 72
private const val MIN_GENERATED_SESSION_TITLE_WORD_BOUNDARY = 24
private const val MAX_RETRIES = 3
private const val CHAT_CLOCK_PLACEHOLDER = "--:--"
private const val STREAMING_PREVIEW = "Streaming..."
private const val STREAM_NON_STREAMING_FALLBACK_MESSAGE = "Retrying without streaming..."
private const val MAX_REASONABLE_CHAT_TIMESTAMP_MILLIS = 253_402_300_799_999L
private val RETRY_BACKOFF_MS = longArrayOf(2_000L, 4_000L, 8_000L)
private const val RATE_LIMIT_WINDOW_MS = 10_000L
private const val RATE_LIMIT_MAX_MESSAGES = 5
private const val MAX_INLINE_ATTACHMENT_BYTES = 8 * 1024 * 1024
private const val DEFAULT_ATTACHMENT_BUFFER_SIZE = 8 * 1024
private val thinkOpenTagPattern = Regex("""(?is)<think(?:ing)?>""")
private val thinkCloseTagPattern = Regex("""(?is)</think(?:ing)?>""")

enum class ExportFormat {
    Markdown,
    PlainText,
    Json,
}

@kotlinx.serialization.Serializable
data class ExportData(
    val session: ExportSession,
    val usage: ExportUsage?,
    val messages: List<ExportMessage>,
    val exportedAt: String,
)

@kotlinx.serialization.Serializable
data class ExportSession(
    val id: String,
    val title: String,
    val agent: String,
    val startedAt: String,
    val model: String,
    val messageCount: Int,
)

@kotlinx.serialization.Serializable
data class ExportUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

@kotlinx.serialization.Serializable
data class ExportMessage(
    val role: String,
    val content: String,
    val timestamp: String,
    val usage: ExportUsage?,
)
