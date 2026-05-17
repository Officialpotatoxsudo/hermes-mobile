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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        advanceUntilIdle()
        val initialReadAt = repository.savedSessions.last().lastReadAt

        coordinator.markSessionHidden("session-1")
        events.send(SseEvent.Opened(null))
        events.send(SseEvent.Delta("Working"))
        advanceUntilIdle()

        assertEquals(0, repository.savedSessions.last().unreadCount)
        assertEquals(initialReadAt, repository.savedSessions.last().lastReadAt)

        events.send(SseEvent.Done)
        events.close()
        advanceUntilIdle()

        assertEquals(1, repository.savedSessions.last().unreadCount)
        assertEquals(initialReadAt, repository.savedSessions.last().lastReadAt)
    }

    private fun mockRepository(events: Flow<SseEvent>? = null): RepositoryFixture {
        val repository = mockk<HermesRepository>()
        val savedSessions = mutableListOf<SessionEntity>()
        val savedMessages = mutableListOf<MessageEntity>()
        val baseSession = session()

        coEvery { repository.cachedSession("session-1") } answers {
            savedSessions.lastOrNull() ?: baseSession
        }
        coEvery { repository.cachedMessages("session-1") } answers {
            savedMessages.filter { it.sessionId == "session-1" }
        }
        coEvery { repository.saveLocalSession(capture(savedSessions)) } just runs
        coEvery { repository.saveLocalMessage(capture(savedMessages)) } just runs
        coEvery { repository.pushMessageToRemote(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { repository.markSessionRead(any(), any()) } just runs
        every { repository.streamChat(any(), "session-1") } returns (
            events ?: flowOf(
                SseEvent.Opened(null),
                SseEvent.Tool(ToolProgress(label = "search", status = "running")),
                SseEvent.Delta("Hi"),
                SseEvent.Done,
            )
            )
        return RepositoryFixture(repository, savedSessions, savedMessages)
    }

    private fun streamCommand(request: ChatCompletionRequest): ChatStreamCommand {
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
