package com.hermes.mobile.core.data

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
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HermesRepository @Inject constructor(
    private val restClient: HermesRestClient,
    private val sseClient: SseClient,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {
    fun sessions(query: String): Flow<List<SessionEntity>> {
        return if (query.isBlank()) sessionDao.getAllFlow() else sessionDao.searchFlow(query.trim())
    }

    fun messages(sessionId: String): Flow<List<MessageEntity>> = messageDao.getBySessionIdFlow(sessionId)

    suspend fun checkHealth(serverUrl: String, apiKey: String): Result<Unit> {
        return restClient.checkHealth(serverUrl, apiKey)
    }

    suspend fun syncSessions(): Result<Unit> {
        return restClient.fetchSessions().mapCatching { response ->
            sessionDao.upsertAll(response.sessions.map(SessionDto::toEntity))
        }
    }

    suspend fun syncMessages(sessionId: String): Result<Unit> {
        return restClient.fetchMessages(sessionId).mapCatching { response ->
            messageDao.upsertAll(response.messages.map { it.toEntity(sessionId) })
        }
    }

    suspend fun fetchModelOptions(): Result<DashboardModelOptionsResponse> = restClient.fetchModelOptions()

    fun streamChat(request: ChatCompletionRequest): Flow<SseEvent> = sseClient.streamChat(request)

    suspend fun getText(path: String): Result<String> = restClient.getText(path)

    suspend fun putText(path: String, body: String): Result<String> = restClient.putText(path, body)

    suspend fun postText(path: String, body: String): Result<String> = restClient.postText(path, body)
}

private fun SessionDto.toEntity(): SessionEntity {
    return SessionEntity(
        id = id,
        title = title,
        source = source,
        startedAt = startedAt,
        endedAt = endedAt,
        messageCount = messageCount,
        model = model,
    )
}

private fun SessionMessageDto.toEntity(sessionId: String): MessageEntity {
    return MessageEntity(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp,
    )
}
