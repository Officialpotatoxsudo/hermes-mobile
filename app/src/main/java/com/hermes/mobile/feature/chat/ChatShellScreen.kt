package com.hermes.mobile.feature.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.hermes.mobile.core.util.visibleMessageText
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardCommandKey
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

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
            }
            .onFailure {
                recorder = null
                recordingFile = null
                isRecording = false
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

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var composerHeightPx by remember { mutableStateOf(0) }
    val composerHeightDp = with(density) { composerHeightPx.toDp() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val topPadding = statusBarTop + 86.dp
    val bottomPadding = (composerHeightDp + imeBottom + 16.dp).coerceAtLeast(108.dp)
    val loadingScrollState = rememberScrollState()
    val emptyScrollState = rememberScrollState()

    LaunchedEffect(imeBottom, state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
                                    isPinned = message.id in state.pinnedMessageIds,
                                    onRetry = viewModel::retryLast,
                                    onReply = { viewModel.replyTo(message.id) },
                                    onPin = { viewModel.togglePin(message.id) },
                                    onEdit = { viewModel.editMessage(message.id) },
                                    onDelete = { viewModel.deleteMessage(message.id) },
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                ChatTopBar(
                    agentName = state.agentName,
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
                )
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
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .onGloballyPositioned { composerHeightPx = it.size.height },
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
                    onCommand = viewModel::insertCommand,
                    onRemoveAttachment = viewModel::removeAttachment,
                    replyTarget = state.replyTarget,
                    onClearReply = viewModel::clearReply,
                    agentName = state.agentName,
                    selectedModel = state.selectedModel,
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    agentName: String,
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistantAvatar(agentName = agentName, active = isConnecting || isStreaming)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
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
                        Spacer(Modifier.width(4.dp))
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
    val latestTool = state.tools.lastOrNull()
    return when {
        latestTool?.label?.isNotBlank() == true -> {
            val toolName = latestTool.tool?.takeIf { it.isNotBlank() } ?: latestTool.label
            "using $toolName"
        }
        state.queuedPrompts.isNotEmpty() && (state.isStreaming || state.isConnecting) -> "queued"
        state.isStreaming && state.messages.lastOrNull { it.role == "assistant" && it.isStreaming }?.content.isNullOrBlank() -> "thinking"
        state.isStreaming -> "typing"
        state.isConnecting -> "researching"
        state.connectionState is ConnectionState.Error -> "connection issue"
        state.connectionState == ConnectionState.Connected -> "connected"
        else -> "ready"
    }
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
        LoadingSkeletonBubble(width = 210, alignEnd = false)
        Spacer(Modifier.height(10.dp))
        LoadingSkeletonBubble(width = 170, alignEnd = true)
        Spacer(Modifier.height(12.dp))
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
private fun LoadingSkeletonBubble(width: Int, alignEnd: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(54.dp)
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(24.dp),
                    containerAlpha = 0.42f,
                    borderAlpha = 0.12f,
                ),
        )
    }
}

@Composable
private fun WelcomeEmptyState(
    agentName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(
                        colors = MaterialTheme.colorScheme,
                        shape = RoundedCornerShape(26.dp),
                        containerAlpha = 0.66f,
                        borderAlpha = 0.14f,
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = "$agentName assistant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Start chat with $agentName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "No messages yet.",
                        style = MaterialTheme.typography.bodyMedium,
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
            .size(42.dp)
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
    isPinned: Boolean,
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
            isPinned = isPinned,
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
    val bubbleColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onPrimary
    var menuOpen by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (message.isStreaming) 1.01f else 1f, label = "bubbleScale")
    val bubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 4.dp, bottomStart = 18.dp)
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(dragOffset, label = "replyDrag")
    val visibleContent = visibleMessageText(message.content, message.imageUris.size)

    BoxWithConstraints(modifier.fillMaxWidth().animateContentSize(), contentAlignment = alignment) {
        val maxBubbleWidth = maxWidth * 0.82f
        Box {
            Column(
                modifier = Modifier
                    .widthIn(min = 40.dp, max = maxBubbleWidth)
                    .offset { IntOffset(animatedDrag.roundToInt(), 0) }
                    .scale(scale)
                    .clip(bubbleShape)
                    .background(bubbleColor)
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
                    .padding(horizontal = 15.dp, vertical = 11.dp),
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
                        style = MaterialTheme.typography.bodyMedium,
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
                if (visibleContent.isNotBlank()) {
                    RichMessageText(visibleContent, textColor)
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
                        .padding(top = 6.dp),
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
	                        style = MaterialTheme.typography.bodyMedium,
	                        color = textColor.copy(alpha = 0.66f),
	                    )
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Message actions",
                            tint = textColor.copy(alpha = 0.66f),
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
    isPinned: Boolean,
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
    val cardShape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    val visibleContent = visibleMessageText(message.content, message.imageUris.size)

    Box(modifier.fillMaxWidth().animateContentSize()) {
        Row(
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
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AssistantAvatar(agentName = agentName, active = message.isStreaming)
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp, end = 4.dp)
                    .shadow(elevation = 2.dp, shape = cardShape)
                    .background(MaterialTheme.colorScheme.surface, cardShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = agentName,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.88f),
                    )
                    if (message.isStreaming) {
                        Spacer(Modifier.width(8.dp))
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
                    if (isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = "Pinned message",
                            modifier = Modifier.size(14.dp),
                            tint = textColor.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    message.responseTime?.let { time ->
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f),
                        )
                    }
                }
                if (message.replyTo != null) {
                    Text(
                        text = message.replyTo,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.64f),
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(textColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (visibleContent.isNotBlank()) {
                    RichMessageText(visibleContent, textColor)
                }
                if (message.isStreaming && message.content.isBlank()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
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
                            .align(Alignment.End)
                            .clickable(onClick = onRetry)
                            .padding(top = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        message.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.56f),
                    )
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Message actions",
                            tint = textColor.copy(alpha = 0.58f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
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

@Composable
private fun AssistantAvatar(agentName: String, active: Boolean) {
    Box(
        modifier = Modifier.size(34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = com.hermes.mobile.R.drawable.hermes_mark),
            contentDescription = "$agentName profile",
            modifier = Modifier.size(30.dp),
            contentScale = ContentScale.Fit,
        )
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
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
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
        IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel queued message")
        }
    }
}

@Composable
private fun RichMessageText(text: String, color: Color) {
    Text(
        text = remember(text, color) { markdownLite(text, color) },
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
        color = color,
    )
}

private fun markdownLite(text: String, color: Color): AnnotatedString = buildAnnotatedString {
    text.lineSequence().forEachIndexed { lineIndex, rawLine ->
        if (lineIndex > 0) append("\n")
        val line = when {
            rawLine.startsWith("### ") -> rawLine.removePrefix("### ")
            rawLine.startsWith("## ") -> rawLine.removePrefix("## ")
            rawLine.startsWith("# ") -> rawLine.removePrefix("# ")
            rawLine.startsWith("- ") -> "• ${rawLine.removePrefix("- ")}"
            rawLine.startsWith("* ") -> "• ${rawLine.removePrefix("* ")}"
            else -> rawLine
        }
        val heading = rawLine.startsWith("#")
        val baseStart = length
        appendInlineMarkdown(line, color)
        if (heading) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), baseStart, length)
        }
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(line: String, color: Color) {
    var index = 0
    val pattern = Regex("""(\*\*[^*]+\*\*|`[^`]+`)""")
    pattern.findAll(line).forEach { match ->
        append(line.substring(index, match.range.first))
        val token = match.value
        val start = length
        val clean = token.trim('*', '`')
        append(clean)
        val style = if (token.startsWith("`")) {
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = color.copy(alpha = 0.12f),
                color = color,
            )
        } else {
            SpanStyle(fontWeight = FontWeight.Bold)
        }
        addStyle(style, start, length)
        index = match.range.last + 1
    }
    append(line.substring(index))
}

@Composable
private fun ThinkingIndicator(
    tools: List<ToolProgress>,
    label: String,
    agentName: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AssistantAvatar(agentName = agentName, active = true)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
                    containerAlpha = 0.68f,
                    borderAlpha = 0.16f,
                )
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
            Text(
                toolName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (statusText != null && (expanded || !hasDetail)) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
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
    onCommand: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    replyTarget: ChatUiMessage?,
    onClearReply: () -> Unit,
    agentName: String,
    selectedModel: ChatModelOption,
) {
    var pickerMenuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val mediaAttachments = attachments.filter { it.kind == "image" }
    val auxiliaryAttachments = attachments.filter { it.kind != "image" }
    val canSend = value.isNotBlank() || attachments.isNotEmpty()
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
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
                .background(MaterialTheme.colorScheme.surface)
                .drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                .padding(10.dp),
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
                RecordingStrip()
            }
            AttachmentTray(
                mediaAttachments = mediaAttachments,
                auxiliaryAttachments = auxiliaryAttachments,
                onRemoveAttachment = onRemoveAttachment,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 4.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box {
                    HermesCircleButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pickerMenuOpen = true
                        },
                        size = 44.dp,
                        background = MaterialTheme.colorScheme.primaryContainer,
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
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 132.dp)
                        .padding(horizontal = 8.dp, vertical = 11.dp),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text("Message $agentName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    },
                )
                AnimatedVisibility(visible = isStreaming) {
                    HermesCircleButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStop()
                        },
                        size = 44.dp,
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
                        .size(46.dp)
                        .scale(sendScale * pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
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
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
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
private fun RecordingStrip() {
    val pulse by rememberInfiniteTransition(label = "recordPulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(620),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recordPulseScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = "Recording voice", modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Recording voice", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Release to send", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        InlineLottie(resId = com.hermes.mobile.R.raw.typing_dots, modifier = Modifier.size(44.dp, 22.dp))
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
        IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
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

private const val HERMES_MEDIA_DIRECTORY = "hermes-media"
