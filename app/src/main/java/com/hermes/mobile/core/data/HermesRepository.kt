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
import com.hermes.mobile.core.util.deleteHermesMediaDirectory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    fun sessions(query: String): Flow<List<SessionEntity>> {
        val cleanQuery = query.cleanRepositoryQuery()
        return accountScopeFlow().flatMapLatest { scope ->
            if (cleanQuery == null) sessionDao.getAllFlow(scope) else sessionDao.searchFlow(scope, cleanQuery)
        }
    }

    fun messages(sessionId: String): Flow<List<MessageEntity>> {
        return accountScopeFlow().flatMapLatest { scope ->
            messageDao.getBySessionIdFlow(scope, sessionId)
        }
    }

    suspend fun latestSession(): SessionEntity? = sessionDao.latest(activeScope())

    suspend fun cachedSession(sessionId: String): SessionEntity? = sessionDao.getById(activeScope(), sessionId)

    suspend fun cachedMessages(sessionId: String): List<MessageEntity> = messageDao.getBySessionId(activeScope(), sessionId)

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
        return restClient.fetchMessages(sessionId).mapCatching { response ->
            val messages = response.messages.map { it.toEntity(scope, sessionId) }
            messageDao.upsertAll(messages)
            messageDao.deleteStaleRemoteMessages(scope, sessionId, messages.map { it.id })
        }
    }

    suspend fun pushMessageToRemote(sessionId: String, role: String, content: String, timestamp: Long): Result<Unit> {
        return restClient.pushMessage(sessionId, role, content, timestamp)
    }

    suspend fun clearLocalDataForActiveConnection() {
        val scope = activeScope()
        deleteHermesMediaDirectory(appContext)
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
        timestamp = timestamp,
        accountScope = accountScope,
        remoteBacked = true,
    )
}
