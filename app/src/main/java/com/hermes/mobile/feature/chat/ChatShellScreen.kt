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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.HermesFeatureActionKind
import com.hermes.mobile.core.model.hermesFeatureCatalog
import com.hermes.mobile.ui.components.frostedGlass
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.material.icons.rounded.Settings
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
    onSessionsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addAttachment(uri = it.toString(), label = it.lastPathSegment ?: "Attachment") }
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
    val topPadding = statusBarTop + 64.dp
    val bottomPadding = (composerHeightDp + imeBottom + 16.dp).coerceAtLeast(108.dp)

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
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            when {
                state.messages.isEmpty() && state.isConnecting -> {
                    InitialLoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topPadding, bottom = bottomPadding),
                    )
                }
                state.messages.isEmpty() -> {
                    WelcomeEmptyState(
                        onSuggestion = { suggestion ->
                            viewModel.onDraftChanged(suggestion)
                        },
                        modifier = Modifier
                            .fillMaxSize()
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
                        items(state.tools) { ToolChip(it) }
                        items(state.messages, key = { it.id }) { message ->
                            if (!(message.role == "assistant" && message.isStreaming && message.content.isBlank())) {
                                MessageBubble(
                                    message = message,
                                    isPinned = message.id in state.pinnedMessageIds,
                                    onRetry = viewModel::retryLast,
                                    onReply = { viewModel.replyTo(message.id) },
                                    onPin = { viewModel.togglePin(message.id) },
                                    onEdit = { viewModel.editMessage(message.id) },
                                    onDelete = { viewModel.deleteMessage(message.id) },
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
                                TypingRow(activityLabel(state))
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
                    sessionId = state.sessionId,
                    isConnecting = state.isConnecting,
                    isStreaming = state.isStreaming,
                    queuedCount = state.queuedPrompts.size,
                    selectedModel = state.selectedModel,
                    activity = activityLabel(state),
                    onSessionsClick = onSessionsClick,
                    onSettingsClick = onSettingsClick,
                )
            }

            AnimatedVisibility(
                visible = showScrollFab && state.messages.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = composerHeightDp + 14.dp),
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
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
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
                    onAttach = { attachmentLauncher.launch(arrayOf("*/*")) },
                    onVoiceStart = ::beginRecording,
                    onVoiceEnd = ::finishRecording,
                    isRecording = isRecording,
                    onCommand = viewModel::insertCommand,
                    onRemoveAttachment = viewModel::removeAttachment,
                    replyTarget = state.replyTarget,
                    onClearReply = viewModel::clearReply,
                    selectedModel = state.selectedModel,
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    sessionId: String?,
    isConnecting: Boolean,
    isStreaming: Boolean,
    queuedCount: Int,
    selectedModel: ChatModelOption,
    activity: String,
    onSessionsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val status = when {
        isConnecting || isStreaming -> activity
        queuedCount > 0 -> "$queuedCount queued"
        sessionId != null -> "session ${sessionId.takeLast(6)}"
        else -> "private agent"
    }
    val statusColor = when {
        isConnecting || isStreaming -> MaterialTheme.colorScheme.primary
        queuedCount > 0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressableCircleButton(
            onClick = onSessionsClick,
            modifier = Modifier.size(46.dp),
            background = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Menu",
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Hermes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isConnecting || isStreaming) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1,
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSessionsClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Sessions",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun activityLabel(state: ChatUiState): String {
    val latestTool = state.tools.lastOrNull()
    return when {
        latestTool?.label?.isNotBlank() == true -> {
            val status = latestTool.status?.takeIf { it.isNotBlank() }
            listOfNotNull("using ${latestTool.label}", status).joinToString(" · ")
        }
        state.isStreaming && state.messages.lastOrNull()?.content.isNullOrBlank() -> "thinking"
        state.isStreaming -> "typing"
        state.isConnecting -> "researching"
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
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = remember {
        listOf(
            "Summarize today's top news" to "Quick brief",
            "Help me draft a polite follow-up email" to "Email assist",
            "Explain a complex concept simply" to "Teach me",
            "Brainstorm ideas for a weekend project" to "Brainstorm",
        )
    }
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "How can I help today?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Ask anything, request a summary, or pick a starting point below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            suggestions.forEach { (prompt, label) ->
                SuggestionCard(
                    label = label,
                    prompt = prompt,
                    onClick = { onSuggestion(prompt) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(label: String, prompt: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(20.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.16f,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            Icons.AutoMirrored.Rounded.Send,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
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
        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Pinned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(pinned.content.ifBlank { "Streaming..." }, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatUiMessage,
    isPinned: Boolean,
    onRetry: () -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val outgoing = message.role == "user"
    if (!outgoing) {
        AssistantMessageRow(
            message = message,
            isPinned = isPinned,
            onRetry = onRetry,
            onReply = onReply,
            onPin = onPin,
            onDelete = onDelete,
        )
        return
    }
    val haptic = LocalHapticFeedback.current
    val alignment = Alignment.CenterEnd
    val bubbleColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    var menuOpen by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (message.isStreaming) 1.01f else 1f, label = "bubbleScale")
    val bubbleShape = RoundedCornerShape(20.dp, 6.dp, 20.dp, 20.dp)
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(dragOffset, label = "replyDrag")

    BoxWithConstraints(Modifier.fillMaxWidth().animateContentSize(), contentAlignment = alignment) {
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
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(14.dp), tint = textColor.copy(alpha = 0.72f))
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
                RichMessageText(message.content, textColor)
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
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (outgoing && message.error == null) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
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
    isPinned: Boolean,
    onRetry: () -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val textColor = MaterialTheme.colorScheme.onSurface
    var menuOpen by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(dragOffset, label = "assistantReplyDrag")

    Box(Modifier.fillMaxWidth().animateContentSize()) {
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
            AssistantAvatar(active = message.isStreaming)
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp, end = 4.dp)
                    .frostedGlass(
                        colors = MaterialTheme.colorScheme,
                        shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 8.dp),
                        containerAlpha = 0.62f,
                        borderAlpha = 0.22f,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Hermes",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.88f),
                    )
                    if (isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = null,
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
                Spacer(Modifier.height(2.dp))
                RichMessageText(message.content, textColor)
                if (message.isStreaming && message.content.isBlank()) {
                    InlineLottie(
                        resId = com.hermes.mobile.R.raw.typing_dots,
                        modifier = Modifier.size(60.dp, 22.dp),
                    )
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
                Text(
                    message.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.56f),
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
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
            onEdit = null,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun AssistantAvatar(active: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            InlineLottie(
                resId = com.hermes.mobile.R.raw.typing_dots,
                modifier = Modifier.size(30.dp, 16.dp),
            )
        } else {
            Image(
                painter = painterResource(id = com.hermes.mobile.R.drawable.hermes_logo),
                contentDescription = "Hermes",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
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
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null) },
            onClick = {
                onReply()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text(if (isPinned) "Unpin" else "Pin") },
            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
            onClick = {
                onPin()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            onClick = {
                clipboard.setText(AnnotatedString(message.content))
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message.content)
                }
                context.startActivity(Intent.createChooser(intent, "Share message"))
                onDismiss()
            },
        )
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    onEdit()
                    onDismiss()
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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
        Icon(Icons.Rounded.KeyboardCommandKey, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Queued", style = MaterialTheme.typography.labelLarge)
            Text(prompt.displayText, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ToolChip(progress: ToolProgress) {
    var expanded by remember(progress.label, progress.status) { mutableStateOf(false) }
    val detail = listOfNotNull(progress.label, progress.status).joinToString("\n")
    val isLong = detail.length > 120 || detail.lines().size > 3
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable(enabled = isLong) { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                progress.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            progress.status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isLong) {
            Text(
                if (expanded) "Less" else "More",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TypingRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistantAvatar(active = true)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(20.dp),
                    containerAlpha = 0.68f,
                    borderAlpha = 0.16f,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InlineLottie(
                    resId = com.hermes.mobile.R.raw.typing_dots,
                    modifier = Modifier.size(46.dp, 18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
private fun PressableCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color,
    contentColor: Color,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "pressScale")
    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
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
    onAttach: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceEnd: () -> Unit,
    isRecording: Boolean,
    onCommand: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    replyTarget: ChatUiMessage?,
    onClearReply: () -> Unit,
    selectedModel: ChatModelOption,
) {
    var commandMenuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = commandMenuOpen || value.startsWith("/"),
            enter = fadeIn() + slideInVertically { it } + scaleIn(initialScale = 0.98f),
            exit = fadeOut() + slideOutVertically { it } + scaleOut(targetScale = 0.98f),
        ) {
            CommandBar(
                selectedModel = selectedModel,
                onCommand = {
                    onCommand(it)
                    commandMenuOpen = false
                },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(28.dp),
                    containerAlpha = 0.72f,
                    borderAlpha = 0.2f,
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
        if (attachments.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                attachments.take(3).forEach { attachment ->
                    AttachmentChip(attachment, onRemove = { onRemoveAttachment(attachment.id) })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PressableCircleButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    commandMenuOpen = !commandMenuOpen
                },
                modifier = Modifier
                    .size(42.dp),
                background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Icon(
                    if (commandMenuOpen) Icons.Rounded.Close else Icons.Rounded.Menu,
                    contentDescription = "Commands",
                )
            }
            Spacer(Modifier.width(6.dp))
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
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.48f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                },
            )
            Spacer(Modifier.width(6.dp))
            PressableCircleButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAttach()
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                background = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Attach")
            }
            AnimatedVisibility(visible = isStreaming) {
                PressableCircleButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStop()
                    },
                    modifier = Modifier
                        .size(42.dp),
                    background = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = "Interrupt")
                }
            }
            if (isStreaming) Spacer(Modifier.width(6.dp))
            val canSend = value.isNotBlank() || attachments.isNotEmpty()
            val sendScale by animateFloatAsState(if (isRecording) 0.92f else 1f, label = "voicePressScale")
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(sendScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .then(
                        if (canSend) {
                            Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    contentDescription = if (canSend) "Send" else "Hold to record",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
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
            Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onPrimary)
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
        Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Replying to ${target.role}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(target.content.ifBlank { "Streaming..." }, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CommandBar(selectedModel: ChatModelOption, onCommand: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val commands = remember(selectedModel) {
        val catalogCommands = hermesFeatureCatalog
            .first { it.id == "chat" }
            .actions
            .filter { it.kind == HermesFeatureActionKind.Command }
            .map { it.target to it.subtitle }
        catalogCommands + ("/model ${selectedModel.model}" to "Use ${selectedModel.label}")
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 230.dp)
            .padding(bottom = 8.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(28.dp),
                containerAlpha = 0.82f,
                borderAlpha = 0.18f,
            ),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(commands) { (command, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCommand(command)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(commandGuiHint(command), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun commandGuiHint(command: String): String {
    return when {
        command.startsWith("/model ") -> "Switch model for this conversation"
        command == "/new" || command == "/reset" -> "Start clean session"
        command == "/retry" -> "Retry last message"
        command == "/undo" -> "Remove last exchange"
        command == "/stop" -> "Stop active response"
        command == "/compress" -> "Compact long context"
        else -> "Run Hermes action"
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
            contentDescription = null,
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
private fun Dot() {
    Box(
        modifier = Modifier
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
    val file = File(context.cacheDir, "hermes-voice-${System.currentTimeMillis()}.m4a")
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

private fun stopVoiceRecording(recorder: MediaRecorder?, file: File?): File? {
    if (recorder == null || file == null) return null
    return runCatching {
        recorder.stop()
        recorder.release()
        file.takeIf { it.exists() && it.length() > 0L }
    }.getOrNull()
}
