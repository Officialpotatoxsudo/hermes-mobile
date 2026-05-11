package com.hermes.mobile.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.model.ToolProgress
import com.hermes.mobile.core.model.TokenUsage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun ChatShellScreen(
    onSessionsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSendPaymentClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 92.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SessionStrip(state.sessionId, state.isConnecting, state.isStreaming) }
                items(state.tools) { ToolChip(it) }
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message, onRetry = viewModel::retryLast)
                }
                item {
                    AnimatedVisibility(visible = state.isConnecting) {
                        TypingRow("Connecting")
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
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                        RoundedCornerShape(28.dp),
                    )
            ) {
                ChatTopBar(
                    onSessionsClick = onSessionsClick,
                    onSettingsClick = onSettingsClick,
                    onSendPaymentClick = onSendPaymentClick,
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
                    isStreaming = state.isStreaming || state.isConnecting,
                    onValueChange = viewModel::onDraftChanged,
                    onSend = viewModel::sendCurrentDraft,
                    onStop = viewModel::stop,
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    onSessionsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSendPaymentClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
            Text("private agent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSessionsClick) {
            Icon(Icons.Default.History, contentDescription = "Sessions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSendPaymentClick) {
            Icon(Icons.Default.Payment, contentDescription = "Send", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionStrip(sessionId: String?, isConnecting: Boolean, isStreaming: Boolean) {
    val status = when {
        isConnecting -> "opening secure stream"
        isStreaming -> "streaming response"
        sessionId != null -> "session $sessionId"
        else -> "new chat"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Live chat", style = MaterialTheme.typography.titleMedium)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatUiMessage, onRetry: () -> Unit) {
    val outgoing = message.role == "user"
    val alignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val bubbleShape = if (outgoing) {
        RoundedCornerShape(22.dp, 6.dp, 22.dp, 22.dp)
    } else {
        RoundedCornerShape(6.dp, 22.dp, 22.dp, 22.dp)
    }

    Box(Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .clip(bubbleShape)
                .background(bubbleColor.copy(alpha = if (outgoing) 1f else 0.72f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!outgoing) {
                Text("Hermes", style = MaterialTheme.typography.labelLarge, color = textColor)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = message.content + if (message.isStreaming) " |" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            message.usage?.let { UsageLine(it, textColor) }
            message.error?.let {
                Text(
                    text = "$it  Retry",
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor.copy(alpha = 0.76f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = onRetry)
                        .padding(top = 6.dp),
                )
            }
            Text(
                message.time,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.66f),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun UsageLine(usage: TokenUsage, color: Color) {
    Text(
        text = "${usage.promptTokens} in / ${usage.completionTokens} out / ${usage.totalTokens} total",
        style = MaterialTheme.typography.bodyMedium,
        color = color.copy(alpha = 0.66f),
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun ToolChip(progress: ToolProgress) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot()
        Spacer(Modifier.width(8.dp))
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
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(com.hermes.mobile.R.raw.typing_dots))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(48.dp, 24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "Retry",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onRetry)
                .padding(8.dp),
        )
    }
}

@Composable
private fun Composer(
    value: String,
    isStreaming: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f), RoundedCornerShape(28.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = !isStreaming,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(if (isStreaming) "Hermes is responding" else "Message Hermes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            },
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = if (isStreaming) onStop else onSend,
            enabled = isStreaming || value.isNotBlank(),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isStreaming || value.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
        ) {
            Icon(
                imageVector = if (isStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isStreaming) "Stop" else "Send",
                tint = if (isStreaming || value.isNotBlank()) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}
