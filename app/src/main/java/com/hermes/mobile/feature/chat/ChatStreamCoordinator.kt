package com.hermes.mobile.feature.chat

import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.TokenUsage
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.network.SseEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ChatStreamSnapshot(
    val sessionId: String,
    val assistantMessageId: Long,
    val content: String = "",
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
    val onFinished: () -> Unit = {},
    val onFailed: (String) -> Unit = {},
)

@Singleton
class ChatStreamCoordinator @Inject constructor(
    private val repository: HermesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val visibleSessionIds = MutableStateFlow<Set<String>>(emptySet())
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

    fun start(command: ChatStreamCommand): Job {
        jobs[command.session.id]?.takeIf { it.isActive }?.let { return it }
        val job = scope.launch {
            runStream(command)
        }
        jobs[command.session.id] = job
        job.invokeOnCompletion { jobs.remove(command.session.id, job) }
        return job
    }

    fun stop(sessionId: String) {
        jobs.remove(sessionId)?.cancel()
        clearSnapshot(sessionId)
    }

    private suspend fun runStream(command: ChatStreamCommand) {
        val sessionId = command.session.id
        var snapshot = ChatStreamSnapshot(
            sessionId = sessionId,
            assistantMessageId = command.assistantMessageId,
            isConnecting = true,
        )
        var completed = false
        var content = ""
        var usage: TokenUsage? = null
        var lastError: Throwable? = null
        updateSnapshot(snapshot)
        try {
            saveInitialLocalState(command)
            val request = try {
                command.requestBuilder()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val readable = ErrorMapper.userMessage(error, "Could not read selected photo")
                fail(sessionId, snapshot.copy(isConnecting = false, isStreaming = false, error = readable), command, readable)
                return
            }

            for (attempt in 0..MAX_STREAM_RETRIES) {
                if (attempt > 0) {
                    snapshot = snapshot.copy(
                        isConnecting = true,
                        isStreaming = false,
                        error = "Reconnecting... (attempt $attempt/$MAX_STREAM_RETRIES)",
                    )
                    updateSnapshot(snapshot)
                    delay(RETRY_BACKOFF_MS[attempt - 1])
                }
                repository.streamChat(request, sessionId)
                    .catch { error -> lastError = error }
                    .collect { event ->
                        when (event) {
                            is SseEvent.Opened -> {
                                snapshot = snapshot.copy(isConnecting = false, isStreaming = true, error = null)
                                updateSnapshot(snapshot)
                            }
                            is SseEvent.Delta -> {
                                content += event.text
                                snapshot = snapshot.copy(
                                    content = content,
                                    isConnecting = false,
                                    isStreaming = true,
                                    error = null,
                                )
                                updateSnapshot(snapshot)
                                saveAssistantProgress(command, content, usage)
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
                                saveAssistantProgress(command, content, usage)
                            }
                            SseEvent.Done -> {
                                completed = true
                                snapshot = snapshot.copy(
                                    content = content,
                                    usage = usage,
                                    isConnecting = false,
                                    isStreaming = false,
                                    error = null,
                                )
                                updateSnapshot(snapshot)
                                finish(command, content)
                            }
                        }
                    }
                if (completed) return
                if (attempt == MAX_STREAM_RETRIES) break
            }
            val readable = ErrorMapper.userMessage(lastError, "Connection lost")
            fail(sessionId, snapshot.copy(isConnecting = false, isStreaming = false, error = readable), command, readable)
        } catch (cancelled: CancellationException) {
            clearSnapshot(sessionId)
            throw cancelled
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
        runCatching {
            repository.pushMessageToRemote(
                sessionId = command.userMessage.sessionId,
                role = command.userMessage.role,
                content = command.userMessage.content,
                timestamp = command.userMessage.timestamp,
            )
        }
    }

    private suspend fun saveAssistantProgress(command: ChatStreamCommand, content: String, usage: TokenUsage?) {
        if (content.isBlank() && usage == null) return
        val readUpdate = if (command.session.id in visibleSessionIds.value) {
            SessionReadUpdate.MarkRead
        } else {
            SessionReadUpdate.Preserve
        }
        repository.saveLocalMessage(
            MessageEntity(
                id = command.assistantMessageId,
                sessionId = command.session.id,
                role = "assistant",
                content = content,
                timestamp = command.assistantMessageId,
            ),
        )
        updateSessionAfterAssistant(command.session.id, content, readUpdate)
    }

    private suspend fun finish(command: ChatStreamCommand, content: String) {
        if (content.isNotBlank()) {
            saveAssistantProgress(command, content, usage = null)
            runCatching {
                repository.pushMessageToRemote(
                    sessionId = command.session.id,
                    role = "assistant",
                    content = content,
                    timestamp = command.assistantMessageId,
                )
            }
        }
        updateSessionAfterAssistant(
            sessionId = command.session.id,
            content = content,
            readUpdate = if (command.session.id in visibleSessionIds.value) {
                SessionReadUpdate.MarkRead
            } else {
                SessionReadUpdate.IncrementUnread
            },
        )
        jobs.remove(command.session.id)
        command.onFinished()
        delay(STREAM_DONE_SNAPSHOT_MS)
        clearSnapshot(command.session.id)
    }

    private suspend fun fail(
        sessionId: String,
        snapshot: ChatStreamSnapshot,
        command: ChatStreamCommand,
        readable: String,
    ) {
        updateSnapshot(snapshot)
        jobs.remove(sessionId)
        command.onFailed(readable)
        delay(STREAM_DONE_SNAPSHOT_MS)
        clearSnapshot(sessionId)
    }

    private suspend fun updateSessionAfterAssistant(
        sessionId: String,
        content: String,
        readUpdate: SessionReadUpdate,
    ) {
        val now = System.currentTimeMillis()
        val existing = runCatching { repository.cachedSession(sessionId) }.getOrNull() ?: return
        val messageCount = runCatching {
            repository.cachedMessages(sessionId).count { it.content.isNotBlank() || it.imageUrisJson != "[]" }
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
                lastMessagePreview = content.firstReadableLine()?.take(MAX_PREVIEW_LENGTH)
                    ?: existing.lastMessagePreview,
                unreadCount = unreadCount,
                lastReadAt = lastReadAt,
            ),
        )
    }

    private fun updateSnapshot(snapshot: ChatStreamSnapshot) {
        _streams.update { it + (snapshot.sessionId to snapshot) }
    }

    private fun clearSnapshot(sessionId: String) {
        _streams.update { it - sessionId }
    }
}

private enum class SessionReadUpdate {
    MarkRead,
    IncrementUnread,
    Preserve,
}

private fun List<ToolProgress>.withToolProgress(progress: ToolProgress): List<ToolProgress> {
    val existingIndex = indexOfLast { it.label == progress.label && it.tool == progress.tool }
    val updated = if (existingIndex >= 0) {
        toMutableList().also { it[existingIndex] = progress }
    } else {
        this + progress
    }
    return updated.takeLast(MAX_VISIBLE_TOOLS)
}

private fun String.firstReadableLine(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}

private const val MAX_VISIBLE_TOOLS = 8
private const val MAX_PREVIEW_LENGTH = 200
private const val STREAM_DONE_SNAPSHOT_MS = 1_500L
private const val MAX_STREAM_RETRIES = 3
private val RETRY_BACKOFF_MS = longArrayOf(2_000L, 4_000L, 8_000L)
