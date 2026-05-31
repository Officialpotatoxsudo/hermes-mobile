package com.hermes.mobile.feature.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.network.ConnectionState
import com.hermes.mobile.core.network.SseEvent
import com.hermes.mobile.core.settings.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class ChatRetryPayloadTest {
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
    fun retryResendsPhotoPayload() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val requests = mutableListOf<ChatCompletionRequest>()
        val firstRequest = CompletableDeferred<Unit>()
        val secondRequest = CompletableDeferred<Unit>()
        val image = "data:image/jpeg;base64,AQID"

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            when (requests.size) {
                1 -> firstRequest.complete(Unit)
                2 -> secondRequest.complete(Unit)
            }
            flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.addAttachment(uri = image, label = "Photo", kind = "image")
        assertEquals(1, viewModel.uiState.value.attachments.size)
        viewModel.sendCurrentDraft()
        assertEquals(2, viewModel.uiState.value.messages.size)
        firstRequest.await()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.error)

        viewModel.retryLast()
        secondRequest.await()
        advanceUntilIdle()

        assertEquals(2, requests.size)
        assertTrue(requests[0].messages.last().content.toString().contains(image))
        assertTrue(requests[1].messages.last().content.toString().contains(image))
        assertEquals(
            2,
            viewModel.uiState.value.messages.count { it.role == "user" && it.imageUris == listOf(image) },
        )
    }

    @Test
    fun sendingDuplicatePhotosKeepsOnePreviewImage() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val image = "data:image/jpeg;base64,AQID"
        val requestStarted = CompletableDeferred<Unit>()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requestStarted.complete(Unit)
            flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.addAttachment(uri = image, label = "Photo", kind = "image")
        viewModel.addAttachment(uri = " $image ", label = "Photo", kind = "image")
        viewModel.sendCurrentDraft()
        requestStarted.await()
        advanceUntilIdle()

        assertEquals(
            listOf(image),
            viewModel.uiState.value.messages.single { it.role == "user" }.imageUris,
        )
    }

    @Test
    fun sendingUppercaseRemotePhotoUrlIsIgnored() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val requests = mutableListOf<ChatCompletionRequest>()
        val image = "HTTPS://example.com/photo.jpg"

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.addAttachment(uri = image, label = "Photo", kind = "image")
        assertTrue(viewModel.uiState.value.attachments.isEmpty())
        viewModel.sendCurrentDraft()
        advanceUntilIdle()

        assertEquals(0, requests.size)
    }

    @Test
    fun attachmentTrayIgnoresBlankAndDuplicateUris() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val image = "data:image/jpeg;base64,AQID"
        val voice = "file:///tmp/voice.m4a"

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.addAttachment(uri = "   ", label = "Photo", kind = "image")
        viewModel.addAttachment(uri = "\n $image \n ignored", label = "\n Photo \n ignored", kind = "\n image/png \n ignored")
        viewModel.addAttachment(uri = image, label = "Photo copy", kind = "image")
        viewModel.addVoiceRecording(uri = "   ", label = "Voice")
        viewModel.addVoiceRecording(uri = "\n $voice \n ignored", label = "\n Voice note \n ignored")
        viewModel.addVoiceRecording(uri = voice, label = "Voice copy")

        assertEquals(listOf(image, voice), viewModel.uiState.value.attachments.map { it.uri })
        assertEquals(listOf("image", "voice"), viewModel.uiState.value.attachments.map { it.kind })
        assertEquals(listOf("Photo", "Voice note"), viewModel.uiState.value.attachments.map { it.label })
    }

    @Test
    fun editPhotoMessageRestoresPhotoAttachment() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val image = "data:image/jpeg;base64,AQID"
        val requests = mutableListOf<ChatCompletionRequest>()
        val requestStarted = CompletableDeferred<Unit>()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        coEvery { repository.deleteLocalMessages(any(), any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            requestStarted.complete(Unit)
            flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.addAttachment(uri = image, label = "Photo", kind = "image")
        viewModel.sendCurrentDraft()
        requestStarted.await()
        advanceUntilIdle()
        val userMessage = viewModel.uiState.value.messages.first { it.role == "user" }

        viewModel.editMessage(userMessage.id)

        assertEquals("", viewModel.uiState.value.draft)
        assertEquals(listOf(image), viewModel.uiState.value.attachments.map { it.uri })
        assertEquals(listOf("image"), viewModel.uiState.value.attachments.map { it.kind })
        assertTrue(viewModel.uiState.value.messages.none { it.id == userMessage.id })
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        viewModel.retryLast()
        advanceUntilIdle()

        assertEquals(1, requests.size)
    }

    @Test
    fun deleteUserMessageRemovesWholeExchangeAndClearsRetry() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val requests = mutableListOf<ChatCompletionRequest>()
        val requestStarted = CompletableDeferred<Unit>()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        coEvery { repository.deleteLocalMessages(any(), any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            requestStarted.complete(Unit)
            flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.onDraftChanged("delete me")
        viewModel.sendCurrentDraft()
        requestStarted.await()
        advanceUntilIdle()
        val userMessage = viewModel.uiState.value.messages.first { it.role == "user" }
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.deleteMessage(userMessage.id)
        viewModel.retryLast()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(1, requests.size)
        coVerify { repository.deleteLocalMessages(viewModel.uiState.value.sessionId.orEmpty(), any()) }
    }

    @Test
    fun stopClearsQueueWithoutSendingNextPrompt() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val firstStream = MutableSharedFlow<SseEvent>()
        val requests = mutableListOf<ChatCompletionRequest>()
        val firstRequestStarted = CompletableDeferred<Unit>()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            if (requests.size == 1) firstRequestStarted.complete(Unit)
            if (requests.size == 1) firstStream else flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.onDraftChanged("first")
        viewModel.sendCurrentDraft()
        firstRequestStarted.await()
        advanceUntilIdle()
        assertEquals(1, requests.size)

        viewModel.onDraftChanged("second")
        viewModel.sendCurrentDraft()
        assertEquals(1, viewModel.uiState.value.queuedPrompts.size)

        viewModel.stop()
        advanceUntilIdle()

        assertEquals(1, requests.size)
        assertTrue(viewModel.uiState.value.queuedPrompts.isEmpty())
        assertEquals(false, viewModel.uiState.value.isConnecting)
        assertEquals(false, viewModel.uiState.value.isStreaming)
        assertEquals(ConnectionState.Disconnected, viewModel.uiState.value.connectionState)
    }

    @Test
    fun queuedPromptSendsAfterFirstStreamCompletes() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val firstStream = MutableSharedFlow<SseEvent>()
        val requests = mutableListOf<ChatCompletionRequest>()
        val firstRequestStarted = CompletableDeferred<Unit>()
        val secondRequestStarted = CompletableDeferred<Unit>()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            when (requests.size) {
                1 -> firstRequestStarted.complete(Unit)
                2 -> secondRequestStarted.complete(Unit)
            }
            if (requests.size == 1) firstStream else flowOf(SseEvent.Done)
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.onDraftChanged("first")
        viewModel.sendCurrentDraft()
        firstRequestStarted.await()
        advanceUntilIdle()
        viewModel.onDraftChanged("second")
        viewModel.sendCurrentDraft()

        assertEquals(1, requests.size)
        assertEquals(1, viewModel.uiState.value.queuedPrompts.size)

        firstStream.emit(SseEvent.Done)
        secondRequestStarted.await()
        advanceUntilIdle()

        assertEquals(2, requests.size)
        assertTrue(viewModel.uiState.value.queuedPrompts.isEmpty())
        assertEquals(
            listOf("first", "second"),
            viewModel.uiState.value.messages.filter { it.role == "user" }.map { it.content },
        )
    }

    @Test
    fun failedStreamClearsQueuedPrompts() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>(relaxed = true)
        val failNow = CompletableDeferred<Unit>()
        val requests = mutableListOf<ChatCompletionRequest>()
        val requestStarted = CompletableDeferred<Unit>()

        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } answers {
            requests += firstArg<ChatCompletionRequest>()
            requestStarted.complete(Unit)
            flow {
                failNow.await()
                throw IOException("offline")
            }
        }

        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences(), context)
        advanceUntilIdle()

        viewModel.onDraftChanged("first")
        viewModel.sendCurrentDraft()
        requestStarted.await()
        advanceUntilIdle()
        viewModel.onDraftChanged("second")
        viewModel.sendCurrentDraft()

        assertEquals(1, viewModel.uiState.value.queuedPrompts.size)

        failNow.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.queuedPrompts.isEmpty())
        assertEquals(
            listOf("first"),
            viewModel.uiState.value.messages.filter { it.role == "user" }.map { it.content },
        )
        assertTrue(requests.all { request -> request.messages.last().content.toString().contains("first") })
    }

    private fun appPreferences(): AppPreferences {
        return mockk(relaxed = true) {
            every { agents } returns flowOf(AppPreferences.defaultAgents)
        }
    }
}
