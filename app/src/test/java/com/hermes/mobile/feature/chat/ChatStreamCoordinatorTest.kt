package com.hermes.mobile.feature.chat

import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.chatRequestMessage
import com.hermes.mobile.core.network.SseEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStreamCoordinatorTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun hiddenSessionPersistsAssistantReplyAndMarksUnread() = runTest(dispatcher) {
        val repository = mockRepository()
        val coordinator = ChatStreamCoordinator(repository.repository)

        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
            ),
        )
        advanceUntilIdle()

        assertEquals("Hi", repository.savedMessages.last { it.role == "assistant" }.content)
        assertEquals(1, repository.savedSessions.last().unreadCount)
        assertEquals("Hi", repository.savedSessions.last().lastMessagePreview)
    }

    @Test
    fun visibleSessionClearsUnreadAfterAssistantReply() = runTest(dispatcher) {
        val repository = mockRepository()
        val coordinator = ChatStreamCoordinator(repository.repository)

        coordinator.markSessionVisible("session-1")
        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
            ),
        )
        advanceUntilIdle()

        assertEquals(0, repository.savedSessions.last().unreadCount)
        coVerify { repository.repository.markSessionRead("session-1", any()) }
    }

    @Test
    fun backgroundedVisibleSessionMarksFinishedReplyUnread() = runTest(dispatcher) {
        val repository = mockRepository()
        val coordinator = ChatStreamCoordinator(repository.repository)

        coordinator.markSessionVisible("session-1")
        coordinator.markAppBackgrounded()
        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
            ),
        )
        advanceUntilIdle()

        assertEquals(1, repository.savedSessions.last().unreadCount)
    }

    @Test
    fun hiddenProgressPreservesReadStateUntilReplyCompletes() = runTest(dispatcher) {
        val events = Channel<SseEvent>(Channel.UNLIMITED)
        val repository = mockRepository(events = events.receiveAsFlow())
        val coordinator = ChatStreamCoordinator(repository.repository)

        coordinator.markSessionVisible("session-1")
        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
            ),
        )
        events.send(SseEvent.Opened(null))
        runCurrent()
        val initialReadAt = repository.savedSessions.last().lastReadAt

        coordinator.markSessionHidden("session-1")
        events.send(SseEvent.Delta("Working"))
        runCurrent()

        assertEquals(0, repository.savedSessions.last().unreadCount)
        assertEquals(initialReadAt, repository.savedSessions.last().lastReadAt)

        events.send(SseEvent.Done)
        events.close()
        advanceUntilIdle()

        assertEquals(1, repository.savedSessions.last().unreadCount)
        assertEquals(initialReadAt, repository.savedSessions.last().lastReadAt)
    }

    @Test
    fun failedPartialStreamRemovesPersistedAssistantText() = runTest(dispatcher) {
        var streamCalls = 0
        val repository = mockRepository(
            eventsFactory = {
                streamCalls += 1
                if (streamCalls == 1) {
                    kotlinx.coroutines.flow.flow {
                        emit(SseEvent.Opened(null))
                        emit(SseEvent.Delta("Hel"))
                        throw java.io.IOException("network dropped")
                    }
                } else {
                    flowOf(SseEvent.Opened(null), SseEvent.Delta("Hello"), SseEvent.Done)
                }
            },
        )
        val coordinator = ChatStreamCoordinator(repository.repository)
        var failure: String? = null

        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
                onFailed = { failure = it },
            ),
        )
        advanceUntilIdle()

        assertEquals(1, streamCalls)
        assertEquals(false, repository.savedMessages.any { it.role == "assistant" })
        assertEquals("Hello", repository.savedSessions.last().lastMessagePreview)
        assertEquals("network dropped", failure)
    }

    @Test
    fun streamCloseAfterContentFinishesWithoutDone() = runTest(dispatcher) {
        val repository = mockRepository(events = flowOf(SseEvent.Opened(null), SseEvent.Delta("Hi")))
        val coordinator = ChatStreamCoordinator(repository.repository)
        var finished = false

        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
                onFinished = { finished = true },
            ),
        )
        advanceUntilIdle()

        assertEquals(true, finished)
        assertEquals("Hi", repository.savedMessages.last { it.role == "assistant" }.content)
        assertEquals("Hi", repository.savedSessions.last().lastMessagePreview)
    }

    @Test
    fun firstEventTimeoutFallsBackToNonStreamingCompletion() = runTest(dispatcher) {
        var streamCalls = 0
        val repository = mockRepository(
            eventsFactory = {
                streamCalls += 1
                kotlinx.coroutines.flow.flow { throw SocketTimeoutException("Stream first event timeout") }
            },
        )
        coEvery { repository.repository.completeChat(any(), "session-1") } returns listOf(
            SseEvent.Opened(null),
            SseEvent.Delta("Fast reply"),
            SseEvent.Done,
        )
        val coordinator = ChatStreamCoordinator(repository.repository)
        var finished = false

        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
                onFinished = { finished = true },
            ),
        )
        advanceUntilIdle()

        assertEquals(1, streamCalls)
        assertEquals(true, finished)
        assertEquals("Fast reply", repository.savedMessages.last { it.role == "assistant" }.content)
        assertEquals("Fast reply", repository.savedSessions.last().lastMessagePreview)
    }

    @Test
    fun openedRemoteSessionRetargetsAssistantPersistenceAndSnapshot() = runTest(dispatcher) {
        val events = Channel<SseEvent>(Channel.UNLIMITED)
        val repository = mockRepository(events = events.receiveAsFlow())
        val coordinator = ChatStreamCoordinator(repository.repository)
        var resolvedSessionId: String? = null

        coordinator.start(
            streamCommand(
                request = ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "Hello"))),
                onSessionResolved = { resolvedSessionId = it },
            ),
        )

        events.send(SseEvent.Opened("remote-session-1"))
        events.send(SseEvent.Delta("Hello from remote"))
        runCurrent()

        assertEquals("remote-session-1", resolvedSessionId)
        assertEquals("Hello from remote", coordinator.streams.value["remote-session-1"]?.content)
        assertEquals("remote-session-1", repository.savedMessages.last { it.role == "assistant" }.sessionId)

        events.send(SseEvent.Done)
        events.close()
        advanceUntilIdle()

        assertEquals("remote-session-1", repository.savedSessions.last().id)
        coVerify { repository.repository.pushMessageToRemote("remote-session-1", "assistant", "Hello from remote", 2L) }
    }

    private fun mockRepository(
        events: Flow<SseEvent>? = null,
        eventsFactory: (() -> Flow<SseEvent>)? = null,
    ): RepositoryFixture {
        val repository = mockk<HermesRepository>()
        val savedSessions = mutableListOf<SessionEntity>()
        val savedMessages = mutableListOf<MessageEntity>()
        val baseSession = session()

        coEvery { repository.cachedSession(any()) } answers {
            val sessionId = firstArg<String>()
            savedSessions.lastOrNull { it.id == sessionId }
                ?: baseSession.takeIf { sessionId == it.id }
        }
        coEvery { repository.cachedMessages(any()) } answers {
            val sessionId = firstArg<String>()
            savedMessages.filter { it.sessionId == sessionId }
        }
        coEvery { repository.saveLocalSession(capture(savedSessions)) } just runs
        coEvery { repository.saveLocalMessage(capture(savedMessages)) } just runs
        coEvery { repository.deleteLocalMessages(any(), any()) } answers {
            val sessionId = firstArg<String>()
            val ids = secondArg<List<Long>>()
            savedMessages.removeAll { it.sessionId == sessionId && it.id in ids }
            Unit
        }
        coEvery { repository.pushMessageToRemote(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { repository.markSessionRead(any(), any()) } just runs
        coEvery { repository.completeChat(any(), any<String>()) } returns listOf(
            SseEvent.Opened(null),
            SseEvent.Delta("Hi"),
            SseEvent.Done,
        )
        every { repository.streamChat(any(), "session-1") } answers {
            eventsFactory?.invoke()
                ?: events
                ?: flowOf(
                    SseEvent.Opened(null),
                    SseEvent.Tool(ToolProgress(label = "search", status = "running")),
                    SseEvent.Delta("Hi"),
                    SseEvent.Done,
                )
        }
        return RepositoryFixture(repository, savedSessions, savedMessages)
    }

    private fun streamCommand(
        request: ChatCompletionRequest,
        onSessionResolved: suspend (String) -> Unit = {},
        onFinished: () -> Unit = {},
        onFailed: (String) -> Unit = {},
    ): ChatStreamCommand {
        return ChatStreamCommand(
            session = session(),
            userMessage = MessageEntity(
                id = 1L,
                sessionId = "session-1",
                role = "user",
                content = "Hello",
                timestamp = 1L,
            ),
            assistantMessageId = 2L,
            requestBuilder = { request },
            onSessionResolved = onSessionResolved,
            onFinished = onFinished,
            onFailed = onFailed,
        )
    }

    private fun session(): SessionEntity {
        return SessionEntity(
            id = "session-1",
            title = "Hermes",
            source = "mobile",
            startedAt = 1L,
            endedAt = null,
            messageCount = 0,
            model = "hermes-agent",
            localLastActivityAt = 1L,
        )
    }

    private data class RepositoryFixture(
        val repository: HermesRepository,
        val savedSessions: List<SessionEntity>,
        val savedMessages: List<MessageEntity>,
    )
}
