package com.hermes.mobile.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.feature.sessions.SessionListViewModel
import com.hermes.mobile.ui.components.frostedGlass
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    onAgentControl: () -> Unit,
    onSettings: () -> Unit,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var addAgentOpen by remember { mutableStateOf(false) }
    var agentName by remember { mutableStateOf("") }
    var agentSubtitle by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeNavigationDrawer(
                sessions = state.sessions,
                agents = state.agents,
                onNewChat = {
                    scope.launch { drawerState.close() }
                    onNewChat()
                },
                onSessionClick = { id ->
                    scope.launch { drawerState.close() }
                    onSessionClick(id)
                },
                onAgentControl = {
                    scope.launch { drawerState.close() }
                    onAgentControl()
                },
                onSettings = {
                    scope.launch { drawerState.close() }
                    onSettings()
                },
                onAddAgent = {
                    scope.launch { drawerState.close() }
                    addAgentOpen = true
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
            ) {
                HomeTopBar(
                    onMenu = { scope.launch { drawerState.open() } },
                    onAgentControl = onAgentControl,
                    onSettings = onSettings,
                    onAddAgent = { addAgentOpen = !addAgentOpen },
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    placeholder = { Text("Search chats") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
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
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AnimatedVisibility(
                        visible = addAgentOpen,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 },
                    ) {
                        AddAgentPanel(
                            name = agentName,
                            subtitle = agentSubtitle,
                            onNameChange = { agentName = it },
                            onSubtitleChange = { agentSubtitle = it },
                            onCancel = {
                                addAgentOpen = false
                                agentName = ""
                                agentSubtitle = ""
                            },
                            onSave = {
                                viewModel.addAgent(agentName, agentSubtitle)
                                addAgentOpen = false
                                agentName = ""
                                agentSubtitle = ""
                            },
                        )
                    }
                }
                item {
                    AgentGroup(
                        agent = state.agents.first(),
                        sessions = state.sessions,
                        isSyncing = state.isSyncing,
                        onAgentClick = onNewChat,
                        onSessionClick = onSessionClick,
                    )
                }
                items(state.agents.drop(1), key = { it.id }) { agent ->
                    AgentOnlyRow(agent = agent, onClick = onNewChat)
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onMenu: () -> Unit,
    onAgentControl: () -> Unit,
    onSettings: () -> Unit,
    onAddAgent: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(onClick = onMenu) { Icon(Icons.Rounded.Menu, contentDescription = "Menu") }
        Spacer(Modifier.width(10.dp))
        Text(
            "Hermes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(onClick = onAddAgent) { Icon(Icons.Rounded.Add, contentDescription = "Add agent") }
        Spacer(Modifier.width(8.dp))
        CircleIconButton(onClick = onAgentControl) { Icon(Icons.Rounded.Memory, contentDescription = "Agent") }
        Spacer(Modifier.width(8.dp))
        CircleIconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    }
}

@Composable
private fun HomeNavigationDrawer(
    sessions: List<SessionEntity>,
    agents: List<AgentProfile>,
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    onAgentControl: () -> Unit,
    onSettings: () -> Unit,
    onAddAgent: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(max = 330.dp)
            .fillMaxSize(),
        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Hermes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    AgentAvatar(agents.firstOrNull()?.initial ?: "H", active = true, size = 42)
                }
            }
            item { DrawerActionRow(Icons.Rounded.ChatBubble, "Chat", "Continue with Hermes", selected = true, onClick = onNewChat) }
            item { DrawerActionRow(Icons.Rounded.Add, "Add agent", "Create another chat target", onClick = onAddAgent) }
            item { DrawerActionRow(Icons.Rounded.Memory, "Agent control", "Memory, skills, tools, schedules", onClick = onAgentControl) }
            item { DrawerActionRow(Icons.Rounded.Terminal, "Native actions", "Runs and desktop tool execution", onClick = onAgentControl) }
            item { DrawerActionRow(Icons.Rounded.Settings, "Settings", "Server profiles and app security", onClick = onSettings) }
            item { DrawerSectionTitle("Agents") }
            items(agents, key = { it.id }) { agent ->
                DrawerAgentRow(agent = agent, onClick = onNewChat)
            }
            item { DrawerSectionTitle("Recents") }
            if (sessions.isEmpty()) {
                item {
                    Text(
                        "No synced chats yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(sessions.take(18), key = { it.id }) { session ->
                    DrawerSessionRow(session = session, onClick = { onSessionClick(session.id) })
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun DrawerActionRow(icon: ImageVector, title: String, subtitle: String, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DrawerAgentRow(agent: AgentProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentAvatar(agent.initial, active = false, size = 34)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(agent.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(agent.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DrawerSessionRow(session: SessionEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                session.title?.takeIf { it.isNotBlank() } ?: "Hermes chat",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                "${session.messageCount} messages · ${formatTimestamp(session.startedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AgentGroup(
    agent: AgentProfile,
    sessions: List<SessionEntity>,
    isSyncing: Boolean,
    onAgentClick: () -> Unit,
    onSessionClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChatRowShell(onClick = onAgentClick) {
            AgentAvatar(agent.initial, active = true)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(agent.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (isSyncing) "Syncing history" else "${sessions.size} chats in history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sessions.isEmpty()) {
            EmptyHistoryState()
        } else {
            sessions.take(8).forEach { session ->
                AgentSessionRow(session, onClick = { onSessionClick(session.id) })
            }
        }
    }
}

@Composable
private fun AgentOnlyRow(agent: AgentProfile, onClick: () -> Unit) {
    ChatRowShell(onClick = onClick) {
        AgentAvatar(agent.initial, active = false)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(agent.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(agent.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text("Chat", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AgentSessionRow(session: SessionEntity, onClick: () -> Unit) {
    ChatRowShell(onClick = onClick, indent = true) {
        AgentAvatar("H", active = false, size = 42)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.title?.takeIf { it.isNotBlank() } ?: "Hermes chat",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                "${session.messageCount} messages · ${session.model.ifBlank { session.source }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
private fun ChatRowShell(onClick: () -> Unit, indent: Boolean = false, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 44.dp else 14.dp, end = 14.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(28.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.18f,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun AgentAvatar(label: String, active: Boolean, size: Int = 56) {
    Box {
        Box(
            modifier = Modifier
                .size(size.dp)
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
                    .size(16.dp)
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
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No history yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Open Hermes Agent. New messages stay under this agent.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AddAgentPanel(
    name: String,
    subtitle: String,
    onNameChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(28.dp),
                containerAlpha = 0.78f,
                borderAlpha = 0.18f,
            )
            .padding(16.dp),
    ) {
        Text("Add agent", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = subtitle,
            onValueChange = onSubtitleChange,
            label = { Text("Role") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
            Text(
                "Cancel",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
            Text(
                "Add",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(enabled = name.isNotBlank(), onClick = onSave)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = CircleShape,
                containerAlpha = 0.72f,
                borderAlpha = 0.18f,
            ),
    ) {
        content()
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
    return DateTimeFormatter.ofPattern("MMM d")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
}
