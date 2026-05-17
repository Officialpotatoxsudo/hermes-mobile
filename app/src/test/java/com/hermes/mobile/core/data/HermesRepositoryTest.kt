package com.hermes.mobile.core.data

import android.content.Context
import com.hermes.mobile.core.auth.SavedConnection
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.auth.connectionIdentityFor
import com.hermes.mobile.core.data.local.LEGACY_ACCOUNT_SCOPE
import com.hermes.mobile.core.data.local.MessageDao
import com.hermes.mobile.core.data.local.SessionDao
import com.hermes.mobile.core.model.MessagesResponse
import com.hermes.mobile.core.model.SessionDto
import com.hermes.mobile.core.model.SessionMessageDto
import com.hermes.mobile.core.model.SessionsResponse
import com.hermes.mobile.core.network.HermesRestClient
import com.hermes.mobile.core.network.SseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HermesRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun sessionsSearchUsesFirstUsefulQueryLine() = runTest {
        val sessionDao = mockk<SessionDao>()
        val tokenStore = scopedTokenStore()
        val scope = connectionIdentityFor("https://agent.example", "api-key")
        every { sessionDao.searchFlow(scope, "planning") } returns flowOf(emptyList())
        val repository = HermesRepository(
            restClient = mockk<HermesRestClient>(),
            sseClient = mockk<SseClient>(),
            sessionDao = sessionDao,
            messageDao = mockk<MessageDao>(),
            tokenStore = tokenStore,
            appContext = testContext(),
        )

        repository.sessions("\n planning \n ignored").first()

        verify { sessionDao.searchFlow(scope, "planning") }
    }

    @Test
    fun deleteLocalMessagesScopesDeleteToSession() = runTest {
        val messageDao = mockk<MessageDao>(relaxed = true)
        val tokenStore = scopedTokenStore("scope-a")
        val repository = HermesRepository(
            restClient = mockk<HermesRestClient>(),
            sseClient = mockk<SseClient>(),
            sessionDao = mockk<SessionDao>(),
            messageDao = messageDao,
            tokenStore = tokenStore,
            appContext = testContext(),
        )

        repository.deleteLocalMessages("session-1", listOf(1L, 2L))

        coVerify { messageDao.deleteByIds("scope-a", "session-1", listOf(1L, 2L)) }
    }

    @Test
    fun deleteLocalMessagesIgnoresBlankSession() = runTest {
        val messageDao = mockk<MessageDao>(relaxed = true)
        val tokenStore = scopedTokenStore("scope-a")
        val repository = HermesRepository(
            restClient = mockk<HermesRestClient>(),
            sseClient = mockk<SseClient>(),
            sessionDao = mockk<SessionDao>(),
            messageDao = messageDao,
            tokenStore = tokenStore,
            appContext = testContext(),
        )

        repository.deleteLocalMessages("", listOf(1L))

        coVerify(exactly = 0) { messageDao.deleteByIds(any(), any(), any()) }
    }

    @Test
    fun syncSessionsFetchesUntilShortPage() = runTest {
        val restClient = mockk<HermesRestClient>()
        val sessionDao = mockk<SessionDao>(relaxed = true)
        val repository = HermesRepository(
            restClient = restClient,
            sseClient = mockk<SseClient>(),
            sessionDao = sessionDao,
            messageDao = mockk<MessageDao>(),
            tokenStore = scopedTokenStore("scope-a"),
            appContext = testContext(),
        )
        val firstPage = (0 until 50).map { index -> sessionDto("session-$index") }
        val secondPage = listOf(sessionDto("session-50"))
        coEvery { restClient.fetchSessions(50, 0) } returns Result.success(SessionsResponse(firstPage))
        coEvery { restClient.fetchSessions(50, 50) } returns Result.success(SessionsResponse(secondPage))

        repository.syncSessions().getOrThrow()

        coVerify { restClient.fetchSessions(50, 0) }
        coVerify { restClient.fetchSessions(50, 50) }
        coVerify {
            sessionDao.upsertAll(match { sessions ->
                sessions.size == 50 && sessions.all { it.accountScope == "scope-a" }
            })
        }
        coVerify {
            sessionDao.upsertAll(match { sessions ->
                sessions.size == 1 && sessions.single().id == "session-50" && sessions.single().accountScope == "scope-a"
            })
        }
    }

    @Test
    fun sessionsUsesActiveAccountScope() {
        val sessionDao = mockk<SessionDao>()
        val tokenStore = scopedTokenStore()
        val scope = connectionIdentityFor("https://agent.example", "api-key")
        every { sessionDao.getAllFlow(scope) } returns flowOf(emptyList())
        val repository = HermesRepository(
            restClient = mockk<HermesRestClient>(),
            sseClient = mockk<SseClient>(),
            sessionDao = sessionDao,
            messageDao = mockk<MessageDao>(),
            tokenStore = tokenStore,
            appContext = testContext(),
        )

        runTest {
            repository.sessions("").first()
        }

        verify { sessionDao.getAllFlow(scope) }
    }

    @Test
    fun messagesUseSavedConnectionScopeWhenSynchronousCacheIsCold() = runTest {
        val messageDao = mockk<MessageDao>()
        val activeScope = connectionIdentityFor("https://agent.example", "api-key")
        val tokenStore = scopedTokenStore("scope-a").also {
            every { it.serverUrl } returns ""
            every { it.apiKey } returns ""
            every { it.savedConnection } returns flowOf(
                SavedConnection(
                    serverUrl = "https://agent.example",
                    apiKey = "api-key",
                    identity = activeScope,
                ),
            )
        }
        every { messageDao.getBySessionIdFlow(LEGACY_ACCOUNT_SCOPE, "session-1") } returns flowOf(emptyList())
        every { messageDao.getBySessionIdFlow(activeScope, "session-1") } returns flowOf(emptyList())
        val repository = HermesRepository(
            restClient = mockk<HermesRestClient>(),
            sseClient = mockk<SseClient>(),
            sessionDao = mockk<SessionDao>(),
            messageDao = messageDao,
            tokenStore = tokenStore,
            appContext = testContext(),
        )

        repository.messages("session-1").first()

        verify { messageDao.getBySessionIdFlow(activeScope, "session-1") }
    }

    @Test
    fun syncMessagesDeletesStaleScopedRemoteMessages() = runTest {
        val restClient = mockk<HermesRestClient>()
        val messageDao = mockk<MessageDao>(relaxed = true)
        val repository = HermesRepository(
            restClient = restClient,
            sseClient = mockk<SseClient>(),
            sessionDao = mockk<SessionDao>(),
            messageDao = messageDao,
            tokenStore = scopedTokenStore("scope-a"),
            appContext = testContext(),
        )
        coEvery { restClient.fetchMessages("session-1") } returns Result.success(
            MessagesResponse(
                messages = listOf(
                    SessionMessageDto(id = 10L, role = "user", content = "hi", timestamp = 1L),
                    SessionMessageDto(id = 11L, role = "assistant", content = "hello", timestamp = 2L),
                ),
            ),
        )

        repository.syncMessages("session-1").getOrThrow()

        coVerify {
            messageDao.upsertAll(match { messages ->
                messages.map { it.id } == listOf(10L, 11L) &&
                    messages.all { it.accountScope == "scope-a" && it.remoteBacked }
            })
        }
        coVerify { messageDao.deleteStaleRemoteMessages("scope-a", "session-1", listOf(10L, 11L)) }
    }

    private fun scopedTokenStore(scope: String = ""): TokenStore {
        val tokenStore = mockk<TokenStore>()
        every { tokenStore.serverUrl } returns "https://agent.example"
        every { tokenStore.apiKey } returns "api-key"
        every { tokenStore.savedConnection } returns flowOf(
            SavedConnection(
                serverUrl = "https://agent.example",
                apiKey = "api-key",
                identity = scope.ifBlank { connectionIdentityFor("https://agent.example", "api-key") },
            ),
        )
        coEvery { tokenStore.connectionIdentity() } returns scope.ifBlank {
            connectionIdentityFor("https://agent.example", "api-key")
        }
        return tokenStore
    }

    private fun testContext(): Context {
        val context = mockk<Context>()
        every { context.filesDir } returns tempDir.toFile()
        return context
    }

    private fun sessionDto(id: String): SessionDto {
        return SessionDto(
            id = id,
            title = id,
            startedAt = 1L,
        )
    }
}
