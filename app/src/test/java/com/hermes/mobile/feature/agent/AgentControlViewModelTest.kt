package com.hermes.mobile.feature.agent

import com.hermes.mobile.core.data.HermesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentControlViewModelTest {
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
    fun opensWithServerHealthAndLoadsDirectEndpoint() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        coEvery { repository.getText("health") } returns Result.success("ok")

        val viewModel = AgentControlViewModel(repository)
        advanceUntilIdle()

        assertEquals("server.health", viewModel.uiState.value.selectedAction?.id)
        assertEquals("ok", viewModel.uiState.value.resultText)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { repository.getText("health") }
    }

    @Test
    fun selectingControlCardUsesDirectGetAndIgnoresDuplicateTapsWhileLoading() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val result = kotlinx.coroutines.CompletableDeferred<Result<String>>()
        coEvery { repository.getText("health") } returns Result.success("ok")
        coEvery { repository.getText("v1/capabilities") } coAnswers { result.await() }
        val viewModel = AgentControlViewModel(repository)
        advanceUntilIdle()

        val capabilities = viewModel.uiState.value.actions.first { it.id == "server.capabilities" }
        viewModel.selectAction(capabilities)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.selectAction(capabilities)
        result.complete(Result.success("capabilities"))
        advanceUntilIdle()

        assertEquals("capabilities", viewModel.uiState.value.resultText)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 1) { repository.getText("v1/capabilities") }
        coVerify(exactly = 0) { repository.postText(any(), any()) }
    }

    @Test
    fun staleControlResponseDoesNotOverwriteCurrentSelection() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val slowHealth = kotlinx.coroutines.CompletableDeferred<Result<String>>()
        val fastCapabilities = kotlinx.coroutines.CompletableDeferred<Result<String>>()
        coEvery { repository.getText("health") } coAnswers { slowHealth.await() }
        coEvery { repository.getText("v1/capabilities") } coAnswers { fastCapabilities.await() }
        val viewModel = AgentControlViewModel(repository)

        val capabilities = viewModel.uiState.value.actions.first { it.id == "server.capabilities" }
        viewModel.selectAction(capabilities)
        fastCapabilities.complete(Result.success("capabilities"))
        advanceUntilIdle()

        assertEquals("server.capabilities", viewModel.uiState.value.selectedAction?.id)
        assertEquals("capabilities", viewModel.uiState.value.resultText)
        assertFalse(viewModel.uiState.value.isLoading)

        slowHealth.complete(Result.success("late health"))
        advanceUntilIdle()

        assertEquals("server.capabilities", viewModel.uiState.value.selectedAction?.id)
        assertEquals("capabilities", viewModel.uiState.value.resultText)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
