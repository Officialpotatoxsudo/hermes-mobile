package com.hermes.mobile.feature.sessions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hermes.mobile.R
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.util.formatMessageCount
import com.hermes.mobile.core.util.legacyImageUrisFromText
import com.hermes.mobile.core.util.messageImageUrisFromJson
import com.hermes.mobile.core.util.visibleMessageText
import com.hermes.mobile.ui.components.HermesHeader
import com.hermes.mobile.ui.components.frostedGlass
import kotlinx.coroutines.delay

@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val continueSessionId = historyContinueSessionId(state.sessionId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(14.dp),
    ) {
        HermesHeader(
            title = "History",
            subtitle = historyHeaderSubtitle(state.sessionId),
            trailingAction = "Back",
            onTrailingAction = onBack,
        )
        if (continueSessionId != null) {
            Text(
                "Continue chat",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onContinue(continueSessionId) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        Text(
            historyCountLabel(state.isSyncing, state.messages.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.isSyncing && state.messages.isEmpty()) {
                items(5) { index -> HistorySkeleton(index) }
            } else if (state.messages.isEmpty()) {
                item { EmptyHistoryState() }
            } else {
                itemsIndexed(state.messages, key = { _, it -> it.id }) { index, message ->
                    val animAlpha = remember { Animatable(0f) }
                    val animOffset = remember { Animatable(20f) }
                    LaunchedEffect(message.id) {
                        delay(index * 30L)
                        animAlpha.animateTo(1f, animationSpec = spring(stiffness = 300f))
                        animOffset.animateTo(0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                    }
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = animAlpha.value
                            translationY = animOffset.value
                        },
                    ) {
                        HistoryBubble(message)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HistoryBubble(message: MessageEntity) {
    val outgoing = message.role == "user"
    val imageUris = remember(message.imageUrisJson, message.content) {
        messageImageUrisFromJson(message.imageUrisJson)
            .ifEmpty { legacyImageUrisFromText(message.content) }
    }
    val text = historyMessageText(message.content, imageUris.size)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(22.dp),
                    containerAlpha = if (outgoing) 0.85f else 0.65f,
                    borderAlpha = 0.14f,
                )
                .then(
                    if (outgoing) Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(22.dp),
                    ) else Modifier
                )
                .padding(14.dp),
        ) {
            if (imageUris.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    imageUris.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (imageUris.size == 1) 172.dp else 96.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                }
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = if (outgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

internal fun historyMessageText(content: String, imageCount: Int): String {
    return visibleMessageText(content, imageCount)
}

internal fun historyHeaderSubtitle(sessionId: String): String? {
    return historyContinueSessionId(sessionId)?.let { "Saved conversation" }
}

internal fun canContinueHistory(sessionId: String): Boolean {
    return historyContinueSessionId(sessionId) != null
}

internal fun historyCountLabel(isSyncing: Boolean, messageCount: Int): String {
    return if (isSyncing) "Syncing messages" else formatMessageCount(messageCount, qualifier = "cached")
}

internal fun historyContinueSessionId(sessionId: String): String? {
    return sessionId.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}

@Composable
private fun EmptyHistoryState() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.no_sessions))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever, isPlaying = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(24.dp),
                containerAlpha = 0.55f,
                borderAlpha = 0.12f,
            )
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LottieAnimation(composition, { progress }, modifier = Modifier.size(100.dp).padding(bottom = 12.dp))
        Text("No cached messages", style = MaterialTheme.typography.titleMedium)
        Text(
            "Continue chat to load or start the conversation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun HistorySkeleton(index: Int) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (index % 2 == 0) 0.72f else 0.58f)
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = RoundedCornerShape(18.dp),
                    containerAlpha = 0.42f,
                    borderAlpha = 0.12f,
                )
                .padding(12.dp),
        ) {
            Box(Modifier.width((150 + index * 10).dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
            if (index % 2 == 0) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .width(96.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)),
                )
            }
        }
    }
}
