package com.hermes.mobile.feature.chat

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.DashboardModelOptionsResponse
import com.hermes.mobile.core.network.SseEvent
import com.hermes.mobile.core.settings.AppPreferences
import io.mockk.every
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMediaStorageTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createsDurableMediaFilesUnderAppFiles() {
        val context = mockk<Context>()
        val filesDir = tempDir.toFile()
        every { context.filesDir } returns filesDir

        val file = createHermesMediaFile(
            context = context,
            kind = "photo",
            extension = ".jpg",
            timestamp = 1234L,
        )

        assertEquals(
            File(filesDir, "hermes-media/hermes-photo-1234.jpg"),
            file,
        )
        assertTrue(file.parentFile?.exists() == true)
    }

    @Test
    fun sanitizesMediaFileNames() {
        val context = mockk<Context>()
        val filesDir = tempDir.toFile()
        every { context.filesDir } returns filesDir

        val file = createHermesMediaFile(
            context = context,
            kind = "voice note",
            extension = "",
            timestamp = 42L,
        )

        assertEquals(
            File(filesDir, "hermes-media/hermes-voicenote-42.bin"),
            file,
        )
    }

    @Test
    fun sanitizesPastedMediaFileExtension() {
        val context = mockk<Context>()
        val filesDir = tempDir.toFile()
        every { context.filesDir } returns filesDir

        val file = createHermesMediaFile(
            context = context,
            kind = "\n voice-note \n ignored",
            extension = "\n .m4a/../../bad \n ignored",
            timestamp = 99L,
        )

        assertEquals(
            File(filesDir, "hermes-media/hermes-voice-note-99.m4a"),
            file,
        )
    }

    @Test
    fun deletingMessageRemovesCopiedHermesMedia() = runTest(dispatcher) {
        val repository = mockk<HermesRepository>()
        val context = mockk<Context>()
        val resolver = mockk<ContentResolver>()
        val filesDir = tempDir.toFile()
        every { context.filesDir } returns filesDir
        every { context.contentResolver } returns resolver

        val file = createHermesMediaFile(
            context = context,
            kind = "photo",
            extension = ".jpg",
            timestamp = 77L,
        ).apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val uri = "content://com.hermes.mobile.fileprovider/hermes_media/${file.name}"

        every { resolver.getType(any<Uri>()) } returns "image/jpeg"
        every { resolver.openInputStream(any<Uri>()) } answers { file.inputStream() }
        coEvery { repository.fetchModelOptions() } returns Result.success(DashboardModelOptionsResponse())
        coEvery { repository.saveLocalSession(any()) } just runs
        coEvery { repository.saveLocalMessage(any()) } just runs
        coEvery { repository.deleteLocalMessages(any(), any()) } just runs
        every { repository.streamChat(any<ChatCompletionRequest>(), any<String>()) } returns flowOf(SseEvent.Done)

        val appPreferences = mockk<AppPreferences>(relaxed = true) {
            every { agents } returns flowOf(AppPreferences.defaultAgents)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), repository, appPreferences, context)
        advanceUntilIdle()

        viewModel.addAttachment(uri = uri, label = "Photo", kind = "image")
        viewModel.sendCurrentDraft()
        advanceUntilIdle()
        withContext(Dispatchers.IO) {}
        val userMessage = viewModel.uiState.value.messages.first { it.role == "user" }

        viewModel.deleteMessage(userMessage.id)
        advanceUntilIdle()
        withContext(Dispatchers.IO) {}
        Thread.sleep(100)
        advanceUntilIdle()

        assertFalse(file.exists())
    }
}
