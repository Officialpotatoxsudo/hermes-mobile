package com.hermes.mobile.feature.connection

import com.hermes.mobile.core.auth.SavedConnection
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.data.HermesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
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
class ConnectionSetupViewModelTest {
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
    fun rejectsFirstTimeBlankApiKey() = runTest(dispatcher) {
        val tokenStore = mockTokenStore()
        val repository = mockk<HermesRepository>()
        val viewModel = ConnectionSetupViewModel(tokenStore, repository)
        var saved = false

        viewModel.onServerUrlChanged("agent.example")
        viewModel.onApiKeyChanged("   ")
        viewModel.checkAndSave { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertFalse(viewModel.uiState.value.isHealthy)
        assertEquals("Enter API key", viewModel.uiState.value.error)
        coVerify(exactly = 0) { repository.checkHealth(any(), any()) }
        coVerify(exactly = 0) { tokenStore.saveCredentials(any(), any()) }
    }

    @Test
    fun editingConnectionWithBlankApiKeyKeepsExistingKey() = runTest(dispatcher) {
        val tokenStore = mockTokenStore(
            SavedConnection(
                serverUrl = "https://old.example",
                apiKey = "saved-key",
                identity = "identity",
            ),
        )
        val repository = mockk<HermesRepository>()
        coEvery { repository.checkHealth("https://agent.example", "saved-key") } returns Result.success(Unit)
        coEvery { tokenStore.saveCredentials("https://agent.example", "saved-key") } just runs
        val viewModel = ConnectionSetupViewModel(tokenStore, repository)
        var saved = false
        advanceUntilIdle()

        viewModel.onServerUrlChanged("agent.example")
        viewModel.onApiKeyChanged("   ")
        viewModel.checkAndSave { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertTrue(viewModel.uiState.value.isHealthy)
        assertTrue(viewModel.uiState.value.isEditingExistingConnection)
        assertEquals("", viewModel.uiState.value.apiKey)
        coVerify { repository.checkHealth("https://agent.example", "saved-key") }
        coVerify { tokenStore.saveCredentials("https://agent.example", "saved-key") }
    }

    @Test
    fun savesFirstSafePastedApiKeyLine() = runTest(dispatcher) {
        val tokenStore = mockTokenStore()
        val repository = mockk<HermesRepository>()
        coEvery { repository.checkHealth("https://agent.example", "api-key") } returns Result.success(Unit)
        coEvery { tokenStore.saveCredentials("https://agent.example", "api-key") } just runs
        val viewModel = ConnectionSetupViewModel(tokenStore, repository)
        var saved = false

        viewModel.onServerUrlChanged("agent.example")
        viewModel.onApiKeyChanged("\n api-key\nInjected: bad ")
        viewModel.checkAndSave { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertEquals("api-key", viewModel.uiState.value.apiKey)
        coVerify { repository.checkHealth("https://agent.example", "api-key") }
        coVerify { tokenStore.saveCredentials("https://agent.example", "api-key") }
    }

    @Test
    fun editingFieldsClearsVerifiedState() = runTest(dispatcher) {
        val tokenStore = mockTokenStore()
        val repository = mockk<HermesRepository>()
        coEvery { repository.checkHealth("https://agent.example", "key") } returns Result.success(Unit)
        coEvery { tokenStore.saveCredentials("https://agent.example", "key") } just runs
        val viewModel = ConnectionSetupViewModel(tokenStore, repository)

        viewModel.onServerUrlChanged("agent.example")
        viewModel.onApiKeyChanged("key")
        viewModel.checkAndSave {}
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isHealthy)

        viewModel.onServerUrlChanged("other.example")
        assertFalse(viewModel.uiState.value.isHealthy)

        val secondTokenStore = mockTokenStore()
        val secondRepository = mockk<HermesRepository>()
        coEvery { secondRepository.checkHealth("https://agent.example", "key") } returns Result.success(Unit)
        coEvery { secondTokenStore.saveCredentials("https://agent.example", "key") } just runs
        val secondViewModel = ConnectionSetupViewModel(secondTokenStore, secondRepository)

        secondViewModel.onServerUrlChanged("agent.example")
        secondViewModel.onApiKeyChanged("key")
        secondViewModel.checkAndSave {}
        advanceUntilIdle()
        assertTrue(secondViewModel.uiState.value.isHealthy)

        secondViewModel.onApiKeyChanged("new-key")
        assertFalse(secondViewModel.uiState.value.isHealthy)
    }

    @Test
    fun staleHealthCheckResultDoesNotVerifyEditedFields() = runTest(dispatcher) {
        val tokenStore = mockTokenStore()
        val repository = mockk<HermesRepository>()
        val healthResult = CompletableDeferred<Result<Unit>>()
        coEvery { repository.checkHealth("https://agent.example", "key") } coAnswers { healthResult.await() }
        val viewModel = ConnectionSetupViewModel(tokenStore, repository)
        var saved = false

        viewModel.onServerUrlChanged("agent.example")
        viewModel.onApiKeyChanged("key")
        viewModel.checkAndSave { saved = true }
        assertTrue(viewModel.uiState.value.isChecking)

        viewModel.onServerUrlChanged("other.example")
        assertFalse(viewModel.uiState.value.isChecking)

        healthResult.complete(Result.success(Unit))
        advanceUntilIdle()

        assertFalse(saved)
        assertFalse(viewModel.uiState.value.isHealthy)
        assertEquals("other.example", viewModel.uiState.value.serverUrl)
        coVerify(exactly = 0) { tokenStore.saveCredentials(any(), any()) }
    }

    private fun mockTokenStore(savedConnection: SavedConnection = SavedConnection()): TokenStore {
        val tokenStore = mockk<TokenStore>()
        coEvery { tokenStore.savedConnectionOnce() } returns savedConnection
        return tokenStore
    }
}
