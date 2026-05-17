package com.hermes.mobile.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import coil3.compose.AsyncImage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.util.agentIdFromChatSessionId
import com.hermes.mobile.core.util.formatMessageCount
import com.hermes.mobile.core.util.formatTimestamp
import com.hermes.mobile.feature.sessions.SessionListViewModel
import com.hermes.mobile.ui.components.frostedGlass
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onAgentClick: (AgentProfile, String?) -> Unit,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rows = remember(state.sessions, state.agents) {
        conversationInboxRows(state.sessions, state.agents)
    }
    val defaultAgent = state.agents.firstOrNull() ?: AppPreferences.defaultAgents.first()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background),
        ) {
            HomeTopBar(
                onStartChat = { onAgentClick(defaultAgent, null) },
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (rows.isEmpty()) {
                item {
                    EmptyConversationState(onStartChat = { onAgentClick(defaultAgent, null) })
                }
            } else {
                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(rows, key = { _, it -> it.sessionId }) { index, row ->
                    val agent = state.agents.firstOrNull { it.id == row.agentId } ?: defaultAgent
                    val animAlpha = remember { Animatable(0f) }
                    val animOffset = remember { Animatable(30f) }
                    LaunchedEffect(row.sessionId) {
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
                        ConversationRow(
                            row = row,
                            onClick = { onAgentClick(agent, row.sessionId) },
                        )
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onStartChat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Chats",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Conversations and agents",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f))
                    .clickable(onClick = onStartChat)
                    .padding(start = 14.dp, end = 18.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "New",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(50.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Search conversations",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

data class ConversationRowModel(
    val sessionId: String,
    val agentId: String,
    val title: String,
    val agentName: String,
    val preview: String,
    val timestamp: String,
    val agentInitial: String,
    val usesDefaultAvatar: Boolean,
    val avatarUri: String? = null,
    val liveState: String = "none",
    val unreadCount: Int = 0,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(row: ConversationRowModel, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "cardScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.44f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentAvatar(
            label = row.agentInitial,
            active = row.liveState != "none",
            size = 46,
            usesDefaultAvatar = row.usesDefaultAvatar,
            avatarUri = row.avatarUri,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = if (row.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.unreadCount > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                row.timestamp,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.unreadCount > 0) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        row.unreadCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState(onStartChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f), RoundedCornerShape(30.dp))
                .clickable(onClick = onStartChat)
                .padding(start = 12.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Start a conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Pick an agent and keep the thread here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AgentListRow(
    agent: AgentProfile,
    session: SessionEntity?,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(22.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.14f,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentAvatar(
            label = agent.initial,
            active = session != null,
            size = 54,
            usesDefaultAvatar = agent.id == AppPreferences.defaultAgents.first().id,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                agent.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                agentRowSubtitle(agent, session),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (onEdit != null || onDelete != null) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Manage ${agent.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    onEdit?.let {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                it()
                            },
                        )
                    }
                    onDelete?.let {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                it()
                            },
                        )
                    }
                }
            }
        } else {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun latestSessionsByAgent(sessions: List<SessionEntity>): Map<String, SessionEntity> {
    val byAgent = linkedMapOf<String, SessionEntity>()
    sessions.forEach { session ->
        val agentId = agentIdFromChatSessionId(session.id) ?: return@forEach
        val current = byAgent[agentId]
        if (current == null || session.localLastActivityAt > current.localLastActivityAt) {
            byAgent[agentId] = session
        }
    }
    return byAgent
}

internal fun conversationInboxRows(
    sessions: List<SessionEntity>,
    agents: List<AgentProfile>,
): List<ConversationRowModel> {
    val defaultAgent = agents.firstOrNull() ?: AppPreferences.defaultAgents.first()
    val agentsById = agents.associateBy { it.id }
    return sessions
        .sortedWith(
            compareByDescending<SessionEntity> { it.localLastActivityAt }
                .thenByDescending { it.startedAt },
        )
        .map { session ->
            val agentId = agentIdFromChatSessionId(session.id) ?: defaultAgent.id
            val agent = agentsById[agentId] ?: defaultAgent
            ConversationRowModel(
                sessionId = session.id,
                agentId = agent.id,
                title = session.title?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()?.takeIf { it.isNotBlank() }
                    ?: agent.name,
                agentName = agent.name,
                preview = session.lastMessagePreview?.takeIf { it.isNotBlank() }
                    ?: formatMessageCount(session.messageCount),
                timestamp = formatTimestamp(session.localLastActivityAt),
                agentInitial = agent.initial,
                usesDefaultAvatar = agent.id == AppPreferences.defaultAgents.first().id,
                avatarUri = agent.avatarUri?.takeIf { it.isNotBlank() },
                liveState = "none",
                unreadCount = session.unreadCount,
            )
        }
}

internal fun agentRowSubtitle(agent: AgentProfile, session: SessionEntity?): String {
    return session
        ?.let {
            val preview = it.lastMessagePreview?.takeIf { p -> p.isNotBlank() }
            if (preview != null) {
                preview.take(60) + if (preview.length > 60) "..." else ""
            } else {
                "${formatMessageCount(it.messageCount)} · ${formatTimestamp(it.localLastActivityAt)}"
            }
        }
        ?: agent.subtitle.cleanAgentSubtitleLine()
}

private fun String.cleanAgentSubtitleLine(): String {
    val subtitle = lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?: "Custom Hermes agent"
    return if (subtitle.length <= MAX_AGENT_SUBTITLE_LENGTH) {
        subtitle
    } else {
        val head = subtitle.take(MAX_AGENT_SUBTITLE_LENGTH - 3).trimEnd()
        val boundary = head.lastIndexOf(' ').takeIf { it >= MIN_AGENT_SUBTITLE_WORD_BOUNDARY }
        (boundary?.let { head.take(it) } ?: head) + "..."
    }
}

private const val MAX_AGENT_SUBTITLE_LENGTH = 48
private const val MIN_AGENT_SUBTITLE_WORD_BOUNDARY = 24

@Composable
private fun AgentAvatar(
    label: String,
    active: Boolean,
    size: Int = 56,
    usesDefaultAvatar: Boolean = false,
    avatarUri: String? = null,
) {
    val avatarPlateColor = if (usesDefaultAvatar && isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(avatarPlateColor)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Agent avatar",
                    modifier = Modifier.size((size * 0.72f).dp),
                    contentScale = ContentScale.Crop,
                )
            } else if (usesDefaultAvatar) {
                Image(
                    painter = painterResource(id = com.hermes.mobile.R.drawable.hermes_mark),
                    contentDescription = "Default agent avatar",
                    modifier = Modifier.size((size * 0.72f).dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
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
private fun AgentFormPanel(
    title: String,
    saveLabel: String,
    name: String,
    subtitle: String,
    onNameChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val canSave = name.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(22.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.14f,
            )
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
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
                saveLabel,
                color = if (canSave) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = canSave, onClick = onSave)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun DeleteAgentPanel(
    agent: AgentProfile,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(22.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.14f,
            )
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Text("Delete ${agent.name}?", style = MaterialTheme.typography.titleMedium)
        Text(
            "Chat history stays. Agent shortcut is removed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
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
                "Delete",
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}
