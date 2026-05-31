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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Memory
import coil3.compose.AsyncImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.hermes.mobile.feature.chat.ChatStreamSnapshot
import com.hermes.mobile.feature.sessions.SessionListViewModel
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.core.settings.VisualStyle
import com.hermes.mobile.ui.components.HermesSearchField
import com.hermes.mobile.ui.components.LocalHermesVisualStyle
import com.hermes.mobile.ui.components.hermesGlass
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onAgentClick: (AgentProfile, String?) -> Unit,
    onSessionClick: (String) -> Unit = {},
    onAllChatsClick: () -> Unit = {},
    onControlsClick: () -> Unit = {},
    onWorkflowClick: () -> Unit = {},
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dashboardNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(DASHBOARD_CLOCK_TICK_MS)
            dashboardNow = System.currentTimeMillis()
        }
    }
    val rows = remember(state.sessions, state.agents, state.activeStreams, dashboardNow) {
        conversationInboxRows(state.sessions, state.agents, state.activeStreams, now = dashboardNow)
    }
    val continueRow = remember(rows) { dashboardContinueRow(rows) }
    val defaultAgent = state.agents.firstOrNull() ?: AppPreferences.defaultAgents.first()
    val isSearching = state.query.isNotBlank()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background),
        ) {
            HomeTopBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChanged,
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
            contentPadding = PaddingValues(top = 14.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isSearching) {
                item { SectionLabel("Search results") }
                if (rows.isEmpty()) {
                    item {
                        EmptyConversationState(onStartChat = { onAgentClick(defaultAgent, null) })
                    }
                } else {
                    itemsIndexed(rows, key = { _, it -> it.sessionId }) { index, row ->
                        AnimatedConversationRow(
                            index = index,
                            row = row,
                            onClick = { onSessionClick(row.sessionId) },
                        )
                    }
                }
            } else {
                item {
                    ContinueWorkCard(
                        row = continueRow,
                        onStartChat = { onAgentClick(defaultAgent, null) },
                        onContinue = { row -> onSessionClick(row.sessionId) },
                    )
                }
                item {
                    QuickActionsGrid(
                        onNewChat = { onAgentClick(defaultAgent, null) },
                        onAllChats = onAllChatsClick,
                        onControls = onControlsClick,
                        onWorkflow = onWorkflowClick,
                    )
                }
                if (rows.isNotEmpty()) {
                    item { SectionLabel("Recent") }
                    itemsIndexed(rows, key = { _, it -> it.sessionId }) { index, row ->
                        AnimatedConversationRow(
                            index = index,
                            row = row,
                            onClick = { onSessionClick(row.sessionId) },
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
    query: String,
    onQueryChange: (String) -> Unit,
    onStartChat: () -> Unit,
) {
    val actionShape = RoundedCornerShape(50.dp)
    val usesLiquidGlass = LocalHermesVisualStyle.current == VisualStyle.LiquidGlass
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
                    "Resume or start a conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .heightIn(min = 44.dp)
                    .then(
                        if (usesLiquidGlass) {
                            Modifier.hermesGlass(
                                shape = actionShape,
                                role = HermesGlassRole.Action,
                                normalContainerAlpha = 0.82f,
                                normalBorderAlpha = 0.12f,
                            )
                        } else {
                            Modifier
                                .clip(actionShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = actionShape,
                                )
                        }
                    )
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
        HermesSearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Search conversations",
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun AnimatedConversationRow(
    index: Int,
    row: ConversationRowModel,
    onClick: () -> Unit,
) {
    val animAlpha = remember(row.sessionId) { Animatable(0f) }
    val animOffset = remember(row.sessionId) { Animatable(30f) }
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
        ConversationRow(row = row, onClick = onClick)
    }
}

@Composable
private fun ContinueWorkCard(
    row: ConversationRowModel?,
    onStartChat: () -> Unit,
    onContinue: (ConversationRowModel) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.ReadablePanel,
                normalContainerAlpha = 0.7f,
                normalBorderAlpha = 0.22f,
            )
            .clickable { row?.let(onContinue) ?: onStartChat() }
            .padding(16.dp),
    ) {
        Text(
            "Resume chat",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        if (row == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Start a conversation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Choose an agent and keep the thread here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgentAvatar(
                    label = row.agentInitial,
                    active = row.liveState != "none",
                    size = 48,
                    usesDefaultAvatar = row.usesDefaultAvatar,
                    avatarUri = row.avatarUri,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            row.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardStatePill(row.liveState, row.unreadCount)
                    }
                    Text(
                        row.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${row.agentName} - ${row.timestamp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatePill(liveState: String, unreadCount: Int) {
    val label = dashboardStateLabel(liveState, unreadCount) ?: return
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun DashboardMetricsStrip(metrics: HomeDashboardMetrics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile("Live", metrics.liveCount.toString(), Modifier.weight(1f))
        MetricTile("Unread", metrics.unreadCount.coerceAtLeast(0).toString(), Modifier.weight(1f))
        MetricTile("Chats", metrics.chatCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.Status,
                normalContainerAlpha = 0.52f,
                normalBorderAlpha = 0.16f,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickActionsGrid(
    onNewChat: () -> Unit,
    onAllChats: () -> Unit,
    onControls: () -> Unit,
    onWorkflow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionButton("New", Icons.Rounded.Add, onNewChat, Modifier.weight(1f))
            QuickActionButton("All chats", Icons.Rounded.ChatBubble, onAllChats, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionButton("Workflow", Icons.Rounded.AutoAwesome, onWorkflow, Modifier.weight(1f))
            QuickActionButton("Controls", Icons.Rounded.Build, onControls, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.Action,
                normalContainerAlpha = 0.58f,
                normalBorderAlpha = 0.18f,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AgentLaunchRail(
    rows: List<DashboardAgentRow>,
    onAgentClick: (DashboardAgentRow) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.agent.id }) { row ->
            AgentLaunchCard(row = row, onClick = { onAgentClick(row) })
        }
    }
}

@Composable
private fun AgentLaunchCard(row: DashboardAgentRow, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .width(148.dp)
            .heightIn(min = 142.dp)
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.ReadablePanel,
                normalContainerAlpha = 0.58f,
                normalBorderAlpha = 0.18f,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        AgentAvatar(
            label = row.agent.initial,
            active = false,
            size = 42,
            usesDefaultAvatar = row.agent.id == AppPreferences.defaultAgents.first().id,
            avatarUri = row.agent.avatarUri,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            row.agent.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            row.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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

data class HomeDashboardMetrics(
    val liveCount: Int,
    val unreadCount: Int,
    val chatCount: Int,
)

data class DashboardAgentRow(
    val agent: AgentProfile,
    val latestSessionId: String?,
    val subtitle: String,
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
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.ReadablePanel,
                normalContainerAlpha = 0.44f,
                normalBorderAlpha = 0.18f,
            )
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
            val statusLabel = conversationLiveStateLabel(row.liveState)
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
            } else if (statusLabel != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState(onStartChat: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .heightIn(min = 56.dp)
                .hermesGlass(
                    shape = shape,
                    role = HermesGlassRole.Action,
                    normalContainerAlpha = 0.58f,
                    normalBorderAlpha = 0.28f,
                )
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

internal fun latestSessionsByAgent(
    sessions: List<SessionEntity>,
    defaultAgentId: String = AppPreferences.defaultAgents.first().id,
): Map<String, SessionEntity> {
    val byAgent = linkedMapOf<String, SessionEntity>()
    sessions.forEach { session ->
        val agentId = agentIdFromChatSessionId(session.id) ?: defaultAgentId
        val current = byAgent[agentId]
        if (current == null ||
            session.localLastActivityAt > current.localLastActivityAt ||
            (session.localLastActivityAt == current.localLastActivityAt && session.startedAt > current.startedAt)
        ) {
            byAgent[agentId] = session
        }
    }
    return byAgent
}

internal fun conversationInboxRows(
    sessions: List<SessionEntity>,
    agents: List<AgentProfile>,
    activeStreams: Map<String, ChatStreamSnapshot> = emptyMap(),
    now: Long = System.currentTimeMillis(),
): List<ConversationRowModel> {
    val defaultAgent = agents.firstOrNull() ?: AppPreferences.defaultAgents.first()
    val agentsById = agents.associateBy { it.id }
    val savedSessionIds = sessions.map { it.id }.toSet()
    val syntheticStreamingSessions = activeStreams
        .filterKeys { it !in savedSessionIds }
        .values
        .map { stream ->
            val startedAt = stream.userMessage?.timestamp ?: stream.assistantMessageId
            SessionEntity(
                id = stream.sessionId,
                title = null,
                source = "mobile",
                startedAt = startedAt,
                endedAt = null,
                messageCount = if (stream.content.isBlank() && stream.receivedAttachments.isEmpty()) 1 else 2,
                model = "hermes-agent",
                localLastActivityAt = maxOf(startedAt, stream.assistantMessageId),
                lastMessagePreview = stream.userMessage?.content,
            )
        }
    return (sessions + syntheticStreamingSessions)
        .sortedWith(
            compareByDescending<SessionEntity> { it.localLastActivityAt }
                .thenByDescending { it.startedAt },
        )
        .map { session ->
            val agentId = agentIdFromChatSessionId(session.id) ?: defaultAgent.id
            val agent = agentsById[agentId] ?: defaultAgent
            val stream = activeStreams[session.id]
            ConversationRowModel(
                sessionId = session.id,
                agentId = agent.id,
                title = session.title?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()?.takeIf { it.isNotBlank() }
                    ?: agent.name,
                agentName = agent.name,
                preview = stream.conversationPreview()
                    ?: session.lastMessagePreview
                    ?.takeIf { it.isNotBlank() && it != STREAMING_PREVIEW }
                    ?: formatMessageCount(session.messageCount),
                timestamp = formatTimestamp(session.localLastActivityAt),
                agentInitial = agent.initial,
                usesDefaultAvatar = agent.id == AppPreferences.defaultAgents.first().id,
                avatarUri = agent.avatarUri?.takeIf { it.isNotBlank() },
                liveState = conversationLiveState(session, now, stream),
                unreadCount = session.unreadCount,
            )
        }
}

internal fun dashboardMetrics(rows: List<ConversationRowModel>): HomeDashboardMetrics {
    return HomeDashboardMetrics(
        liveCount = rows.count { it.liveState in dashboardLiveStates },
        unreadCount = rows.sumOf { it.unreadCount.coerceAtLeast(0) },
        chatCount = rows.size,
    )
}

internal fun dashboardContinueRow(rows: List<ConversationRowModel>): ConversationRowModel? {
    return rows.minWithOrNull(
        compareBy<ConversationRowModel> { dashboardContinuePriority(it.liveState) }
            .thenByDescending { it.unreadCount.coerceAtLeast(0) }
    )
}

internal fun dashboardAgentRows(
    agents: List<AgentProfile>,
    sessions: List<SessionEntity>,
): List<DashboardAgentRow> {
    val defaultAgentId = AppPreferences.defaultAgents.first().id
    val latestByAgent = latestSessionsByAgent(sessions, defaultAgentId)
    return agents
        .mapIndexed { index, agent ->
            val latestSession = latestByAgent[agent.id]
            IndexedDashboardAgentRow(
                index = index,
                row = DashboardAgentRow(
                    agent = agent,
                    latestSessionId = latestSession?.id,
                    subtitle = agentRowSubtitle(agent, latestSession),
                ),
                latestActivityAt = latestSession?.localLastActivityAt ?: Long.MIN_VALUE,
            )
        }
        .sortedWith(
            compareBy<IndexedDashboardAgentRow> { if (it.row.agent.id == defaultAgentId) 0 else 1 }
                .thenByDescending { it.latestActivityAt }
                .thenBy { it.index },
        )
        .map { it.row }
}

internal fun conversationLiveState(
    session: SessionEntity,
    now: Long = System.currentTimeMillis(),
    stream: ChatStreamSnapshot? = null,
): String {
    if (stream?.error != null) return "error"
    if (stream?.isConnecting == true) return "thinking"
    if (stream?.isStreaming == true) return "streaming"
    if (session.unreadCount > 0) return "unread"
    val age = now - session.localLastActivityAt
    return if (age in 0..RECENT_ACTIVITY_WINDOW_MS) "recent" else "none"
}

internal fun conversationLiveStateLabel(liveState: String): String? {
    return when (liveState) {
        "thinking" -> "Thinking"
        "streaming" -> "Live"
        "error" -> "Issue"
        "recent" -> "Active"
        else -> null
    }
}

internal fun dashboardStateLabel(liveState: String, unreadCount: Int): String? {
    return when {
        liveState == "error" -> "Issue"
        liveState == "thinking" -> "Thinking"
        liveState == "streaming" -> "Live"
        unreadCount > 0 -> "Unread"
        liveState == "recent" -> "Active"
        else -> null
    }
}

private fun dashboardContinuePriority(liveState: String): Int {
    return when (liveState) {
        "error" -> 0
        "thinking" -> 1
        "streaming" -> 2
        "unread" -> 3
        "recent" -> 4
        else -> 5
    }
}

private data class IndexedDashboardAgentRow(
    val index: Int,
    val row: DashboardAgentRow,
    val latestActivityAt: Long,
)

private fun ChatStreamSnapshot?.conversationPreview(): String? {
    if (this == null) return null
    return when {
        error != null -> error
        content.isNotBlank() -> content.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() }?.take(200)
        isConnecting -> "Thinking"
        isStreaming -> "Responding"
        else -> null
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
private const val RECENT_ACTIVITY_WINDOW_MS = 2 * 60 * 1000L
private const val DASHBOARD_CLOCK_TICK_MS = 30 * 1000L
private const val STREAMING_PREVIEW = "Streaming..."
private val dashboardLiveStates = setOf("thinking", "streaming", "error")

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

