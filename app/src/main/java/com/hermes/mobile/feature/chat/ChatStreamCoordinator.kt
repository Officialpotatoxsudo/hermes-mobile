package com.hermes.mobile.feature.chat

import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.TokenUsage
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.network.SseEvent
import com.hermes.mobile.core.util.ReceivedAttachment
import com.hermes.mobile.core.util.resolveOpenedChatSessionId
import com.hermes.mobile.core.util.visibleReceivedAttachmentText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ChatStreamSnapshot(
    val sessionId: String,
    val assistantMessageId: Long,
    val userMessage: MessageEntity? = null,
    val content: String = "",
    val reasoning: String = "",
    val receivedAttachments: List<ReceivedAttachment> = emptyList(),
    val tools: List<ToolProgress> = emptyList(),
    val usage: TokenUsage? = null,
    val isConnecting: Boolean = true,
    val isStreaming: Boolean = false,
    val error: String? = null,
)

data class ChatStreamCommand(
    val session: SessionEntity,
    val userMessage: MessageEntity,
    val assistantMessageId: Long,
    val requestBuilder: suspend () -> ChatCompletionRequest,
    val onSessionResolved: suspend (String) -> Unit = {},
    val onFinished: () -> Unit = {},
    val onFailed: (String) -> Unit = {},
)

@Singleton
class ChatStreamCoordinator @Inject constructor(
    private val repository: HermesRepository,
    private val notifications: ChatNotificationManager? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val visibleSessionIds = MutableStateFlow<Set<String>>(emptySet())
    private val appInForeground = MutableStateFlow(true)
    private val _streams = MutableStateFlow<Map<String, ChatStreamSnapshot>>(emptyMap())
    val streams: StateFlow<Map<String, ChatStreamSnapshot>> = _streams.asStateFlow()

    fun markSessionVisible(sessionId: String) {
        val cleanSessionId = sessionId.takeIf { it.isNotBlank() } ?: return
        visibleSessionIds.update { it + cleanSessionId }
        scope.launch {
            runCatching { repository.markSessionRead(cleanSessionId) }
        }
    }

    fun markSessionHidden(sessionId: String) {
        if (sessionId.isBlank()) return
        visibleSessionIds.update { it - sessionId }
    }

    fun markAppForegrounded() {
        appInForeground.value = true
    }

    fun markAppBackgrounded() {
        appInForeground.value = false
    }

    fun start(command: ChatStreamCommand): Job {
        jobs[command.session.id]?.takeIf { it.isActive }?.let { return it }
        runCatching { notifications?.startStreamingService() }
        val job = scope.launch {
            runStream(command)
        }
        jobs[command.session.id] = job
        job.invokeOnCompletion {
            jobs.remove(command.session.id, job)
            jobs.entries
                .filter { it.value == job }
                .map { it.key }
                .forEach { sessionId -> jobs.remove(sessionId, job) }
            stopStreamingServiceIfIdle()
        }
        return job
    }

    fun stop(sessionId: String) {
        jobs.remove(sessionId)?.cancel()
        clearSnapshot(sessionId)
        stopStreamingServiceIfIdle()
    }

    private suspend fun runStream(command: ChatStreamCommand) {
        val originalSessionId = command.session.id
        var activeSession = command.session
        var activeUserMessage = command.userMessage
        var activeSessionId = originalSessionId
        var snapshot = ChatStreamSnapshot(
            sessionId = activeSessionId,
            assistantMessageId = command.assistantMessageId,
            userMessage = activeUserMessage,
            isConnecting = true,
        )
        var completed = false
        var rawContent = ""
        var content = ""
        var reasoningFromEvents = ""
        var reasoning = ""
        var receivedAttachments = emptyList<ReceivedAttachment>()
        var usage: TokenUsage? = null
        var lastError: Throwable? = null
        var userMessageRemotePushed = false
        updateSnapshot(snapshot)
        try {
            val request = try {
                command.requestBuilder()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val readable = ErrorMapper.userMessage(error, "Could not read selected photo")
                fail(activeSessionId, snapshot.copy(isConnecting = false, isStreaming = false, error = readable), command, readable)
                return
            }
            saveInitialLocalState(command)
            var nonStreamingFallbackTried = false

            suspend fun handleEvent(event: SseEvent) {
                when (event) {
                    is SseEvent.Opened -> {
                        val resolvedSessionId = resolveOpenedChatSessionId(originalSessionId, event.sessionId)
                        if (resolvedSessionId != activeSessionId) {
                            val previousSessionId = activeSessionId
                            activeSessionId = resolvedSessionId
                            activeSession = activeSession.copy(id = resolvedSessionId)
                            activeUserMessage = activeUserMessage.copy(sessionId = resolvedSessionId)
                            retargetJob(previousSessionId, resolvedSessionId)
                            retargetVisibility(previousSessionId, resolvedSessionId)
                            saveResolvedLocalState(activeSession, activeUserMessage)
                            command.onSessionResolved(resolvedSessionId)
                            clearSnapshot(previousSessionId)
                        }
                        snapshot = snapshot.copy(isConnecting = false, isStreaming = true, error = null)
                        if (snapshot.sessionId != activeSessionId || snapshot.userMessage?.sessionId != activeSessionId) {
                            snapshot = snapshot.copy(
                                sessionId = activeSessionId,
                                userMessage = activeUserMessage,
                            )
                        }
                        if (!userMessageRemotePushed) {
                            pushUserMessageRemote(activeUserMessage)
                            userMessageRemotePushed = true
                        }
                        updateSnapshot(snapshot)
                    }
                    is SseEvent.Delta -> {
                        if (!userMessageRemotePushed) {
                            pushUserMessageRemote(activeUserMessage)
                            userMessageRemotePushed = true
                        }
                        rawContent += event.text
                        val split = splitReasoningFromContent(rawContent)
                        content = split.content
                        reasoning = combineReasoning(reasoningFromEvents, split.reasoning)
                        snapshot = snapshot.copy(
                            content = content,
                            reasoning = reasoning,
                            isConnecting = false,
                            isStreaming = true,
                            error = null,
                        )
                        updateSnapshot(snapshot)
                        saveAssistantProgress(activeSessionId, command.assistantMessageId, content, reasoning, receivedAttachments, usage)
                    }
                    is SseEvent.ReasoningDelta -> {
                        reasoningFromEvents += event.text
                        reasoning = combineReasoning(reasoningFromEvents, splitReasoningFromContent(rawContent).reasoning)
                        snapshot = snapshot.copy(
                            reasoning = reasoning,
                            isConnecting = false,
                            isStreaming = true,
                            error = null,
                        )
                        updateSnapshot(snapshot)
                        saveAssistantProgress(activeSessionId, command.assistantMessageId, content, reasoning, receivedAttachments, usage)
                    }
                    is SseEvent.Attachment -> {
                        receivedAttachments = (receivedAttachments + event.attachment).distinctBy { it.url }
                        snapshot = snapshot.copy(
                            receivedAttachments = receivedAttachments,
                            isConnecting = false,
                            isStreaming = true,
                            error = null,
                        )
                        updateSnapshot(snapshot)
                        saveAssistantProgress(activeSessionId, command.assistantMessageId, content, reasoning, receivedAttachments, usage)
                    }
                    is SseEvent.Tool -> {
                        snapshot = snapshot.copy(
                            tools = snapshot.tools.withToolProgress(event.progress),
                            isConnecting = false,
                            isStreaming = true,
                            error = null,
                        )
                        updateSnapshot(snapshot)
                    }
                    is SseEvent.Usage -> {
                        usage = event.usage
                        snapshot = snapshot.copy(usage = usage)
                        updateSnapshot(snapshot)
                        saveAssistantProgress(activeSessionId, command.assistantMessageId, content, reasoning, receivedAttachments, usage)
                    }
                    SseEvent.Done -> {
                        if (!userMessageRemotePushed) {
                            pushUserMessageRemote(activeUserMessage)
                            userMessageRemotePushed = true
                        }
                        completed = true
                        snapshot = snapshot.copy(
                            content = content,
                            reasoning = reasoning,
                            receivedAttachments = receivedAttachments,
                            usage = usage,
                            isConnecting = false,
                            isStreaming = false,
                            error = null,
                        )
                        updateSnapshot(snapshot)
                        finish(command, activeSessionId, content, reasoning, receivedAttachments)
                    }
                }
            }

            for (attempt in 0..MAX_STREAM_RETRIES) {
                lastError = null
                if (attempt > 0) {
                    snapshot = snapshot.copy(
                        isConnecting = true,
                        isStreaming = false,
                        error = "Reconnecting... (attempt $attempt/$MAX_STREAM_RETRIES)",
                    )
                    updateSnapshot(snapshot)
                    delay(RETRY_BACKOFF_MS[attempt - 1])
                }
                try {
                    repository.streamChat(request, activeSessionId)
                        .catch { error -> lastError = error }
                        .collectStreamEvents { event -> handleEvent(event) }
                } catch (error: Throwable) {
                    error.streamTimeoutCause()?.let { lastError = it }
                        ?: if (error is CancellationException) throw error else {
                            lastError = error
                        }
                }
                if (completed) return
                if (!nonStreamingFallbackTried && content.isBlank() && lastError.isStreamTimeout()) {
                    nonStreamingFallbackTried = true
                    snapshot = snapshot.copy(
                        isConnecting = true,
                        isStreaming = false,
                        error = "Retrying without streaming...",
                    )
                    updateSnapshot(snapshot)
                    runCatching {
                        repository.completeChat(request, activeSessionId)
                            .forEach { event -> handleEvent(event) }
                    }.onFailure { error ->
                        lastError = error
                    }
                    if (completed) return
                }
                if (content.isBlank() && lastError != null && !lastError.isStreamTimeout()) break
                if (lastError == null && content.isNotBlank()) {
                    if (!userMessageRemotePushed) {
                        pushUserMessageRemote(activeUserMessage)
                        userMessageRemotePushed = true
                    }
                    snapshot = snapshot.copy(
                        content = content,
                        reasoning = reasoning,
                        receivedAttachments = receivedAttachments,
                        usage = usage,
                        isConnecting = false,
                        isStreaming = false,
                        error = null,
                    )
                    updateSnapshot(snapshot)
                    finish(command, activeSessionId, content, reasoning, receivedAttachments)
                    return
                }
                if ((content.isNotBlank() || reasoning.isNotBlank() || receivedAttachments.isNotEmpty()) && lastError != null) break
                if (attempt == MAX_STREAM_RETRIES) break
            }
            val readable = ErrorMapper.userMessage(lastError, "Connection lost")
            fail(activeSessionId, snapshot.copy(isConnecting = false, isStreaming = false, error = readable), command, readable)
        } catch (cancelled: CancellationException) {
            clearSnapshot(activeSessionId)
            throw cancelled
        }
    }

    private fun retargetJob(previousSessionId: String, resolvedSessionId: String) {
        if (previousSessionId == resolvedSessionId) return
        jobs[previousSessionId]?.let { job ->
            jobs.remove(previousSessionId, job)
            jobs[resolvedSessionId] = job
        }
    }

    private fun retargetVisibility(previousSessionId: String, resolvedSessionId: String) {
        if (previousSessionId == resolvedSessionId) return
        visibleSessionIds.update { visible ->
            if (previousSessionId in visible) {
                visible - previousSessionId + resolvedSessionId
            } else {
                visible
            }
        }
    }

    private suspend fun saveInitialLocalState(command: ChatStreamCommand) {
        val now = System.currentTimeMillis()
        val existing = runCatching { repository.cachedSession(command.session.id) }.getOrNull()
        val session = command.session.copy(
            source = existing?.source ?: command.session.source,
            startedAt = existing?.startedAt ?: command.session.startedAt,
            endedAt = existing?.endedAt,
            model = existing?.model ?: command.session.model,
            lastSyncedAt = existing?.lastSyncedAt ?: command.session.lastSyncedAt,
            localLastActivityAt = now,
            unreadCount = 0,
            lastReadAt = now,
        )
        repository.saveLocalSession(session)
        repository.saveLocalMessage(command.userMessage)
    }

    private suspend fun pushUserMessageRemote(message: MessageEntity) {
        runCatching {
            repository.pushMessageToRemote(
                sessionId = message.sessionId,
                role = message.role,
                content = message.content,
                timestamp = message.timestamp,
            )
        }
    }

    private suspend fun saveResolvedLocalState(session: SessionEntity, userMessage: MessageEntity) {
        val now = System.currentTimeMillis()
        val existing = runCatching { repository.cachedSession(session.id) }.getOrNull()
        val resolvedSession = session.copy(
            source = existing?.source ?: session.source,
            startedAt = existing?.startedAt ?: session.startedAt,
            endedAt = existing?.endedAt,
            model = existing?.model ?: session.model,
            lastSyncedAt = existing?.lastSyncedAt ?: session.lastSyncedAt,
            localLastActivityAt = now,
            unreadCount = 0,
            lastReadAt = now,
        )
        repository.saveLocalSession(resolvedSession)
        repository.saveLocalMessage(userMessage)
    }

    private suspend fun saveAssistantProgress(
        sessionId: String,
        assistantMessageId: Long,
        content: String,
        reasoning: String,
        receivedAttachments: List<ReceivedAttachment>,
        usage: TokenUsage?,
    ) {
        if (content.isBlank() && reasoning.isBlank() && receivedAttachments.isEmpty() && usage == null) return
        val readUpdate = if (isSessionVisible(sessionId)) {
            SessionReadUpdate.MarkRead
        } else {
            SessionReadUpdate.Preserve
        }
        repository.saveLocalMessage(
            MessageEntity(
                id = assistantMessageId,
                sessionId = sessionId,
                role = "assistant",
                content = content,
                reasoning = reasoning,
                timestamp = assistantMessageId,
                receivedAttachmentsJson = receivedAttachments.toReceivedAttachmentsJson(),
            ),
        )
        updateSessionAfterAssistant(sessionId, content, receivedAttachments, readUpdate)
    }

    private suspend fun finish(
        command: ChatStreamCommand,
        sessionId: String,
        content: String,
        reasoning: String,
        receivedAttachments: List<ReceivedAttachment>,
    ) {
        val hasVisibleReply = content.isNotBlank() || receivedAttachments.isNotEmpty()
        val shouldNotify = !isSessionVisible(sessionId) && hasVisibleReply
        if (content.isNotBlank() || reasoning.isNotBlank() || receivedAttachments.isNotEmpty()) {
            saveAssistantProgress(sessionId, command.assistantMessageId, content, reasoning, receivedAttachments, usage = null)
        }
        val remoteContent = content.ifBlank { receivedAttachments.toRemoteAttachmentMessage() }
        if (remoteContent.isNotBlank()) {
            runCatching {
                repository.pushMessageToRemote(
                    sessionId = sessionId,
                    role = "assistant",
                    content = remoteContent,
                    timestamp = command.assistantMessageId,
                )
            }
        }
        updateSessionAfterAssistant(
            sessionId = sessionId,
            content = content,
            receivedAttachments = receivedAttachments,
            readUpdate = if (isSessionVisible(sessionId)) {
                SessionReadUpdate.MarkRead
            } else {
                SessionReadUpdate.IncrementUnread
            },
        )
        jobs.remove(sessionId)
        stopStreamingServiceIfIdle()
        if (shouldNotify) {
            runCatching { notifications?.notifyReply(sessionId, content.ifBlank { receivedAttachments.receivedAttachmentPreview().orEmpty() }) }
        }
        command.onFinished()
        delay(STREAM_DONE_SNAPSHOT_MS)
        clearSnapshot(sessionId)
    }

    private suspend fun fail(
        sessionId: String,
        snapshot: ChatStreamSnapshot,
        command: ChatStreamCommand,
        readable: String,
    ) {
        runCatching {
            repository.deleteLocalMessages(sessionId, listOf(command.assistantMessageId))
            updateSessionAfterFailure(sessionId, command.userMessage.content)
        }
        updateSnapshot(snapshot)
        jobs.remove(sessionId)
        stopStreamingServiceIfIdle()
        if (!isSessionVisible(sessionId)) {
            runCatching { notifications?.notifyFailure(sessionId, readable) }
        }
        command.onFailed(readable)
        delay(STREAM_DONE_SNAPSHOT_MS)
        clearSnapshot(sessionId)
    }

    private suspend fun updateSessionAfterAssistant(
        sessionId: String,
        content: String,
        receivedAttachments: List<ReceivedAttachment> = emptyList(),
        readUpdate: SessionReadUpdate,
    ) {
        val now = System.currentTimeMillis()
        val existing = runCatching { repository.cachedSession(sessionId) }.getOrNull() ?: return
        val messageCount = runCatching {
            repository.cachedMessages(sessionId).count {
                it.content.isNotBlank() || it.imageUrisJson != "[]" || it.receivedAttachmentsJson != "[]"
            }
        }.getOrDefault(existing.messageCount)
        val unreadCount = when (readUpdate) {
            SessionReadUpdate.MarkRead -> 0
            SessionReadUpdate.IncrementUnread -> existing.unreadCount + 1
            SessionReadUpdate.Preserve -> existing.unreadCount
        }
        val lastReadAt = when (readUpdate) {
            SessionReadUpdate.MarkRead -> now
            SessionReadUpdate.IncrementUnread,
            SessionReadUpdate.Preserve -> existing.lastReadAt
        }
        repository.saveLocalSession(
            existing.copy(
                messageCount = messageCount,
                localLastActivityAt = now,
                lastMessagePreview = visibleReceivedAttachmentText(content).firstReadableLine()?.take(MAX_PREVIEW_LENGTH)
                    ?: receivedAttachments.receivedAttachmentPreview()
                    ?: existing.lastMessagePreview,
                unreadCount = unreadCount,
                lastReadAt = lastReadAt,
            ),
        )
    }

    private suspend fun updateSessionAfterFailure(sessionId: String, fallbackPreview: String) {
        val existing = runCatching { repository.cachedSession(sessionId) }.getOrNull() ?: return
        val messages = runCatching { repository.cachedMessages(sessionId) }.getOrDefault(emptyList())
        val messageCount = messages.count { it.content.isNotBlank() || it.imageUrisJson != "[]" }
        val preview = messages.lastOrNull { it.content.isNotBlank() }
            ?.content
            ?.firstReadableLine()
            ?: fallbackPreview.firstReadableLine()
            ?: existing.lastMessagePreview
        repository.saveLocalSession(
            existing.copy(
                messageCount = messageCount,
                localLastActivityAt = System.currentTimeMillis(),
                lastMessagePreview = preview?.take(MAX_PREVIEW_LENGTH),
            ),
        )
    }

    private fun updateSnapshot(snapshot: ChatStreamSnapshot) {
        _streams.update { it + (snapshot.sessionId to snapshot) }
    }

    private fun clearSnapshot(sessionId: String) {
        _streams.update { it - sessionId }
    }

    private fun isSessionVisible(sessionId: String): Boolean {
        return appInForeground.value && sessionId in visibleSessionIds.value
    }

    private fun stopStreamingServiceIfIdle() {
        if (jobs.values.any { it.isActive }) return
        runCatching { notifications?.stopStreamingService() }
    }
}

private enum class SessionReadUpdate {
    MarkRead,
    IncrementUnread,
    Preserve,
}

private suspend fun <T> Flow<T>.collectStreamEvents(
    onEvent: suspend (T) -> Unit,
) {
    collect { event ->
        onEvent(event)
    }
}

private fun Throwable.streamTimeoutCause(): SocketTimeoutException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is SocketTimeoutException) return current
        current = current.cause
    }
    return null
}

private fun Throwable?.isStreamTimeout(): Boolean = this?.streamTimeoutCause() != null

private fun List<ToolProgress>.withToolProgress(progress: ToolProgress): List<ToolProgress> {
    val existingIndex = indexOfLast { it.label == progress.label && it.tool == progress.tool }
    val updated = if (existingIndex >= 0) {
        toMutableList().also { it[existingIndex] = progress }
    } else {
        this + progress
    }
    return updated.takeLast(MAX_VISIBLE_TOOLS)
}

private fun combineReasoning(explicit: String, extracted: String): String {
    return listOf(explicit, extracted)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n\n")
}

private fun String.firstReadableLine(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}

private fun List<ReceivedAttachment>.receivedAttachmentPreview(): String? {
    return when (size) {
        0 -> null
        1 -> first().label
        else -> "$size attachments"
    }
}

private fun List<ReceivedAttachment>.toRemoteAttachmentMessage(): String {
    if (isEmpty()) return ""
    return buildString {
        appendLine("Generated files:")
        this@toRemoteAttachmentMessage.forEach { attachment ->
            append("[")
            append(attachment.label.ifBlank { attachment.kind.name })
            append("](")
            append(attachment.url)
            appendLine(")")
        }
    }.trim()
}

private fun List<ReceivedAttachment>.toReceivedAttachmentsJson(): String {
    return Json.encodeToString(this)
}

private const val MAX_VISIBLE_TOOLS = 8
private const val MAX_PREVIEW_LENGTH = 200
private const val STREAM_DONE_SNAPSHOT_MS = 1_500L
private const val MAX_STREAM_RETRIES = 3
private val RETRY_BACKOFF_MS = longArrayOf(2_000L, 4_000L, 8_000L)
