package com.hermes.mobile.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.data.local.MessageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHistoryViewModelTest {
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
    fun cleansSavedSessionIdBeforeSyncAndDisplay() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        every { repository.messages("session-1") } returns flowOf(emptyList())
        coEvery { repository.syncMessages("session-1") } returns Result.success(Unit)
        coEvery { repository.markSessionRead("session-1", any()) } returns Unit

        val viewModel = SessionHistoryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "\n session-1\nignored ")),
            repository = repository,
        )
        advanceUntilIdle()

        assertEquals("session-1", viewModel.uiState.value.sessionId)
        coVerify { repository.syncMessages("session-1") }
        coVerify { repository.markSessionRead("session-1", any()) }
    }

    @Test
    fun blankSavedSessionIdDoesNotSync() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()

        val viewModel = SessionHistoryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "   ")),
            repository = repository,
        )
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.sessionId)
        assertEquals("Session not found", viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isSyncing)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        coVerify(exactly = 0) { repository.syncMessages(any()) }
        coVerify(exactly = 0) { repository.markSessionRead(any(), any()) }
    }

    @Test
    fun cachedMessagesRemainVisibleWhenHistorySyncFails() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        every { repository.messages("session-1") } returns flowOf(emptyList())
        coEvery { repository.cachedMessages("session-1") } returns listOf(
            MessageEntity(
                id = 1L,
                sessionId = "session-1",
                role = "user",
                content = "cached",
                timestamp = 1L,
            ),
        )
        coEvery { repository.markSessionRead("session-1", any()) } returns Unit
        coEvery { repository.syncMessages("session-1") } returns Result.failure(RuntimeException("HTTP 503"))

        val viewModel = SessionHistoryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to "session-1")),
            repository = repository,
        )
        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(listOf("cached"), viewModel.uiState.value.messages.map { it.content })
        assertEquals("HTTP 503", viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isSyncing)
        collectJob.cancel()
    }
}
