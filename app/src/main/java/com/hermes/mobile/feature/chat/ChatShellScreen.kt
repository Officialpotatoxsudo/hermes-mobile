package com.hermes.mobile.feature.chat

import android.Manifest
import android.content.Context
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.TokenUsage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardCommandKey
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val result = startVoiceRecording(context)
            recorder = result.recorder
            recordingFile = result.file
            isRecording = true
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    fun beginRecording() {
        if (isRecording) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val result = startVoiceRecording(context)
            recorder = result.recorder
            recordingFile = result.file
            isRecording = true
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

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .imeNestedScroll(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 128.dp, bottom = 108.dp),
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(32.dp),
                    )
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("H", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Hermes", style = MaterialTheme.typography.titleMedium)
            Text(
                "$status · ${selectedModel.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(onClick = onSessionsClick) {
            Icon(Icons.Rounded.History, contentDescription = "Sessions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val alignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    var menuOpen by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (message.isStreaming) 1.015f else 1f, label = "bubbleScale")
    val bubbleShape = if (outgoing) {
        RoundedCornerShape(26.dp, 8.dp, 26.dp, 26.dp)
    } else {
        RoundedCornerShape(8.dp, 26.dp, 26.dp, 26.dp)
    }
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedDrag by animateFloatAsState(dragOffset, label = "replyDrag")

    BoxWithConstraints(Modifier.fillMaxWidth().animateContentSize(), contentAlignment = alignment) {
        val maxBubbleWidth = maxWidth * 0.86f
        Box {
            Column(
                modifier = Modifier
                    .widthIn(min = 92.dp, max = maxBubbleWidth)
                    .offset { IntOffset(animatedDrag.roundToInt(), 0) }
                    .scale(scale)
                    .clip(bubbleShape)
                    .background(bubbleColor.copy(alpha = if (outgoing) 1f else 0.88f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), bubbleShape)
                    .pointerInput(message.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragOffset > 72f) onReply()
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
                        onLongClick = { menuOpen = true },
                    )
                    .padding(horizontal = 15.dp, vertical = 11.dp),
            )
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!outgoing) {
                        Text("Hermes", style = MaterialTheme.typography.labelLarge, color = textColor)
                    }
                    if (isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(14.dp), tint = textColor.copy(alpha = 0.72f))
                    }
                    Spacer(Modifier.weight(1f))
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
                RichMessageText(message.content + if (message.isStreaming) " |" else "", textColor)
                message.usage?.let { UsageLine(it, textColor) }
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
                    if (outgoing) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = textColor.copy(alpha = 0.66f), modifier = Modifier.size(14.dp))
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
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
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
private fun UsageLine(usage: TokenUsage, color: Color) {
    Text(
        text = "${usage.promptTokens} in / ${usage.completionTokens} out / ${usage.totalTokens} total",
        style = MaterialTheme.typography.bodyMedium,
        color = color.copy(alpha = 0.66f),
        modifier = Modifier.padding(top = 8.dp),
    )
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot()
        Spacer(Modifier.width(10.dp))
        Text(progress.label, style = MaterialTheme.typography.bodyMedium)
        progress.status?.let {
            Text("  $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TypingRow(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.hermes.mobile.R.raw.typing_dots))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(48.dp, 24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        AnimatedVisibility(
            visible = commandMenuOpen || value.startsWith("/"),
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            CommandBar(
                selectedModel = selectedModel,
                onCommand = {
                    onCommand(it)
                    commandMenuOpen = false
                },
            )
        }
        AnimatedVisibility(
            visible = replyTarget != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            replyTarget?.let { target ->
                ReplyPreview(target, onClearReply)
            }
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
            IconButton(
                onClick = { commandMenuOpen = !commandMenuOpen },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
            ) {
                Icon(
                    if (commandMenuOpen) Icons.Rounded.Close else Icons.Rounded.Menu,
                    contentDescription = "Commands",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.54f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                },
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onAttach,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = isStreaming) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.9f)),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = "Interrupt", tint = MaterialTheme.colorScheme.onError)
                }
            }
            if (isStreaming) Spacer(Modifier.width(6.dp))
            val canSend = value.isNotBlank() || attachments.isNotEmpty()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .then(
                        if (canSend) {
                            Modifier.clickable(onClick = onSend)
                        } else {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onVoiceStart()
                                        tryAwaitRelease()
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
    val commands = listOf(
        "/new" to "Start fresh synced session",
        "/retry" to "Resend last message",
        "/undo" to "Remove last exchange",
        "/status" to "Agent status",
        "/tools" to "List tools",
        "/skills" to "List skills",
        "/memory" to "Open memory",
        "/persona" to "Agent persona",
        "/compact" to "Compact context",
        "/clear" to "Clear local draft",
        "/help" to "Command help",
        "/model ${selectedModel.model}" to "Use ${selectedModel.label}",
        "/web" to "Web action",
        "/image" to "Image action",
        "/file" to "File action",
        "/shell" to "Shell action",
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 230.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), RoundedCornerShape(22.dp)),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(commands) { (command, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCommand(command) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(command, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(command.substringBefore(" "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
