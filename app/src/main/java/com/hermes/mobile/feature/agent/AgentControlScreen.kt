package com.hermes.mobile.feature.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.ui.components.HermesChip
import com.hermes.mobile.ui.components.hermesGlass

@Composable
fun AgentControlScreen(
    onBack: () -> Unit,
    viewModel: AgentControlViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ControlsHeader(onBack = onBack)
        }
        items(state.actions, key = { it.id }) { action ->
            ControlActionCard(
                action = action,
                selected = action.id == state.selectedAction?.id,
                loading = state.isLoading && action.id == state.selectedAction?.id,
                onClick = { viewModel.selectAction(action) },
            )
        }
        item {
            ControlResultPanel(
                selectedAction = state.selectedAction,
                resultText = state.resultText,
                error = state.error,
                isLoading = state.isLoading,
                onRefresh = viewModel::refreshSelected,
            )
        }
    }
}

@Composable
private fun ControlsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Controls",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Server status and capabilities",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ControlActionCard(
    action: AgentControlAction,
    selected: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember(action.id) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f),
        label = "controlCardScale",
    )
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.Action,
                normalContainerAlpha = if (selected) 0.78f else 0.58f,
                normalBorderAlpha = if (selected) 0.34f else 0.18f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.18f else 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(action),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 10.dp),
        ) {
            Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                action.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedVisibility(visible = loading, enter = fadeIn(), exit = fadeOut()) {
            Text(
                "Checking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ControlResultPanel(
    selectedAction: AgentControlAction?,
    resultText: String,
    error: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    val title = selectedAction?.title ?: "Status"
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.ReadablePanel,
                normalContainerAlpha = 0.72f,
                normalBorderAlpha = 0.2f,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    error != null -> Icons.Rounded.ErrorOutline
                    resultText.isNotBlank() -> Icons.Rounded.CloudDone
                    else -> Icons.Rounded.Refresh
                },
                contentDescription = null,
                tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (isLoading) "Checking server..." else "Tap any card to refresh its status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HermesChip(
                text = "Refresh",
                selected = true,
                onClick = onRefresh,
                enabled = !isLoading && selectedAction != null,
            )
        }
        Spacer(Modifier.height(14.dp))
        when {
            error != null -> Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            resultText.isNotBlank() -> RenderedControlResultView(renderControlResult(resultText))
            isLoading -> Text("Loading...", style = MaterialTheme.typography.bodyMedium)
            else -> Text("No status loaded yet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RenderedControlResultView(result: RenderedControlResult) {
    when (result) {
        is RenderedControlResult.Text -> Text(
            text = result.text.compactControlResult(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        is RenderedControlResult.Structured -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.fields.forEach { field ->
                    ControlResultField(field)
                }
            }
        }
    }
}

@Composable
private fun ControlResultField(field: RenderedControlField) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            field.key,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            field.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun iconFor(action: AgentControlAction): ImageVector {
    return when (action.id) {
        "server.health" -> Icons.Rounded.Wifi
        "server.capabilities" -> Icons.Rounded.Memory
        "server.models" -> Icons.Rounded.Storage
        else -> Icons.Rounded.CloudDone
    }
}

private fun String.compactControlResult(): String {
    val trimmed = trim()
    if (trimmed.length <= MAX_RESULT_CHARS) return trimmed
    return trimmed.take(MAX_RESULT_CHARS).trimEnd() + "\n..."
}

private const val MAX_RESULT_CHARS = 1_600

