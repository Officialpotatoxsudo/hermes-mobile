package com.hermes.mobile.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.DashboardProviderDto
import com.hermes.mobile.core.model.chatRequestMessage
import com.hermes.mobile.core.network.SseEvent
import com.hermes.mobile.core.settings.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
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
class ChatDraftPersistenceTest {
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
    fun clearCommandClearsPersistedDraft() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("draft" to "/clear"))
        val viewModel = newViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.sendCurrentDraft()

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals("", savedStateHandle["draft"])
    }

    @Test
    fun startNewChatWithSameAgentKeepsPreviousSessionIdOutOfDeletionPath() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "activeSessionId" to "agent-chat-hermes--1000",
                "draft" to "/new",
            ),
        )
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.syncMessages("agent-chat-hermes--1000") } returns Result.success(Unit)
        every { repository.messages("agent-chat-hermes--1000") } returns flowOf(
            listOf(
                MessageEntity(
                    id = 1L,
                    sessionId = "agent-chat-hermes--1000",
                    role = "user",
                    content = "old chat",
                    timestamp = 1L,
                ),
            ),
        )

        val viewModel = ChatViewModel(savedStateHandle, repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.sendCurrentDraft()

        val newSessionId = viewModel.uiState.value.sessionId.orEmpty()
        assertEquals(true, newSessionId.startsWith("agent-chat-hermes--"))
        assertEquals(false, newSessionId.endsWith("--1000"))
        assertEquals(newSessionId, savedStateHandle["activeSessionId"])
        assertEquals(emptyList<ChatUiMessage>(), viewModel.uiState.value.messages)
        coVerify(exactly = 0) { repository.deleteLocalSession("agent-chat-hermes--1000") }
    }

    @Test
    fun modelCommandClearsPersistedDraft() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("draft" to "/model Hermes"))
        val viewModel = newViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.sendCurrentDraft()

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals("", savedStateHandle["draft"])
        assertEquals("hermes-agent", viewModel.uiState.value.selectedModel.model)
    }

    @Test
    fun insertedModelCommandAcceptsFlexibleWhitespaceAndCase() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("draft" to "old"))
        val viewModel = newViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.insertCommand("/MODEL\tHermes")

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals("", savedStateHandle["draft"])
        assertEquals("hermes-agent", viewModel.uiState.value.selectedModel.model)
    }

    @Test
    fun insertedModelCommandUsesFirstUsefulLine() = runTest(dispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("draft" to "old"))
        val viewModel = newViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.insertCommand("\n /MODEL Hermes \n ignored")

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals("", savedStateHandle["draft"])
        assertEquals("hermes-agent", viewModel.uiState.value.selectedModel.model)
    }

    @Test
    fun modelOptionsUseFirstUsefulServerLines() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        coEvery { repository.fetchModelOptions() } returns Result.success(
            DashboardModelOptionsResponse(
                providers = listOf(
                    DashboardProviderDto(
                        slug = "\n openrouter \n ignored",
                        models = listOf("\n openrouter/auto \n ignored", "   "),
                    ),
                ),
                provider = "\n openrouter \n ignored",
                model = "\n openrouter/auto \n ignored",
            ),
        )

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        assertEquals(listOf("openrouter"), viewModel.uiState.value.modelOptions.map { it.provider })
        assertEquals(listOf("openrouter/auto"), viewModel.uiState.value.modelOptions.map { it.model })
        assertEquals("auto", viewModel.uiState.value.selectedModel.label)
    }

    @Test
    fun queuedPromptClearsPersistedDraft() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val firstStream = MutableSharedFlow<SseEvent>()
        val savedStateHandle = SavedStateHandle()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any(), any<String>()) } returns firstStream

        val viewModel = ChatViewModel(savedStateHandle, repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.onDraftChanged("first")
        viewModel.sendCurrentDraft()
        advanceUntilIdle()

        viewModel.onDraftChanged("queued")
        viewModel.sendCurrentDraft()

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals("", savedStateHandle["draft"])
        assertEquals(1, viewModel.uiState.value.queuedPrompts.size)
    }

    @Test
    fun reenteringChatWithActiveStreamShowsUserMessageAndTypingImmediately() = runTest(dispatcher) {
        val sessionId = "session-active"
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val stream = MutableSharedFlow<SseEvent>()
        val coordinator = ChatStreamCoordinator(repository)

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.cachedMessages(sessionId) } returns emptyList()
        coEvery { repository.syncMessages(sessionId) } returns Result.success(Unit)
        coEvery { repository.cachedSession(sessionId) } returns null
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        coEvery { repository.pushMessageToRemote(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { repository.markSessionRead(any(), any()) } just runs
        every { repository.streamChat(any(), sessionId) } returns stream

        coordinator.start(
            ChatStreamCommand(
                session = SessionEntity(
                    id = sessionId,
                    title = "Hermes",
                    source = "mobile",
                    startedAt = 1L,
                    endedAt = null,
                    messageCount = 0,
                    model = "hermes-agent",
                    localLastActivityAt = 1L,
                ),
                userMessage = MessageEntity(
                    id = 10L,
                    sessionId = sessionId,
                    role = "user",
                    content = "hello",
                    timestamp = 10L,
                ),
                assistantMessageId = 11L,
                requestBuilder = { ChatCompletionRequest(messages = listOf(chatRequestMessage("user", "hello"))) },
            ),
        )
        advanceUntilIdle()

        val viewModel = ChatViewModel(
            SavedStateHandle(mapOf("activeSessionId" to sessionId)),
            repository,
            appPreferences(),
            context,
            coordinator,
        )
        advanceUntilIdle()

        assertEquals(listOf("user", "assistant"), viewModel.uiState.value.messages.map { it.role })
        assertEquals("hello", viewModel.uiState.value.messages.first().content)
        assertEquals(true, viewModel.uiState.value.messages.last().isStreaming)
        assertEquals(true, viewModel.uiState.value.isConnecting || viewModel.uiState.value.isStreaming)

        coordinator.stop(sessionId)
    }

    private fun newViewModel(savedStateHandle: SavedStateHandle): ChatViewModel {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        return ChatViewModel(savedStateHandle, repository, appPreferences(), context)
    }

    private fun appPreferences(): AppPreferences {
        return mockk(relaxed = true) {
            every { agents } returns flowOf(AppPreferences.defaultAgents)
        }
    }
}
