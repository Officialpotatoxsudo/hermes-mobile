package com.hermes.mobile.core.data

import android.content.Context
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.data.local.LEGACY_ACCOUNT_SCOPE
import com.hermes.mobile.core.data.local.MessageDao
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionDao
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.SessionDto
import com.hermes.mobile.core.model.SessionMessageDto
import com.hermes.mobile.core.network.HermesRestClient
import com.hermes.mobile.core.network.SseClient
import com.hermes.mobile.core.network.SseEvent
import com.hermes.mobile.core.util.deleteAppOwnedMessageMedia
import com.hermes.mobile.core.util.legacyImageUrisFromText
import com.hermes.mobile.core.util.messageImageUrisFromJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class HermesRepository @Inject constructor(
    private val restClient: HermesRestClient,
    private val sseClient: SseClient,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val tokenStore: TokenStore,
    @param:ApplicationContext private val appContext: Context,
) {
    private val migratedLocalScopes = ConcurrentHashMap.newKeySet<String>()

    fun sessions(query: String): Flow<List<SessionEntity>> {
        val cleanQuery = query.cleanRepositoryQuery()
        return accountScopeFlow().flatMapLatest { scope ->
            val flow = if (cleanQuery == null) sessionDao.getAllFlow(scope) else sessionDao.searchFlow(scope, cleanQuery)
            flow.onStart { migrateLocalHistoryToActiveScopeIfNeeded(scope) }
        }
    }

    fun messages(sessionId: String): Flow<List<MessageEntity>> {
        return accountScopeFlow().flatMapLatest { scope ->
            messageDao.getBySessionIdFlow(scope, sessionId)
                .onStart { migrateLocalHistoryToActiveScopeIfNeeded(scope) }
        }
    }

    suspend fun latestSession(): SessionEntity? {
        val scope = activeScope()
        migrateLocalHistoryToActiveScopeIfNeeded(scope)
        return sessionDao.latest(scope)
    }

    suspend fun cachedSession(sessionId: String): SessionEntity? {
        val scope = activeScope()
        migrateLocalHistoryToActiveScopeIfNeeded(scope)
        return sessionDao.getById(scope, sessionId)
    }

    suspend fun cachedMessages(sessionId: String): List<MessageEntity> {
        val scope = activeScope()
        migrateLocalHistoryToActiveScopeIfNeeded(scope)
        return messageDao.getBySessionId(scope, sessionId)
    }

    suspend fun saveLocalSession(session: SessionEntity) {
        sessionDao.upsert(session.copy(accountScope = activeScope()))
    }

    suspend fun saveLocalMessage(message: MessageEntity) {
        messageDao.upsert(message.copy(accountScope = activeScope(), remoteBacked = false))
    }

    suspend fun saveLocalMessages(messages: List<MessageEntity>) {
        val scope = activeScope()
        messageDao.upsertAll(messages.map { it.copy(accountScope = scope, remoteBacked = false) })
    }

    suspend fun markSessionRead(sessionId: String, readAt: Long = System.currentTimeMillis()) {
        if (sessionId.isNotBlank()) {
            sessionDao.markRead(activeScope(), sessionId, readAt)
        }
    }

    suspend fun deleteLocalMessages(sessionId: String, messageIds: List<Long>) {
        if (sessionId.isNotBlank() && messageIds.isNotEmpty()) {
            messageDao.deleteByIds(activeScope(), sessionId, messageIds)
        }
    }

    suspend fun deleteLocalSession(sessionId: String) {
        sessionDao.deleteById(activeScope(), sessionId)
    }

    suspend fun checkHealth(serverUrl: String, apiKey: String): Result<Unit> {
        return restClient.checkHealth(serverUrl, apiKey)
    }

    suspend fun syncSessions(): Result<Unit> {
        val scope = activeScope()
        migrateLocalHistoryToActiveScopeIfNeeded(scope)
        return runCatching {
            var offset = 0
            while (true) {
                val response = restClient.fetchSessions(syncSessionsPageSize, offset).getOrThrow()
                sessionDao.upsertAll(response.sessions.map { it.toEntity(scope) })
                if (response.sessions.size < syncSessionsPageSize) break
                offset += syncSessionsPageSize
            }
        }
    }

    suspend fun syncMessages(sessionId: String): Result<Unit> {
        val scope = activeScope()
        migrateLocalHistoryToActiveScopeIfNeeded(scope)
        return restClient.fetchMessages(sessionId).mapCatching { response ->
            val beforeSync = messageDao.getBySessionId(scope, sessionId)
            val messages = response.messages.map { it.toEntity(scope, sessionId) }
            messageDao.upsertAll(messages)
            messageDao.deleteStaleRemoteMessages(scope, sessionId, messages.map { it.id })
            val afterSyncIds = messageDao.getBySessionId(scope, sessionId).map { it.id }.toSet()
            val missingLocalHistory = beforeSync
                .filter { it.id !in afterSyncIds }
                .map { it.copy(remoteBacked = false) }
            if (missingLocalHistory.isNotEmpty()) {
                messageDao.upsertAll(missingLocalHistory)
            }
        }
    }

    suspend fun pushMessageToRemote(sessionId: String, role: String, content: String, timestamp: Long): Result<Unit> {
        return restClient.pushMessage(sessionId, role, content, timestamp)
    }

    suspend fun clearLocalDataForActiveConnection() {
        val scope = activeScope()
        val scopedMediaUris = messageDao.getByScope(scope).flatMap { message ->
            messageImageUrisFromJson(message.imageUrisJson) + legacyImageUrisFromText(message.content)
        }
        deleteAppOwnedMessageMedia(appContext, scopedMediaUris)
        messageDao.deleteByScope(scope)
        sessionDao.deleteByScope(scope)
    }

    suspend fun fetchModelOptions(): Result<DashboardModelOptionsResponse> = restClient.fetchModelOptions()

    fun streamChat(request: ChatCompletionRequest, sessionId: String? = null): Flow<SseEvent> {
        return sseClient.streamChat(request, sessionId)
    }

    suspend fun completeChat(request: ChatCompletionRequest, sessionId: String? = null): List<SseEvent> {
        return sseClient.completeChat(request, sessionId)
    }

    suspend fun getText(path: String): Result<String> = restClient.getText(path)

    suspend fun putText(path: String, body: String): Result<String> = restClient.putText(path, body)

    suspend fun postText(path: String, body: String): Result<String> = restClient.postText(path, body)

    suspend fun deleteText(path: String): Result<String> = restClient.deleteText(path)

    private fun accountScopeFlow(): Flow<String> {
        return tokenStore.savedConnection
            .map { it.identity.ifBlank { LEGACY_ACCOUNT_SCOPE } }
            .distinctUntilChanged()
    }

    private suspend fun activeScope(): String {
        return tokenStore.connectionIdentity().ifBlank { LEGACY_ACCOUNT_SCOPE }
    }

    private suspend fun migrateLocalHistoryToActiveScopeIfNeeded(scope: String) {
        if (scope.isBlank() || scope == LEGACY_ACCOUNT_SCOPE) return
        if (!migratedLocalScopes.add(scope)) return
        runCatching {
            val activeSessions = sessionDao.getByScope(scope).associateBy { it.id }
            val sourceSessions = sessionDao.getOutsideScope(scope)
            if (sourceSessions.isEmpty()) return@runCatching

            val sourceById = sourceSessions
                .groupBy { it.id }
                .mapValues { (_, sessions) ->
                    sessions.maxWith(
                        compareBy<SessionEntity> { it.localLastActivityAt }
                            .thenBy { it.startedAt },
                    )
                }
            val sessionsToCopy = sourceById.values.mapNotNull { source ->
                val active = activeSessions[source.id]
                if (active == null || source.localLastActivityAt > active.localLastActivityAt) {
                    source.copy(accountScope = scope)
                } else {
                    null
                }
            }
            if (sessionsToCopy.isNotEmpty()) {
                sessionDao.upsertAll(sessionsToCopy)
            }

            val knownSessionIds = activeSessions.keys + sourceById.keys
            val activeMessageKeys = messageDao.getByScope(scope)
                .mapTo(mutableSetOf()) { it.sessionId to it.id }
            val messagesToCopy = messageDao.getOutsideScope(scope)
                .filter { it.sessionId in knownSessionIds }
                .filter { (it.sessionId to it.id) !in activeMessageKeys }
                .map { it.copy(accountScope = scope) }
            if (messagesToCopy.isNotEmpty()) {
                messageDao.upsertAll(messagesToCopy)
            }
        }
    }
}

private const val syncSessionsPageSize = 50

private fun String.cleanRepositoryQuery(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}

private fun SessionDto.toEntity(accountScope: String): SessionEntity {
    return SessionEntity(
        id = id,
        title = title,
        source = source,
        startedAt = startedAt,
        endedAt = endedAt,
        messageCount = messageCount,
        model = model,
        accountScope = accountScope,
        lastMessagePreview = title,
    )
}

private fun SessionMessageDto.toEntity(accountScope: String, sessionId: String): MessageEntity {
    return MessageEntity(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        reasoning = reasoningContent ?: reasoning.orEmpty(),
        timestamp = timestamp,
        accountScope = accountScope,
        remoteBacked = true,
    )
}
