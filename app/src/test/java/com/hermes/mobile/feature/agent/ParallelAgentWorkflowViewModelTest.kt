package com.hermes.mobile.feature.agent

import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.feature.chat.ChatStreamCommand
import com.hermes.mobile.feature.chat.ChatStreamCoordinator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParallelAgentWorkflowViewModelTest {
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
    fun startsOneStreamPerSelectedAgentWithAgentInstructions() = runTest(dispatcher) {
        val agents = listOf(
            AgentProfile("research", "Research", "Find sources", "R", instructions = "Use sources."),
            AgentProfile("writer", "Writer", "Draft copy", "W", instructions = "Be concise."),
        )
        val appPreferences = mockk<AppPreferences>()
        every { appPreferences.agents } returns flowOf(agents)
        val coordinator = mockk<ChatStreamCoordinator>()
        val commandSlot = slot<ChatStreamCommand>()
        val commands = mutableListOf<ChatStreamCommand>()
        every { coordinator.start(capture(commandSlot)) } answers {
            commands += commandSlot.captured
            Job()
        }
        val viewModel = ParallelAgentWorkflowViewModel(
            appPreferences = appPreferences,
            streamCoordinator = coordinator,
            now = { 1_000L + commands.size * 10L },
        )
        advanceUntilIdle()

        viewModel.onPromptChanged("Plan the launch")
        viewModel.runWorkflow()
        advanceUntilIdle()

        assertEquals(listOf("research", "writer"), commands.map { it.session.id.substringAfter("agent-chat-").substringBefore("--") })
        assertEquals(listOf("Plan the launch", "Plan the launch"), commands.map { it.userMessage.content })

        val encodedRequests = commands.map { Json.encodeToString(it.requestBuilder()) }
        assertTrue(encodedRequests.all { it.contains("\"model\":\"hermes-agent\"") })
        assertTrue(encodedRequests[0].contains("\"role\":\"system\""))
        assertTrue(encodedRequests[0].contains("Use sources."))
        assertTrue(encodedRequests[1].contains("Be concise."))
    }
}
