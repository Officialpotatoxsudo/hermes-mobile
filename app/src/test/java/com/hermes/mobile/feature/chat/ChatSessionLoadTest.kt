package com.hermes.mobile.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.network.ConnectionState
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatSessionLoadTest {
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
    fun cachedSessionLoadKeepsSyncFailureVisible() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.cachedMessages("session-1") } returns listOf(
            MessageEntity(
                id = 1L,
                sessionId = "session-1",
                role = "user",
                content = "cached",
                timestamp = 1L,
            ),
        )
        coEvery { repository.syncMessages("session-1") } returns Result.failure(RuntimeException("offline"))
        every { repository.messages("session-1") } returns flowOf(emptyList())

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activeSessionId" to "session-1")),
            repository = repository,
            appPreferences = appPreferences(),
            appContext = context,
        )
        advanceUntilIdle()

        assertEquals(listOf("cached"), viewModel.uiState.value.messages.map { it.content })
        assertFalse(viewModel.uiState.value.isConnecting)
        assertEquals("offline", viewModel.uiState.value.error)
        assertEquals(ConnectionState.Error("offline"), viewModel.uiState.value.connectionState)
    }

    @Test
    fun savedSessionIdAndAgentNameUseFirstCleanLineOnRestore() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.syncMessages("session-1") } returns Result.success(Unit)
        every { repository.messages("session-1") } returns flowOf(emptyList())

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "activeSessionId" to "\n session-1\nignored ",
                    "agentName" to "\n Hermes\nIgnored ",
                ),
            ),
            repository = repository,
            appPreferences = appPreferences(),
            appContext = context,
        )
        advanceUntilIdle()

        assertEquals("session-1", viewModel.uiState.value.sessionId)
        assertEquals("Hermes", viewModel.uiState.value.agentName)
    }

    @Test
    fun loadSessionRestoresCachedMessagesPrunedBySync() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val firstMessage = MessageEntity(
            id = 1L,
            sessionId = "session-1",
            role = "user",
            content = "first",
            timestamp = 1L,
        )
        val secondMessage = MessageEntity(
            id = 2L,
            sessionId = "session-1",
            role = "user",
            content = "second",
            timestamp = 2L,
        )

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        var cachedMessageCalls = 0
        coEvery { repository.cachedMessages("session-1") } answers {
            cachedMessageCalls += 1
            if (cachedMessageCalls == 1) listOf(firstMessage, secondMessage) else listOf(secondMessage)
        }
        coEvery { repository.syncMessages("session-1") } returns Result.success(Unit)
        coEvery { repository.saveLocalMessages(any()) } just runs
        every { repository.messages("session-1") } returns flowOf(emptyList())

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activeSessionId" to "session-1")),
            repository = repository,
            appPreferences = appPreferences(),
            appContext = context,
        )
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), viewModel.uiState.value.messages.map { it.content })
        coVerify {
            repository.saveLocalMessages(match { messages ->
                messages.single().id == firstMessage.id && messages.single().content == "first"
            })
        }
    }

    @Test
    fun restoredAgentChatUsesStoredAgentName() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val appPreferences = appPreferences(
            agentProfiles = listOf(
                AgentProfile(
                    id = "research",
                    name = "Research Agent",
                    subtitle = "Finds context",
                    initial = "R",
                ),
            ),
        )

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.syncMessages("agent-chat-research--1000") } returns Result.success(Unit)
        every { repository.messages("agent-chat-research--1000") } returns flowOf(emptyList())

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "agent-chat-research--1000")),
            repository = repository,
            appPreferences = appPreferences,
            appContext = context,
        )
        advanceUntilIdle()

        assertEquals("Research Agent", viewModel.uiState.value.agentName)
    }

    @Test
    fun restorePrefersResolvedActiveSessionOverStaleRouteSession() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.syncMessages("remote-session-1") } returns Result.success(Unit)
        coEvery { repository.cachedMessages("remote-session-1") } returns listOf(
            MessageEntity(
                id = 1L,
                sessionId = "remote-session-1",
                role = "user",
                content = "resolved",
                timestamp = 1L,
            ),
        )
        every { repository.messages("remote-session-1") } returns flowOf(emptyList())

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "sessionId" to "stale-local-session",
                    "activeSessionId" to "remote-session-1",
                ),
            ),
            repository = repository,
            appPreferences = appPreferences(),
            appContext = context,
        )
        advanceUntilIdle()

        assertEquals("remote-session-1", viewModel.uiState.value.sessionId)
        assertEquals(listOf("resolved"), viewModel.uiState.value.messages.map { it.content })
        coVerify(exactly = 0) { repository.syncMessages("stale-local-session") }
    }

    @Test
    fun editingLegacyPhotoOnlyMessageRestoresBlankDraftAndPhoto() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val legacyContent = """
            Photo attached.

            Attachments:
            content://media/picker/0/photo/1
        """.trimIndent()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.syncMessages("session-1") } returns Result.success(Unit)
        coEvery { repository.deleteLocalMessages(any(), any()) } just runs
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.cachedMessages("session-1") } returns listOf(
            MessageEntity(
                id = 1L,
                sessionId = "session-1",
                role = "user",
                content = legacyContent,
                timestamp = 1L,
            ),
        )
        every { repository.messages("session-1") } returns flowOf(
            listOf(
                MessageEntity(
                    id = 1L,
                    sessionId = "session-1",
                    role = "user",
                    content = legacyContent,
                    timestamp = 1L,
                ),
            ),
        )

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activeSessionId" to "session-1")),
            repository = repository,
            appPreferences = appPreferences(),
            appContext = context,
        )
        advanceUntilIdle()
        val userMessage = viewModel.uiState.value.messages.single()

        viewModel.editMessage(userMessage.id)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals(listOf("content://media/picker/0/photo/1"), viewModel.uiState.value.attachments.map { it.uri })
        assertEquals(emptyList<ChatUiMessage>(), viewModel.uiState.value.messages)
    }

    @Test
    fun activityLabelUsesLatestToolName() {
        val state = ChatUiState(
            tools = listOf(
                ToolProgress(label = "browser", status = "done"),
                ToolProgress(label = "tool output", tool = "search", status = "running"),
            ),
            isConnecting = true,
        )

        assertEquals("searching web · running", activityLabel(state))
    }

    @Test
    fun activityLabelUsesThinkingBeforeFirstToken() {
        val state = ChatUiState(
            isStreaming = true,
            messages = listOf(ChatUiMessage(id = "assistant-1", role = "assistant", content = "", time = "10:00", isStreaming = true)),
        )

        assertEquals("thinking", activityLabel(state))
    }

    @Test
    fun activityLabelNeverUsesConnectingForToolOrStreamState() {
        val labels = listOf(
            activityLabel(ChatUiState(tools = listOf(ToolProgress(label = "search")), isConnecting = true)),
            activityLabel(ChatUiState(isStreaming = true, messages = listOf(ChatUiMessage("assistant-1", "assistant", "", "10:00", isStreaming = true)))),
            activityLabel(ChatUiState(isStreaming = true, messages = listOf(ChatUiMessage("assistant-1", "assistant", "Hi", "10:00", isStreaming = true)))),
            activityLabel(ChatUiState(isStreaming = true, queuedPrompts = listOf(PendingPrompt(id = "queued-1", text = "next")))),
        )

        assertFalse(labels.any { it == "connecting" })
        assertEquals(listOf("searching web", "thinking", "typing", "queued"), labels)
    }

    @Test
    fun activityLabelIgnoresStaleToolAfterStreamStops() {
        val state = ChatUiState(
            tools = listOf(ToolProgress(label = "tool output", tool = "search", status = "running")),
            isConnecting = false,
            isStreaming = false,
            connectionState = ConnectionState.Connected,
        )

        assertEquals("connected", activityLabel(state))
    }

    @Test
    fun persistSessionSnapshotPreservesExistingMetadata() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val appPreferences = appPreferences()
        val context = mockk<Context>(relaxed = true)
        val existing = SessionEntity(
            id = "session-1",
            title = "Old title",
            source = "desktop",
            startedAt = 100L,
            endedAt = 200L,
            messageCount = 1,
            model = "remote-model",
            lastSyncedAt = 300L,
        )
        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.syncMessages("session-1") } returns Result.success(Unit)
        every { repository.messages("session-1") } returns flowOf(emptyList())
        coEvery { repository.saveLocalMessage(any()) } just runs
        coEvery { repository.cachedSession("session-1") } returns existing
        coEvery { repository.saveLocalSession(any()) } just runs
        every { repository.streamChat(any(), any<String>()) } returns flowOf(com.hermes.mobile.core.network.SseEvent.Done)

        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(mapOf("activeSessionId" to "session-1")),
            repository = repository,
            appPreferences = appPreferences,
            appContext = context,
        )
        advanceUntilIdle()

        viewModel.onDraftChanged("hello")
        viewModel.sendCurrentDraft()
        advanceUntilIdle()

        coVerify {
            repository.saveLocalSession(
                match { saved ->
                    saved.id == "session-1" &&
                        saved.source == "desktop" &&
                        saved.startedAt == 100L &&
                        saved.endedAt == 200L &&
                        saved.model == "remote-model" &&
                        saved.lastSyncedAt == 300L &&
                        saved.localLastActivityAt >= 100L
                },
            )
        }
        coVerify { appPreferences.setLastOpenedChatSessionId("session-1") }
    }

    private fun appPreferences(agentProfiles: List<AgentProfile> = AppPreferences.defaultAgents): AppPreferences {
        return mockk(relaxed = true) {
            every { agents } returns flowOf(agentProfiles)
        }
    }
}
