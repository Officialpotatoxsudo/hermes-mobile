package com.hermes.mobile.feature.sessions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hermes.mobile.R
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.util.formatTimestamp
import com.hermes.mobile.ui.components.HermesHeader
import com.hermes.mobile.ui.components.HermesSearchField
import com.hermes.mobile.ui.components.frostedGlass
import kotlinx.coroutines.delay

@Composable
fun SessionListScreen(
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        HermesHeader(title = "Sessions", trailingAction = "Library", onTrailingAction = onBack)
        HermesSearchField(
            query = state.query,
            onQueryChange = viewModel::onQueryChanged,
            placeholder = "Search chats",
            showLeadingIcon = false,
        )
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
        }
        Text(
            if (state.isSyncing) "Syncing from Hermes..." else "${state.sessions.size} cached sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 4.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.isSyncing && state.sessions.isEmpty()) {
                items(4) { index -> SessionSkeleton(index) }
            } else if (state.sessions.isEmpty()) {
                item { EmptySearchState() }
            } else {
                itemsIndexed(state.sessions, key = { _, it -> it.id }) { index, session ->
                    val animAlpha = remember { Animatable(0f) }
                    val animOffset = remember { Animatable(24f) }
                    LaunchedEffect(session.id) {
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
                        SessionRow(session, onClick = { onSessionClick(session.id) })
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun EmptySearchState() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_search))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever, isPlaying = true)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LottieAnimation(composition, { progress }, modifier = Modifier.size(100.dp).padding(bottom = 12.dp))
        Text("No sessions found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SessionRow(session: SessionEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(24.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.16f,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sessionDisplayTitle(session.title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (session.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                ),
                modifier = Modifier.weight(1f),
            )
            if (session.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        session.unreadCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            session.lastMessagePreview?.takeIf { it.isNotBlank() }
                ?: sessionMetadataLine(session.messageCount, session.model, session.source),
            style = MaterialTheme.typography.bodyMedium,
            color = if (session.unreadCount > 0) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            formatTimestamp(session.startedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun SessionSkeleton(index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(24.dp),
                containerAlpha = 0.42f,
                borderAlpha = 0.12f,
            )
            .padding(16.dp),
    ) {
        Box(Modifier.width((180 - index * 18).dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width((120 + index * 12).dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
    }
}

private fun formatTimestamp(timestamp: Long): String = com.hermes.mobile.core.util.formatTimestamp(timestamp, "MMM d, HH:mm")
