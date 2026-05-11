package com.hermes.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.feature.sessions.SessionListViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    onAgentControl: () -> Unit,
    onSendPayment: () -> Unit,
    onSettings: () -> Unit,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
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
        HomeTopBar(
            onAgentControl = onAgentControl,
            onSendPayment = onSendPayment,
            onSettings = onSettings,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            placeholder = { Text("Search chats") },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                LiveAgentRow(
                    isSyncing = state.isSyncing,
                    onClick = onNewChat,
                )
            }
            if (state.sessions.isEmpty()) {
                item {
                    EmptyChatState(onClick = onNewChat)
                }
            } else {
                items(state.sessions, key = { it.id }) { session ->
                    AgentSessionRow(session, onClick = { onSessionClick(session.id) })
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HomeTopBar(
    onAgentControl: () -> Unit,
    onSendPayment: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Hermes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(onClick = onAgentControl) { Icon(Icons.Default.Memory, contentDescription = "Agent") }
        Spacer(Modifier.width(8.dp))
        CircleIconButton(onClick = onSendPayment) { Icon(Icons.Default.Payment, contentDescription = "Pay") }
        Spacer(Modifier.width(8.dp))
        CircleIconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    }
}

@Composable
private fun LiveAgentRow(isSyncing: Boolean, onClick: () -> Unit) {
    ChatRowShell(onClick = onClick) {
        AgentAvatar("H", active = true)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hermes Agent", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isSyncing) "Syncing chats" else "Start live agent chat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AgentSessionRow(session: SessionEntity, onClick: () -> Unit) {
    ChatRowShell(onClick = onClick) {
        AgentAvatar("H", active = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.title?.takeIf { it.isNotBlank() } ?: "Hermes chat",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${session.messageCount} messages · ${session.model.ifBlank { session.source }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatTimestamp(session.startedAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatRowShell(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.64f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun AgentAvatar(label: String, active: Boolean) {
    Box {
        Box(
            modifier = Modifier
                .size(52.dp)
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
            Text(
                label,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun EmptyChatState(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No synced chats", style = MaterialTheme.typography.titleMedium)
        Text(
            "Start chat, then synced sessions appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "New chat",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f), CircleShape),
    ) {
        content()
    }
}

private fun formatTimestamp(seconds: Long): String {
    return DateTimeFormatter.ofPattern("MMM d")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(seconds))
}
