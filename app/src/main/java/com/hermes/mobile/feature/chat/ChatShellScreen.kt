package com.hermes.mobile.feature.chat

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.hermesCommandSuggestions
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.network.ConnectionState
import com.hermes.mobile.core.util.agentIdFromChatSessionId
import com.hermes.mobile.core.util.ReceivedAttachment
import com.hermes.mobile.core.util.ReceivedAttachmentKind
import com.hermes.mobile.ui.components.frostedGlass
import com.hermes.mobile.ui.components.HermesCircleButton
import com.hermes.mobile.ui.components.MediaThumbnail
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardCommandKey
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class ChatBubblePalette(
    val container: Color,
    val content: Color,
    val border: Color,
)

internal fun outgoingBubblePalette(colors: ColorScheme): ChatBubblePalette {
    return ChatBubblePalette(
        container = colors.primaryContainer,
        content = colors.onPrimaryContainer,
        border = colors.primary.copy(alpha = 0.24f),
    )
}

internal fun assistantBubblePalette(colors: ColorScheme): ChatBubblePalette {
    return ChatBubblePalette(
        container = colors.surfaceContainer,
        content = colors.onSurface,
        border = colors.outlineVariant.copy(alpha = 0.42f),
    )
}

internal fun chatBubbleMaxWidth(availableWidth: Dp, isOutgoing: Boolean): Dp {
    val width = availableWidth * if (isOutgoing) 0.70f else 0.78f
    val absoluteMax = if (isOutgoing) 440.dp else 560.dp
    return width.coerceAtMost(absoluteMax).coerceAtLeast(96.dp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatShellScreen(
    onHistoryClick: (String?) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var pendingDeleteMessage by remember { mutableStateOf<ChatUiMessage?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                persistPickedPhotoAccess(context, uri)
                viewModel.addAttachment(uri = uri.toString(), label = "Photo", kind = "image")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
        uris.forEach { uri ->
            persistPickedPhotoAccess(context, uri)
            viewModel.addAttachment(uri = uri.toString(), label = "Photo", kind = "image")
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.take(48) ?: "File"
            viewModel.addAttachment(uri = uri.toString(), label = name, kind = "file")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createCameraImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    fun startRecordingSafely() {
        runCatching { startVoiceRecording(context) }
            .onSuccess { result ->
                recorder = result.recorder
                recordingFile = result.file
                isRecording = true
                recordingStartedAt = System.currentTimeMillis()
            }
            .onFailure {
                recorder = null
                recordingFile = null
                isRecording = false
                recordingStartedAt = 0L
                Toast.makeText(context, "Could not start voice recording", Toast.LENGTH_SHORT).show()
            }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startRecordingSafely()
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    fun beginRecording() {
        if (isRecording) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecordingSafely()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun finishRecording() {
        if (!isRecording) return
        val file = stopVoiceRecording(recorder, recordingFile)
        recorder = null
        recordingFile = null
        isRecording = false
        recordingStartedAt = 0L
        if (file != null) {
            viewModel.addVoiceRecording(
                uri = Uri.fromFile(file).toString(),
                label = file.name,
            )
        }
    }

    DisposableEffect(recorder, recordingFile) {
        onDispose {
            stopVoiceRecording(recorder, recordingFile)?.delete()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var composerHeightPx by remember { mutableStateOf(with(density) { 88.dp.roundToPx() }) }
    val composerHeightDp = with(density) { composerHeightPx.toDp() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val topPadding = statusBarTop + 74.dp
    val bottomPadding = (composerHeightDp + imeBottom + 20.dp).coerceAtLeast(112.dp)
    val loadingScrollState = rememberScrollState()
    val emptyScrollState = rememberScrollState()
    val isNearBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total == 0 || lastVisible >= total - 3
        }
    }
    var previousMessageCount by remember { mutableStateOf(0) }

    LaunchedEffect(imeBottom, state.messages.size, state.messages.lastOrNull()?.content, state.isSearchOpen) {
        if (state.messages.isNotEmpty() && !state.isSearchOpen) {
            val addedMessage = state.messages.size > previousMessageCount
            if (addedMessage || isNearBottom) {
                listState.animateScrollToItem(state.messages.lastIndex)
            }
        }
        previousMessageCount = state.messages.size
    }

    LaunchedEffect(state.searchScrollTargetIndex) {
        val target = state.searchScrollTargetIndex
        if (target >= 0) {
            listState.animateScrollToItem(target)
        }
    }

    val showScrollFab by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible < total - 2
        }
    }

    Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            when {
                state.messages.isEmpty() && state.isConnecting -> {
                    InitialLoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(loadingScrollState)
                            .padding(top = topPadding, bottom = bottomPadding),
                    )
                }
                state.messages.isEmpty() && state.error != null -> {
                    EmptyErrorState(
                        message = state.error.orEmpty(),
                        onRetry = viewModel::retryLast,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(emptyScrollState)
                            .padding(top = topPadding, bottom = bottomPadding),
                    )
                }
                state.messages.isEmpty() -> {
                    WelcomeEmptyState(
                        agentName = state.agentName,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(emptyScrollState)
                            .padding(top = topPadding, bottom = bottomPadding),
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = topPadding,
                            bottom = bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.pinnedMessageIds.isNotEmpty()) {
                            item {
                                PinnedStrip(
                                    messages = state.messages.filter { it.id in state.pinnedMessageIds },
                                )
                            }
                        }
                        items(state.messages, key = { it.id }) { message ->
                            if (!(message.role == "assistant" && message.isStreaming && message.content.isBlank())) {
                                MessageBubble(
                                    message = message,
                                    agentName = state.agentName,
                                    agentAvatarUri = state.agentAvatarUri,
                                    isPinned = message.id in state.pinnedMessageIds,
                                    isSearchMatch = message.id in state.matchedMessageIds,
                                    searchQuery = state.searchQuery,
                                    onRetry = viewModel::retryLast,
                                    onReply = { viewModel.replyTo(message.id) },
                                    onPin = { viewModel.togglePin(message.id) },
                                    onEdit = { viewModel.editMessage(message.id) },
                                    onDelete = { pendingDeleteMessage = message },
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                        fadeOutSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                                    ),
                                )
                            }
                        }
                        items(state.queuedPrompts, key = { it.id }) { queued ->
                            QueuedPromptRow(
                                prompt = queued,
                                onCancel = { viewModel.cancelQueuedPrompt(queued.id) },
                            )
                        }
                        item {
                            AnimatedVisibility(
                                visible = state.isConnecting || state.isStreaming,
                                enter = fadeIn() + slideInVertically { it / 2 },
                                exit = fadeOut() + slideOutVertically { it / 2 },
                            ) {
                                ThinkingIndicator(
                                    tools = state.tools,
                                    label = activityLabel(state),
                                    agentName = state.agentName,
                                    agentAvatarUri = state.agentAvatarUri,
                                )
                            }
                        }
                        item {
                            state.error?.let {
                                InlineError(message = it, onRetry = viewModel::retryLast)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (state.isSearchOpen) {
                    ChatSearchBar(
                        query = state.searchQuery,
                        matchCount = state.matchedMessageIds.size,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onClose = viewModel::closeSearch,
                        onNext = viewModel::scrollToNextMatch,
                    )
                } else {
                    ChatTopBar(
                        agentName = state.agentName,
                        agentAvatarUri = state.agentAvatarUri,
                        agents = state.agents,
                        sessionId = state.sessionId,
                        isConnecting = state.isConnecting,
                        isStreaming = state.isStreaming,
                        queuedCount = state.queuedPrompts.size,
                        connectionState = state.connectionState,
                        activity = activityLabel(state),
                        onHistoryClick = onHistoryClick,
                        onNewChat = viewModel::startNewChat,
                        onSwitchAgent = viewModel::switchAgent,
                        onSearchClick = viewModel::openSearch,
                        onExportMarkdown = { shareExport(context, viewModel.exportConversation(ExportFormat.Markdown), "md") },
                        onExportPlainText = { shareExport(context, viewModel.exportConversation(ExportFormat.PlainText), "txt") },
                        onExportJson = { shareExport(context, viewModel.exportConversation(ExportFormat.Json), "json") },
                    )
                }
            }

            AnimatedVisibility(
                visible = showScrollFab && state.messages.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = composerHeightDp + imeBottom + 14.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
            ) {
                ScrollToBottomFab(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    },
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Composer(
                    value = state.draft,
                    attachments = state.attachments,
                    isStreaming = state.isStreaming || state.isConnecting,
                    onValueChange = viewModel::onDraftChanged,
                    onSend = viewModel::sendCurrentDraft,
                    onStop = viewModel::stop,
                    onCamera = ::launchCamera,
                    onPhotos = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onFiles = { fileLauncher.launch(arrayOf("*/*")) },
                    onVoiceStart = ::beginRecording,
                    onVoiceEnd = ::finishRecording,
                    isRecording = isRecording,
                    recordingStartedAtMillis = recordingStartedAt,
                    onCommand = viewModel::insertCommand,
                    onRemoveAttachment = viewModel::removeAttachment,
                    replyTarget = state.replyTarget,
                    onClearReply = viewModel::clearReply,
                    agentName = state.agentName,
                    selectedModel = state.selectedModel,
                    modifier = Modifier.onGloballyPositioned { composerHeightPx = it.size.height },
                )
            }

            pendingDeleteMessage?.let { message ->
                val deletesExchange = message.role == "user"
                AlertDialog(
                    onDismissRequest = { pendingDeleteMessage = null },
                    title = { Text(if (deletesExchange) "Delete exchange?" else "Delete message?") },
                    text = {
                        Text(
                            if (deletesExchange) {
                                "This removes your message and the assistant reply that followed it."
                            } else {
                                "This removes this message from the conversation."
                            },
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteMessage(message.id)
                                pendingDeleteMessage = null
                            },
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteMessage = null }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    agentName: String,
    agentAvatarUri: String?,
    agents: List<AgentProfile>,
    sessionId: String?,
    isConnecting: Boolean,
    isStreaming: Boolean,
    queuedCount: Int,
    connectionState: ConnectionState,
    activity: String,
    onHistoryClick: (String?) -> Unit,
    onNewChat: () -> Unit,
    onSwitchAgent: (String) -> Unit,
    onSearchClick: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportPlainText: () -> Unit,
    onExportJson: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var agentPickerOpen by remember { mutableStateOf(false) }
    val status = chatStatusLabel(connectionState, isConnecting, isStreaming, queuedCount, activity)
    val statusColor = when {
        isConnecting || isStreaming -> MaterialTheme.colorScheme.primary
        queuedCount > 0 -> MaterialTheme.colorScheme.primary
        connectionState is ConnectionState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), RoundedCornerShape(32.dp))
            .padding(start = 14.dp, top = 11.dp, end = 10.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistantAvatar(agentName = agentName, active = isConnecting || isStreaming, avatarUri = agentAvatarUri)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
                .clickable { agentPickerOpen = true },
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    agentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (agents.size > 1) {
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Switch agent",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = statusColor, active = isConnecting || isStreaming || queuedCount > 0)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(
                expanded = agentPickerOpen,
                onDismissRequest = { agentPickerOpen = false },
                shape = RoundedCornerShape(22.dp),
            ) {
                agents.forEach { agent ->
                    val isCurrent = agent.name == agentName
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    agent.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(
                                    agent.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    agent.initial,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        trailingIcon = {
                            if (isCurrent) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Current agent",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        onClick = {
                            agentPickerOpen = false
                            if (!isCurrent) onSwitchAgent(agent.id)
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search in chat",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier
                .size(42.dp),
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "Chat options",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = RoundedCornerShape(22.dp),
        ) {
            DropdownMenuItem(
                text = { Text("New chat with $agentName") },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = "New chat") },
                onClick = {
                    onNewChat()
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("$agentName history") },
                leadingIcon = { Icon(Icons.Rounded.Menu, contentDescription = "Chat history") },
                onClick = {
                    onHistoryClick(agentIdFromChatSessionId(sessionId))
                    menuOpen = false
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Export as Markdown") },
                leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                onClick = {
                    onExportMarkdown()
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("Export as Plain Text") },
                leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                onClick = {
                    onExportPlainText()
                    menuOpen = false
                },
            )
            DropdownMenuItem(
                text = { Text("Export as JSON") },
                leadingIcon = { Icon(Icons.Rounded.Code, contentDescription = null) },
                onClick = {
                    onExportJson()
                    menuOpen = false
                },
            )
        }
    }
}

@Composable
private fun ChatSearchBar(
    query: String,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), RoundedCornerShape(32.dp))
            .padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search in chat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
        )
        if (query.isNotBlank()) {
            Text(
                if (matchCount > 0) "$matchCount" else "0",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Next match",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Close search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color, active: Boolean) {
    val scale by animateFloatAsState(if (active) 1.12f else 0.86f, label = "statusDotScale")
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color),
    )
}

internal fun activityLabel(state: ChatUiState): String {
    val latestTool = state.tools.lastOrNull().takeIf { state.isConnecting || state.isStreaming }
    return when {
        latestTool?.label?.isNotBlank() == true -> {
            val toolName = latestTool.tool?.takeIf { it.isNotBlank() } ?: latestTool.label
            getToolStatusLabel(toolName, latestTool.status)
        }
        state.queuedPrompts.isNotEmpty() && (state.isStreaming || state.isConnecting) -> "queued"
        state.isConnecting && state.messages.lastOrNull { it.role == "assistant" && it.isStreaming }?.content.isNullOrBlank() -> "thinking"
        state.isStreaming && state.messages.lastOrNull { it.role == "assistant" && it.isStreaming }?.content.isNullOrBlank() -> "thinking"
        state.isStreaming -> "typing"
        state.isConnecting -> "preparing"
        state.connectionState is ConnectionState.Error -> "connection issue"
        state.connectionState == ConnectionState.Connected -> "connected"
        else -> "ready"
    }
}

private fun getToolStatusLabel(toolName: String, status: String?): String {
    val cleanTool = toolName.lowercase().replace(Regex("[^a-z0-9_]"), "")
    val action = when {
        cleanTool.contains("web") || cleanTool.contains("search") || cleanTool.contains("browse") -> "searching web"
        cleanTool.contains("database") || cleanTool.contains("db") || cleanTool.contains("sql") -> "querying database"
        cleanTool.contains("code") || cleanTool.contains("exec") || cleanTool.contains("run") -> "running code"
        cleanTool.contains("file") || cleanTool.contains("read") || cleanTool.contains("write") -> "reading files"
        cleanTool.contains("reason") || cleanTool.contains("think") -> "reasoning"
        cleanTool.contains("plan") -> "planning"
        cleanTool.contains("memory") -> "using memory"
        cleanTool.contains("skill") -> "using skill"
        else -> "using ${toolName.replace('_', ' ').trim().ifBlank { "tool" }}"
    }
    val cleanStatus = status?.trim()?.takeIf { it.isNotBlank() }
    return cleanStatus?.let { "$action · $it" } ?: action
}

internal fun chatStatusLabel(
    connectionState: ConnectionState,
    isConnecting: Boolean,
    isStreaming: Boolean,
    queuedCount: Int,
    activity: String,
): String {
    return when {
        (isConnecting || isStreaming) && activity.isNotBlank() -> activity
        queuedCount > 0 -> "$queuedCount queued"
        connectionState is ConnectionState.Error -> "connection issue"
        connectionState == ConnectionState.Connected -> "connected"
        else -> "ready"
    }
}

@Composable
private fun InitialLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(22.dp),
                    containerAlpha = 0.68f,
                    borderAlpha = 0.14f,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InlineLottie(
                    resId = com.hermes.mobile.R.raw.loading_dots,
                    modifier = Modifier.size(64.dp, 22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Loading conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WelcomeEmptyState(
    agentName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = "$agentName assistant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Start chat with $agentName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Composer is ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Retry",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onRetry)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ScrollToBottomFab(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Scroll to latest",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PinnedStrip(messages: List<ChatUiMessage>) {
    val pinned = messages.firstOrNull() ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.PushPin, contentDescription = "Pinned message", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Pinned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(pinned.previewContent(), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatUiMessage,
    agentName: String,
    agentAvatarUri: String?,
    isPinned: Boolean,
    isSearchMatch: Boolean = false,
    searchQuery: String = "",
    onRetry: () -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outgoing = message.role == "user"
    if (!outgoing) {
        AssistantMessageRow(
            message = message,
            agentName = agentName,
            agentAvatarUri = agentAvatarUri,
            isPinned = isPinned,
            isSearchMatch = isSearchMatch,
            searchQuery = searchQuery,
            onRetry = onRetry,
            onReply = onReply,
            onPin = onPin,
            onDelete = onDelete,
            modifier = modifier,
        )
        return
    }
    val haptic = LocalHapticFeedback.current
    val alignment = Alignment.CenterEnd
    val palette = outgoingBubblePalette(MaterialTheme.colorScheme)
    val bubbleColor = palette.container
    val textColor = palette.content
    var menuOpen by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (message.isStreaming) 1.01f else 1f, label = "bubbleScale")
    val bubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(dragOffset, label = "replyDrag")
    val visibleContent = message.visibleContent()

    BoxWithConstraints(modifier.fillMaxWidth().animateContentSize(), contentAlignment = alignment) {
        val bubbleLaneWidth = (maxWidth - 56.dp).coerceAtLeast(160.dp)
        val maxBubbleWidth = chatBubbleMaxWidth(bubbleLaneWidth, isOutgoing = true)
        val searchBorderColor = MaterialTheme.colorScheme.tertiary
        val borderColor = if (isSearchMatch) searchBorderColor else palette.border
        val borderWidth = if (isSearchMatch) 2.dp else 1.dp
        Box {
            Column(
                modifier = Modifier
                    .widthIn(min = 40.dp, max = maxBubbleWidth)
                    .offset { IntOffset(animatedDrag.roundToInt(), 0) }
                    .scale(scale)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .border(borderWidth, borderColor, bubbleShape)
                    .pointerInput(message.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset < -72f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onReply()
                                }
                                dragOffset = 0f
                            },
                            onDragCancel = { dragOffset = 0f },
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceIn(-96f, 0f)
                        }
                    }
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        },
                    )
                    .padding(horizontal = 13.dp, vertical = 9.dp),
            )
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(Icons.Rounded.PushPin, contentDescription = "Pinned message", modifier = Modifier.size(14.dp), tint = textColor.copy(alpha = 0.72f))
                    }
                }
                if (message.replyTo != null) {
                    Text(
                        text = message.replyTo,
                        maxLines = 2,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.72f),
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(textColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                if (message.imageUris.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        message.imageUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Attached image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (message.imageUris.size == 1) 216.dp else 118.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                        }
                    }
                }
                ReceivedAttachments(
                    attachments = message.receivedAttachments,
                    toneColor = textColor,
                    modifier = Modifier.padding(bottom = if (visibleContent.isNotBlank()) 8.dp else 0.dp),
                )
                if (visibleContent.isNotBlank()) {
                    RichMessageText(
                        text = visibleContent,
                        color = textColor,
                        searchQuery = searchQuery,
                        fillWidth = false,
                    )
                }
                message.error?.let {
                    Text(
                        text = "$it  Retry",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.76f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable(onClick = onRetry)
                            .padding(top = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (outgoing && message.error == null) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Message sent",
                            tint = textColor.copy(alpha = 0.66f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        message.time,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor.copy(alpha = 0.72f),
                    )
                }
            }
            MessageActionsMenu(
                expanded = menuOpen,
                message = message,
                isPinned = isPinned,
                onDismiss = { menuOpen = false },
                onReply = onReply,
                onPin = onPin,
                onEdit = onEdit.takeIf { message.role == "user" },
                onDelete = onDelete,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssistantMessageRow(
    message: ChatUiMessage,
    agentName: String,
    agentAvatarUri: String?,
    isPinned: Boolean,
    isSearchMatch: Boolean = false,
    searchQuery: String = "",
    onRetry: () -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val textColor = MaterialTheme.colorScheme.onSurface
    var menuOpen by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(dragOffset, label = "assistantReplyDrag")
    val visibleContent = message.visibleContent()
    val searchBorderColor = MaterialTheme.colorScheme.tertiary

    Box(modifier.fillMaxWidth().animateContentSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedDrag.roundToInt(), 0) }
                .pointerInput(message.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 72f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReply()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount).coerceIn(0f, 96f)
                    }
                }
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .then(
                    if (isSearchMatch) {
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                            .border(1.dp, searchBorderColor.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(start = 4.dp, end = 22.dp, top = 10.dp, bottom = 12.dp),
        ) {
            if (message.replyTo != null) {
                Text(
                    text = message.replyTo,
                    maxLines = 2,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.64f),
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(textColor.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isPinned) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Pinned message",
                        modifier = Modifier.size(14.dp),
                        tint = textColor.copy(alpha = 0.56f),
                    )
                }
                if (message.isStreaming && visibleContent.isNotBlank()) {
                    Text(
                        "live",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                message.responseTime?.let { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f),
                    )
                }
            }
            if (isPinned || message.responseTime != null || (message.isStreaming && visibleContent.isNotBlank())) {
                Spacer(Modifier.height(8.dp))
            }
            ReceivedAttachments(
                attachments = message.receivedAttachments,
                toneColor = textColor,
                modifier = Modifier.padding(bottom = if (visibleContent.isNotBlank()) 8.dp else 0.dp),
            )
            if (visibleContent.isNotBlank()) {
                RichMessageText(visibleContent, textColor, searchQuery)
            }
            if (message.isStreaming && message.content.isBlank()) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineLottie(
                        resId = com.hermes.mobile.R.raw.typing_dots,
                        modifier = Modifier.size(32.dp, 14.dp),
                    )
                }
            }
            message.error?.let {
                Text(
                    text = "$it  Retry",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable(onClick = onRetry)
                        .padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    message.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.54f),
                )
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Message actions",
                        tint = textColor.copy(alpha = 0.58f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        MessageActionsMenu(
            expanded = menuOpen,
            message = message,
            isPinned = isPinned,
            onDismiss = { menuOpen = false },
            onReply = onReply,
            onPin = onPin,
            onEdit = null,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReceivedAttachments(
    attachments: List<ReceivedAttachment>,
    toneColor: Color,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    val context = LocalContext.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            ReceivedAttachmentCard(
                attachment = attachment,
                toneColor = toneColor,
                onOpen = { openReceivedAttachment(context, attachment) },
                onSave = { saveReceivedAttachment(context, attachment) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceivedAttachmentCard(
    attachment: ReceivedAttachment,
    toneColor: Color,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val label = attachment.label
    val icon = when (attachment.kind) {
        ReceivedAttachmentKind.Image -> Icons.Rounded.Image
        ReceivedAttachmentKind.Video -> Icons.Rounded.PlayArrow
        ReceivedAttachmentKind.File -> Icons.Rounded.Description
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(toneColor.copy(alpha = 0.07f))
        .border(1.dp, toneColor.copy(alpha = 0.12f), shape)
        .combinedClickable(onClick = onOpen)

    if (attachment.kind == ReceivedAttachmentKind.Image) {
        Box(
            modifier = cardModifier
                .height(184.dp),
        ) {
            AsyncImage(
                model = attachment.url,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AttachmentIconButton(icon = Icons.AutoMirrored.Rounded.OpenInNew, description = "Open image", onClick = onOpen)
                AttachmentIconButton(icon = Icons.Rounded.Download, description = "Save image", onClick = onSave)
            }
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        return
    }

    Row(
        modifier = cardModifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(toneColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = toneColor.copy(alpha = 0.86f), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                color = toneColor,
            )
            Text(
                when (attachment.kind) {
                    ReceivedAttachmentKind.Video -> "Video"
                    ReceivedAttachmentKind.File -> "File"
                    ReceivedAttachmentKind.Image -> "Image"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = toneColor.copy(alpha = 0.62f),
            )
        }
        AttachmentIconButton(icon = Icons.AutoMirrored.Rounded.OpenInNew, description = "Open attachment", onClick = onOpen)
        Spacer(Modifier.width(2.dp))
        AttachmentIconButton(icon = Icons.Rounded.Download, description = "Save attachment", onClick = onSave)
    }
}

@Composable
private fun AttachmentIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AssistantAvatar(agentName: String, active: Boolean, avatarUri: String? = null) {
    val avatarPlateColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(avatarPlateColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUri != null) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "$agentName profile",
                modifier = Modifier.size(31.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(id = com.hermes.mobile.R.drawable.hermes_mark),
                contentDescription = "$agentName profile",
                modifier = Modifier.size(31.dp),
                contentScale = ContentScale.Fit,
            )
        }
        if (active) {
            Dot(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(9.dp),
            )
        }
    }
}

@Composable
private fun MessageActionsMenu(
    expanded: Boolean,
    message: ChatUiMessage,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
    ) {
        DropdownMenuItem(
            text = { Text("Reply") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = "Reply") },
            onClick = {
                onReply()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text(if (isPinned) "Unpin" else "Pin") },
            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = if (isPinned) "Unpin" else "Pin") },
            onClick = {
                onPin()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy") },
            onClick = {
                clipboard.setText(AnnotatedString(message.readableContent()))
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "Share") },
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message.readableContent())
                }
                context.startActivity(Intent.createChooser(intent, "Share message"))
                onDismiss()
            },
        )
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = "Edit") },
                onClick = {
                    onEdit()
                    onDismiss()
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (message.role == "user") "Delete exchange" else "Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDelete()
                onDismiss()
            },
        )
    }
}

@Composable
private fun QueuedPromptRow(prompt: PendingPrompt, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.KeyboardCommandKey, contentDescription = "Queued command", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Queued", style = MaterialTheme.typography.labelLarge)
            Text(prompt.readableContent(), maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel queued message")
        }
    }
}

@Composable
private fun RichMessageText(
    text: String,
    color: Color,
    searchQuery: String = "",
    fillWidth: Boolean = true,
) {
    val parsed = remember(text, color) { parseMarkdown(text, color) }
    Column(modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier) {
        parsed.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> {
                    val highlighted = if (searchQuery.isNotBlank()) {
                        highlightSearchMatches(block.content, searchQuery, color)
                    } else {
                        block.content
                    }
                    LinkAwareText(
                        text = highlighted,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Medium,
                            color = color,
                        ),
                    )
                }
                is MarkdownBlock.CodeBlock -> CodeBlockView(block.language, block.content, color)
                is MarkdownBlock.Diagram -> DiagramCanvasView(block.type, block.content, color)
                is MarkdownBlock.Table -> TableView(block.headers, block.rows, color)
                is MarkdownBlock.Blockquote -> BlockquoteView(block.content, color)
                is MarkdownBlock.HorizontalRule -> HorizontalDivider(
                    color = color.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                is MarkdownBlock.TaskList -> TaskListView(block.items, color)
            }
            if (block !is MarkdownBlock.HorizontalRule) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LinkAwareText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(text) {
            detectTapGestures { position ->
                val offset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                val url = text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()
                    ?.item
                    ?: return@detectTapGestures
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }
            }
        },
    )
}

sealed class MarkdownBlock {
    data class Text(val content: AnnotatedString) : MarkdownBlock()
    data class CodeBlock(val language: String, val content: String) : MarkdownBlock()
    data class Diagram(val type: String, val content: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Blockquote(val content: AnnotatedString) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
    data class TaskList(val items: List<TaskItem>) : MarkdownBlock()
    data class TaskItem(val completed: Boolean, val content: AnnotatedString)
}

@Composable
private fun CodeBlockView(language: String, content: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(12.dp),
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        SelectionContainer {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp,
                ),
                color = color,
            )
        }
    }
}

@Composable
private fun DiagramCanvasView(type: String, content: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(
            type.ifBlank { "diagram" }.lowercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        SelectionContainer {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp,
                ),
                color = color,
            )
        }
    }
}

private data class DiagramNode(
    val id: String,
    val label: String,
    val shape: String = "rect",
)

private data class DiagramEdge(
    val from: String,
    val to: String,
    val label: String = "",
    val style: String = "solid",
)

private data class ParsedDiagram(
    val nodes: List<DiagramNode>,
    val edges: List<DiagramEdge>,
    val direction: String = "TD",
)

private fun parseDiagram(content: String): ParsedDiagram {
    val lines = content.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
    val direction = when {
        lines.firstOrNull()?.startsWith("graph ") == true -> lines.first().substringAfter("graph ").take(2)
        lines.firstOrNull()?.startsWith("flowchart ") == true -> lines.first().substringAfter("flowchart ").take(2)
        else -> "TD"
    }
    val nodes = mutableMapOf<String, DiagramNode>()
    val edges = mutableListOf<DiagramEdge>()

    for (line in lines) {
        if (line.startsWith("graph ") || line.startsWith("flowchart ") || line.startsWith("sequenceDiagram")) continue
        if (line.startsWith("classDef") || line.startsWith("class ") || line.startsWith("style ")) continue
        if (line.startsWith("%%") || line.startsWith("#")) continue

        val edgeMatch = Regex("([A-Za-z0-9_]+)\\s*(?:\\[([^\\]]+)]|\\{([^}]+)}|\\(([^)]+)\\)|\\(\\(([^)]+)\\)\\))?\\s*(?:--[-.]>(?:\\|([^|]*)\\|)?|==>[^|]*(?:\\|([^|]*)\\|)?|-\\.-[^|]*(?:\\|([^|]*)\\|)?)").find(line)
        if (edgeMatch != null) {
            val fromId = edgeMatch.groupValues[1]
            val toId = line.substringAfter("-->").substringAfter("==>").substringBefore("[").substringBefore("{").substringBefore("(").trim()
                .takeIf { it.isNotBlank() } ?: run {
                    val afterArrow = line.substringAfter("-->").substringAfter("==>")
                    Regex("[A-Za-z0-9_]+").find(afterArrow)?.value.orEmpty()
                }
            val label = edgeMatch.groupValues.drop(6).firstOrNull { it.isNotBlank() } ?: ""
            val nodeLabel = edgeMatch.groupValues.drop(2).firstOrNull { it.isNotBlank() } ?: fromId

            if (fromId !in nodes) {
                val shape = when {
                    line.contains("{$fromId") || line.contains("{$fromId") -> "diamond"
                    line.contains("($fromId") -> "circle"
                    line.contains("(($fromId)") -> "rounded"
                    else -> "rect"
                }
                nodes[fromId] = DiagramNode(fromId, nodeLabel, shape)
            }
            if (toId.isNotBlank() && toId !in nodes) {
                val toLabelMatch = Regex("$toId\\s*\\[([^\\]]+)\\]").find(line)
                val toLabel = toLabelMatch?.groupValues?.getOrNull(1) ?: toId
                nodes[toId] = DiagramNode(toId, toLabel, "rect")
            }
            if (toId.isNotBlank()) {
                val style = when {
                    line.contains("-->") -> "solid"
                    line.contains("==>") -> "thick"
                    line.contains("-.-") -> "dotted"
                    else -> "solid"
                }
                edges.add(DiagramEdge(fromId, toId, label, style))
            }
        } else {
            val nodeMatch = Regex("([A-Za-z0-9_]+)\\s*\\[([^\\]]+)\\]").find(line)
                ?: Regex("([A-Za-z0-9_]+)\\s*\\{([^}]+)\\}").find(line)
                ?: Regex("([A-Za-z0-9_]+)\\s*\\(([^)]+)\\)").find(line)
            if (nodeMatch != null) {
                val id = nodeMatch.groupValues[1]
                val label = nodeMatch.groupValues[2]
                val shape = when {
                    line.contains("{$id") -> "diamond"
                    line.contains("($id") && !line.contains("(($id)") -> "circle"
                    line.contains("(($id)") -> "rounded"
                    else -> "rect"
                }
                nodes[id] = DiagramNode(id, label, shape)
            }
        }
    }

    return ParsedDiagram(nodes.values.toList(), edges, direction)
}

private fun DrawScope.drawDiagram(diagram: ParsedDiagram, color: Color) {
    if (diagram.nodes.isEmpty()) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.parseColor("#80808080")
                textSize = 14.sp.toPx()
            }
            canvas.nativeCanvas.drawText("No diagram data", size.width / 2 - 60, size.height / 2, paint)
        }
        return
    }

    val isHorizontal = diagram.direction in listOf("LR", "RL")
    val nodeWidth = 120f
    val nodeHeight = 40f
    val padding = 30f

    val nodePositions = mutableMapOf<String, Offset>()

    val cols = if (isHorizontal) 1 else kotlin.math.ceil(kotlin.math.sqrt(diagram.nodes.size.toDouble())).toInt().coerceAtLeast(1)

    diagram.nodes.forEachIndexed { index, node ->
        val row = index / cols
        val col = index % cols
        val x = if (isHorizontal) {
            padding + index * (nodeWidth + padding)
        } else {
            padding + col * (nodeWidth + padding)
        }
        val y = if (isHorizontal) {
            padding
        } else {
            padding + row * (nodeHeight + padding * 2)
        }
        nodePositions[node.id] = Offset(x, y)
    }

    for (edge in diagram.edges) {
        val fromPos = nodePositions[edge.from] ?: continue
        val toPos = nodePositions[edge.to] ?: continue

        val startX = fromPos.x + nodeWidth / 2
        val startY = fromPos.y + nodeHeight
        val endX = toPos.x + nodeWidth / 2
        val endY = toPos.y

        val edgeColor = color.copy(alpha = 0.6f).toArgb()
        val path = android.graphics.Path()
        path.moveTo(startX, startY)
        val midY = (startY + endY) / 2
        path.cubicTo(startX, midY, endX, midY, endX, endY)

        val paint = android.graphics.Paint().apply {
            this.color = edgeColor
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = when (edge.style) {
                "dotted" -> 1.5f
                "thick" -> 3f
                else -> 1.5f
            }
            if (edge.style == "dotted") {
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
        }
        drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }

        val arrowSize = 8f
        val arrowPath = Path()
        arrowPath.moveTo(endX, endY)
        arrowPath.lineTo(endX - arrowSize / 2, endY - arrowSize)
        arrowPath.lineTo(endX + arrowSize / 2, endY - arrowSize)
        arrowPath.close()
        drawPath(arrowPath, color = color.copy(alpha = 0.6f))
    }

    val textPaint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = 12.sp.toPx()
        textAlign = android.graphics.Paint.Align.LEFT
    }

    for (node in diagram.nodes) {
        val pos = nodePositions[node.id] ?: continue
        val nodeColor = color.copy(alpha = 0.12f).toArgb()
        val borderColor = color.copy(alpha = 0.5f).toArgb()

        val shapePath = android.graphics.Path()
        when (node.shape) {
            "circle" -> {
                shapePath.addOval(
                    android.graphics.RectF(pos.x, pos.y, pos.x + nodeWidth, pos.y + nodeHeight),
                    android.graphics.Path.Direction.CW,
                )
            }
            "diamond" -> {
                val cx = pos.x + nodeWidth / 2
                val cy = pos.y + nodeHeight / 2
                shapePath.moveTo(cx, pos.y)
                shapePath.lineTo(pos.x + nodeWidth, cy)
                shapePath.lineTo(cx, pos.y + nodeHeight)
                shapePath.lineTo(pos.x, cy)
                shapePath.close()
            }
            "rounded" -> {
                shapePath.addRoundRect(
                    android.graphics.RectF(pos.x, pos.y, pos.x + nodeWidth, pos.y + nodeHeight),
                    20f, 20f, android.graphics.Path.Direction.CW
                )
            }
            else -> {
                shapePath.addRect(
                    android.graphics.RectF(pos.x, pos.y, pos.x + nodeWidth, pos.y + nodeHeight),
                    android.graphics.Path.Direction.CW
                )
            }
        }

        val fillPaint = android.graphics.Paint().apply {
            this.color = nodeColor
            style = android.graphics.Paint.Style.FILL
        }
        val borderPaint = android.graphics.Paint().apply {
            this.color = borderColor
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPath(shapePath, fillPaint)
            canvas.nativeCanvas.drawPath(shapePath, borderPaint)
            canvas.nativeCanvas.drawText(node.label.take(20), pos.x + 8, pos.y + nodeHeight / 2 + 4, textPaint)
        }
    }
}

@Composable
private fun TableView(headers: List<String>, rows: List<List<String>>, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.06f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            headers.forEachIndexed { index, header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.weight(1f).padding(end = if (index < headers.lastIndex) 8.dp else 0.dp),
                )
            }
        }
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                row.forEachIndexed { index, cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f).padding(end = if (index < row.lastIndex) 8.dp else 0.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockquoteView(content: AnnotatedString, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        LinkAwareText(
            text = content,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp, color = color.copy(alpha = 0.85f)),
        )
    }
}

@Composable
private fun TaskListView(items: List<MarkdownBlock.TaskItem>, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.completed) "☑" else "☐",
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                    modifier = Modifier.padding(end = 8.dp),
                )
                LinkAwareText(
                    text = item.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 23.sp,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                        color = if (item.completed) color.copy(alpha = 0.6f) else color,
                    ),
                )
            }
        }
    }
}

private fun parseMarkdown(text: String, color: Color): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        when {
            line.matches(Regex("^(`{3,}|~{3,})(\\w*)$")) -> {
                val fence = line.takeWhile { it == '`' || it == '~' }
                val lang = line.removePrefix(fence).trim()
                val contentLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(fence)) {
                    contentLines.add(lines[i])
                    i++
                }
                i++
                val content = contentLines.joinToString("\n")
                if (lang.lowercase() in setOf("mermaid", "diagram", "graph", "flowchart", "sequencediagram")) {
                    blocks.add(MarkdownBlock.Diagram(lang, content))
                } else {
                    blocks.add(MarkdownBlock.CodeBlock(lang, content))
                }
            }
            line.matches(Regex("^---+$")) || line.matches(Regex("^\\*\\*\\*+$")) || line.matches(Regex("^___+$")) -> {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
            }
            line.startsWith("> ") || line == ">" -> {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && (lines[i].startsWith("> ") || lines[i] == ">")) {
                    quoteLines.add(lines[i].removePrefix(">").removePrefix(" "))
                    i++
                }
                blocks.add(MarkdownBlock.Blockquote(parseInlineMarkdown(quoteLines.joinToString("\n"), color)))
            }
            line.matches(Regex("^\\|.*\\|$")) && i + 1 < lines.size && lines[i + 1].matches(Regex("^\\|[\\s:|-]+\\|$")) -> {
                val headers = line.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].matches(Regex("^\\|.*\\|$"))) {
                    rows.add(lines[i].removePrefix("|").removeSuffix("|").split("|").map { it.trim() })
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
            }
            line.matches(Regex("^- \\[[ xX]\\] .+")) -> {
                val items = mutableListOf<MarkdownBlock.TaskItem>()
                while (i < lines.size && lines[i].matches(Regex("^- \\[[ xX]\\] .+"))) {
                    val completed = lines[i].contains("- [x]") || lines[i].contains("- [X]")
                    val content = lines[i].substringAfter("] ").trim()
                    items.add(MarkdownBlock.TaskItem(completed, parseInlineMarkdown(content, color)))
                    i++
                }
                blocks.add(MarkdownBlock.TaskList(items))
            }
            line.isBlank() -> {
                i++
            }
            else -> {
                val textLines = mutableListOf<String>()
                while (i < lines.size && lines[i].isNotBlank() &&
                    !lines[i].matches(Regex("^(`{3,}|~{3,})(\\w*)$")) &&
                    !lines[i].matches(Regex("^---+$")) &&
                    !lines[i].matches(Regex("^\\*\\*\\*+$")) &&
                    !lines[i].matches(Regex("^___+$")) &&
                    !lines[i].startsWith("> ") &&
                    !lines[i].matches(Regex("^\\|.*\\|$")) &&
                    !lines[i].matches(Regex("^- \\[[ xX]\\] .+"))) {
                    textLines.add(lines[i])
                    i++
                }
                if (textLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Text(parseInlineMarkdown(textLines.joinToString("\n"), color)))
                }
            }
        }
    }
    return blocks
}

private fun parseInlineMarkdown(text: String, color: Color): AnnotatedString = buildAnnotatedString {
    var remaining = text
    val linkPattern = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    val boldPattern = Regex("\\*\\*([^*]+)\\*\\*")
    val italicPattern = Regex("\\*([^*]+)\\*")
    val codePattern = Regex("`([^`]+)`")
    val urlPattern = Regex("(https?://[^\\s]+)")

    while (remaining.isNotEmpty()) {
        val linkMatch = linkPattern.find(remaining)
        val boldMatch = boldPattern.find(remaining)
        val italicMatch = italicPattern.find(remaining)
        val codeMatch = codePattern.find(remaining)
        val urlMatch = urlPattern.find(remaining)

        val matches = listOfNotNull(linkMatch, boldMatch, italicMatch, codeMatch, urlMatch)
            .sortedBy { it.range.first }
            .distinctBy { it.range.first }

        if (matches.isEmpty()) {
            append(remaining)
            break
        }

        val first = matches.first()
        if (first.range.first > 0) {
            append(remaining.substring(0, first.range.first))
        }

        val start = length
        when {
            first == linkMatch -> {
                val linkText = first.groupValues[1]
                val url = first.groupValues[2]
                append(linkText)
                addStyle(SpanStyle(color = color.copy(alpha = 0.8f), textDecoration = TextDecoration.Underline), start, length)
                addStringAnnotation(tag = "URL", annotation = url, start = start, end = length)
            }
            first == codeMatch -> {
                append(first.groupValues[1])
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = color.copy(alpha = 0.12f)), start, length)
            }
            first == boldMatch -> {
                append(first.groupValues[1])
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
            }
            first == italicMatch -> {
                append(first.groupValues[1])
                addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, length)
            }
            first == urlMatch -> {
                append(first.value)
                addStyle(SpanStyle(color = color.copy(alpha = 0.8f), textDecoration = TextDecoration.Underline), start, length)
                addStringAnnotation(tag = "URL", annotation = first.value, start = start, end = length)
            }
        }
        remaining = remaining.substring(first.range.last + 1)
    }
}

@Composable
private fun ThinkingIndicator(
    tools: List<ToolProgress>,
    label: String,
    agentName: String,
    agentAvatarUri: String?,
) {
    val palette = assistantBubblePalette(MaterialTheme.colorScheme)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        val bubbleLaneWidth = (maxWidth - 52.dp).coerceAtLeast(160.dp)
        val maxBubbleWidth = chatBubbleMaxWidth(bubbleLaneWidth, isOutgoing = false)
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            AssistantAvatar(agentName = agentName, active = true, avatarUri = agentAvatarUri)
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .widthIn(min = 116.dp, max = maxBubbleWidth)
                    .background(palette.container, RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp))
                    .padding(14.dp)
                    .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 300f)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ThinkingDots()
                    Spacer(Modifier.width(10.dp))
                    Text(
                        label.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (tools.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        thickness = 0.5.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    tools.forEachIndexed { index, tool ->
                        ToolStep(
                            tool = tool,
                            isActive = index == tools.lastIndex,
                        )
                        if (index < tools.lastIndex) {
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots() {
    val transition = rememberInfiniteTransition(label = "thinkingDots")
    val colors = listOf(0, 200, 400).map { delay ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot$delay",
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha.value)),
            )
        }
    }
}

@Composable
private fun ToolStep(tool: ToolProgress, isActive: Boolean) {
    val toolName = tool.tool?.takeIf { it.isNotBlank() } ?: tool.label
    val statusText = tool.status?.takeIf { it.isNotBlank() }
    var expanded by remember(tool.label) { mutableStateOf(false) }
    val hasDetail = statusText != null && (statusText.length > 80 || statusText.lines().size > 2)
    val toolIcon = getToolIcon(toolName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (hasDetail) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(vertical = 3.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isActive) {
            val pulse by rememberInfiniteTransition(label = "toolPulse").animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "toolDotPulse",
            )
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = pulse)),
            )
        } else {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Completed",
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(12.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    toolIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    toolName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (statusText != null && (expanded || !hasDetail)) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 20.dp, top = 2.dp),
                )
            }
        }
        if (hasDetail) {
            Text(
                if (expanded) "Less" else "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

private fun getToolIcon(toolName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val clean = toolName.lowercase()
    return when {
        clean.contains("web") || clean.contains("search") || clean.contains("browse") -> Icons.Rounded.Language
        clean.contains("database") || clean.contains("db") || clean.contains("sql") -> Icons.Rounded.Storage
        clean.contains("code") || clean.contains("exec") || clean.contains("run") -> Icons.Rounded.Code
        clean.contains("file") || clean.contains("read") || clean.contains("write") -> Icons.Rounded.Description
        clean.contains("reason") || clean.contains("think") -> Icons.Rounded.Psychology
        clean.contains("plan") -> Icons.Rounded.Route
        clean.contains("memory") -> Icons.Rounded.Bookmarks
        clean.contains("skill") -> Icons.Rounded.Build
        clean.contains("image") || clean.contains("photo") -> Icons.Rounded.Image
        clean.contains("audio") || clean.contains("voice") -> Icons.Rounded.Mic
        clean.contains("network") || clean.contains("api") -> Icons.Rounded.Wifi
        else -> Icons.Rounded.AutoAwesome
    }
}

@Composable
private fun InlineLottie(resId: Int, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier,
    )
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "Retry",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onRetry)
                .padding(10.dp),
        )
    }
}


@Composable
private fun Composer(
    value: String,
    attachments: List<ChatAttachment>,
    isStreaming: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onCamera: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceEnd: () -> Unit,
    isRecording: Boolean,
    recordingStartedAtMillis: Long,
    onCommand: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    replyTarget: ChatUiMessage?,
    onClearReply: () -> Unit,
    agentName: String,
    selectedModel: ChatModelOption,
    modifier: Modifier = Modifier,
) {
    var pickerMenuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val mediaAttachments = attachments.filter { it.kind == "image" }
    val auxiliaryAttachments = attachments.filter { it.kind != "image" }
    val canSend = value.isNotBlank() || attachments.isNotEmpty()
    val hasComposerContentAbove = replyTarget != null ||
        isRecording ||
        mediaAttachments.isNotEmpty() ||
        auxiliaryAttachments.isNotEmpty()
    val actionBackground = if (canSend) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val actionTint = if (canSend) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = value.startsWith("/"),
            enter = fadeIn() + slideInVertically { it } + scaleIn(initialScale = 0.98f),
            exit = fadeOut() + slideOutVertically { it } + scaleOut(targetScale = 0.98f),
        ) {
            CommandBar(
                query = value,
                selectedModel = selectedModel,
                onCommand = onCommand,
            )
        }
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f), RoundedCornerShape(32.dp))
                    .padding(8.dp),
        ) {
            AnimatedVisibility(
                visible = replyTarget != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                replyTarget?.let { target ->
                    ReplyPreview(target, onClearReply)
                }
            }
            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                RecordingStrip(startedAtMillis = recordingStartedAtMillis)
            }
            AttachmentTray(
                mediaAttachments = mediaAttachments,
                auxiliaryAttachments = auxiliaryAttachments,
                onRemoveAttachment = onRemoveAttachment,
            )
            if (hasComposerContentAbove) {
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    HermesCircleButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pickerMenuOpen = true
                        },
                        size = 48.dp,
                        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        animated = true,
                        frosted = false,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Attach photo")
                    }
                    ChatMediaPicker(
                        expanded = pickerMenuOpen,
                        onDismiss = { pickerMenuOpen = false },
                        onCamera = onCamera,
                        onPhotos = onPhotos,
                        onFiles = onFiles,
                    )
                }
                MessageInputField(
                    value = value,
                    onValueChange = onValueChange,
                    agentName = agentName,
                    modifier = Modifier
                        .weight(1f)
                )
                AnimatedVisibility(visible = isStreaming) {
                    HermesCircleButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStop()
                        },
                        size = 48.dp,
                        background = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onError,
                        animated = true,
                        frosted = false,
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = "Interrupt")
                    }
                }
                if (isStreaming) Spacer(Modifier.width(5.dp))
                val sendScale by animateFloatAsState(if (isRecording) 0.92f else 1f, label = "voicePressScale")
                var sendPulse by remember { mutableStateOf(false) }
                val pulseScale by animateFloatAsState(
                    targetValue = if (sendPulse) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "sendPulse",
                    finishedListener = { sendPulse = false },
                )
                val actionDescription = if (canSend) "Send" else "Hold to record"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .scale(sendScale * pulseScale)
                        .clip(CircleShape)
                        .background(actionBackground)
                        .semantics {
                            role = Role.Button
                            contentDescription = actionDescription
                        }
                        .then(
                            if (canSend) {
                                Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sendPulse = true
                                    onSend()
                                }
                            } else {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onVoiceStart()
                                            tryAwaitRelease()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onVoiceEnd()
                                        },
                                    )
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (canSend) Icons.AutoMirrored.Rounded.Send else if (isRecording) Icons.Rounded.StopCircle else Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = actionTint,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    agentName: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val requestInputFocus = {
        focusRequester.requestFocus()
        keyboardController?.show()
        Unit
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp, max = 140.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = requestInputFocus,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = textFieldModifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "Message $agentName",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun AttachmentTray(
    mediaAttachments: List<ChatAttachment>,
    auxiliaryAttachments: List<ChatAttachment>,
    onRemoveAttachment: (String) -> Unit,
) {
    if (mediaAttachments.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
        ) {
            mediaAttachments.forEach { attachment ->
                MediaThumbnail(
                    uri = attachment.uri,
                    isVideo = false,
                    size = 74.dp,
                    onRemove = { onRemoveAttachment(attachment.id) },
                )
            }
        }
    }
    if (auxiliaryAttachments.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
        ) {
            auxiliaryAttachments.take(3).forEach { attachment ->
                AttachmentChip(attachment, onRemove = { onRemoveAttachment(attachment.id) })
            }
        }
    }
}

@Composable
private fun RecordingStrip(startedAtMillis: Long) {
    val transition = rememberInfiniteTransition(label = "recordingMotion")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(620),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recordPulseScale",
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(760),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recordWave",
    )
    var elapsedSeconds by remember(startedAtMillis) { mutableStateOf(0L) }
    LaunchedEffect(startedAtMillis) {
        while (startedAtMillis > 0L) {
            elapsedSeconds = ((System.currentTimeMillis() - startedAtMillis) / 1000L).coerceAtLeast(0L)
            delay(500L)
        }
    }
    val elapsed = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    val tone = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(tone.copy(alpha = 0.13f))
            .border(1.dp, tone.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(tone),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = "Recording voice", modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onError)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Recording voice",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = tone,
            )
            Text(
                "Release to attach",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RecordingWaveform(level = wave, color = tone, modifier = Modifier.size(58.dp, 28.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            elapsed,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = tone,
        )
    }
}

@Composable
private fun RecordingWaveform(level: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val bars = 7
        val gap = size.width / (bars * 2f)
        val stroke = gap.coerceAtLeast(3f)
        repeat(bars) { index ->
            val phase = ((index % 3) * 0.18f + level).coerceIn(0f, 1f)
            val heightFactor = 0.28f + (abs(sin((phase + index * 0.21f) * Math.PI).toFloat()) * 0.68f)
            val barHeight = size.height * heightFactor
            val x = gap + index * gap * 2f
            drawLine(
                color = color.copy(alpha = 0.88f),
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun ReplyPreview(target: ChatUiMessage, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = "Reply target", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Replying to ${target.role}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(target.previewContent(), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CommandBar(query: String, selectedModel: ChatModelOption, onCommand: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val commands = remember(query, selectedModel) {
        hermesCommandSuggestions(
            selectedModel = selectedModel.model,
            selectedLabel = selectedModel.label,
            query = query,
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .padding(bottom = 8.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(28.dp),
                containerAlpha = 0.82f,
                borderAlpha = 0.18f,
            ),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(commands) { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCommand(suggestion.command)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.KeyboardCommandKey,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            suggestion.command,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (suggestion.categoryId.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                suggestion.categoryId.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        suggestion.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: ChatAttachment, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Icon(
            imageVector = if (attachment.kind == "voice") Icons.Rounded.Mic else Icons.Rounded.AttachFile,
            contentDescription = if (attachment.kind == "voice") "Voice attachment" else "File attachment",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(attachment.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove attachment", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Dot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

private data class RecordingResult(
    val recorder: MediaRecorder,
    val file: File,
)

private fun startVoiceRecording(context: Context): RecordingResult {
    val file = createHermesMediaFile(context, kind = "voice", extension = "m4a")
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(96_000)
        setAudioSamplingRate(44_100)
        setOutputFile(file.absolutePath)
        prepare()
        start()
    }
    return RecordingResult(recorder, file)
}

private fun createCameraImageUri(context: Context): Uri {
    val file = createHermesMediaFile(context, kind = "photo", extension = "jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

internal fun createHermesMediaFile(
    context: Context,
    kind: String,
    extension: String,
    timestamp: Long = System.currentTimeMillis(),
): File {
    val cleanKind = kind.cleanMediaFileSegment(allowDash = true, fallback = "media")
    val cleanExtension = extension.cleanMediaExtension()
    val directory = File(context.filesDir, HERMES_MEDIA_DIRECTORY).apply { mkdirs() }
    return File(directory, "hermes-$cleanKind-$timestamp.$cleanExtension")
}

private fun String.cleanMediaExtension(): String {
    return lineSequence()
        .map { line ->
            line.trim()
                .trimStart('.')
                .takeWhile { it.isLetterOrDigit() }
        }
        .firstOrNull { it.isNotBlank() }
        ?: "bin"
}

private fun String.cleanMediaFileSegment(allowDash: Boolean, fallback: String): String {
    return lineSequence()
        .map { line ->
            line.filter { it.isLetterOrDigit() || (allowDash && it == '-') }
        }
        .firstOrNull { it.isNotBlank() }
        ?: fallback
}

private fun stopVoiceRecording(recorder: MediaRecorder?, file: File?): File? {
    if (recorder == null || file == null) return null
    return runCatching {
        recorder.stop()
        recorder.release()
        file.takeIf { it.exists() && it.length() > 0L }
    }.getOrNull()
}

private fun persistPickedPhotoAccess(context: Context, uri: Uri) {
    if (!shouldPersistPickedPhotoAccess(uri.toString())) return
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

internal fun shouldPersistPickedPhotoAccess(uri: String): Boolean {
    if (!uri.startsWith("content://", ignoreCase = true)) return false
    val authority = uri.substringAfter("://").substringBefore("/")
    return authority.isNotBlank() && !authority.lowercase().endsWith(".fileprovider")
}

private fun highlightSearchMatches(text: AnnotatedString, query: String, baseColor: Color): AnnotatedString {
    if (query.isBlank()) return text
    val originalText = text.text
    val lowerText = originalText.lowercase()
    val lowerQuery = query.lowercase()
    val builder = AnnotatedString.Builder()
    var lastIndex = 0
    var index = lowerText.indexOf(lowerQuery, startIndex = lastIndex)
    while (index >= 0) {
        if (index > lastIndex) {
            builder.append(text.substring(lastIndex, index))
        }
        val matchEnd = index + query.length
        builder.pushStyle(
            SpanStyle(
                background = baseColor.copy(alpha = 0.3f),
                color = baseColor,
                fontWeight = FontWeight.Bold,
            )
        )
        builder.append(text.substring(index, matchEnd))
        builder.pop()
        lastIndex = matchEnd
        index = lowerText.indexOf(lowerQuery, startIndex = lastIndex)
    }
    if (lastIndex < originalText.length) {
        builder.append(text.substring(lastIndex))
    }
    return builder.toAnnotatedString()
}

private fun shareExport(context: Context, content: String, extension: String) {
    val file = File(context.cacheDir, "hermes-export.$extension")
    file.writeText(content)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = when (extension) {
            "md" -> "text/markdown"
            "json" -> "application/json"
            else -> "text/plain"
        }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export conversation"))
}

private fun openReceivedAttachment(context: Context, attachment: ReceivedAttachment) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url)).apply {
        attachment.mimeType?.let { setDataAndType(Uri.parse(attachment.url), it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "No app can open this attachment", Toast.LENGTH_SHORT).show()
    }
}

private fun saveReceivedAttachment(context: Context, attachment: ReceivedAttachment) {
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (manager == null) {
        Toast.makeText(context, "Downloads are unavailable", Toast.LENGTH_SHORT).show()
        return
    }
    val request = DownloadManager.Request(Uri.parse(attachment.url)).apply {
        setTitle(attachment.label)
        setDescription("Saved from Hermes")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, attachment.safeDownloadFileName())
        attachment.mimeType?.let(::setMimeType)
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
    }
    runCatching {
        manager.enqueue(request)
    }.onSuccess {
        Toast.makeText(context, "Saving ${attachment.label}", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Could not save attachment", Toast.LENGTH_SHORT).show()
    }
}

private fun ReceivedAttachment.safeDownloadFileName(): String {
    val cleanLabel = label.lineSequence()
        .map { line -> line.filter { it.isLetterOrDigit() || it in safeDownloadFileNameChars }.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.take(96)
        ?: "hermes-attachment"
    val labelHasExtension = cleanLabel.substringAfterLast('.', "").let { it.length in 2..8 && it.all(Char::isLetterOrDigit) }
    if (labelHasExtension) return cleanLabel
    val extension = url.substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', "")
        .takeIf { it.length in 2..8 && it.all(Char::isLetterOrDigit) }
        ?: return cleanLabel
    return "$cleanLabel.$extension"
}

private const val HERMES_MEDIA_DIRECTORY = "hermes-media"
private val safeDownloadFileNameChars = setOf('.', '-', '_', ' ')
