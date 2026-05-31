package com.hermes.mobile.feature.lab

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.core.settings.HermesGlassType
import com.hermes.mobile.core.settings.hermesGlassTypeForRole
import com.hermes.mobile.core.settings.hermesGlassTypes
import com.hermes.mobile.ui.components.liquidGlassSurface
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.qmdeve.liquidglass.widget.LiquidGlassView
import java.util.Locale
import kotlin.math.roundToInt

internal data class LiquidGlassPreviewTab(
    val id: String,
    val label: String,
)

internal val liquidGlassPreviewTabs = listOf(
    LiquidGlassPreviewTab("home", "Home"),
    LiquidGlassPreviewTab("chat", "Chat"),
    LiquidGlassPreviewTab("controls", "Controls"),
    LiquidGlassPreviewTab("settings", "Settings"),
)

@Composable
fun LiquidGlassLabScreen(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableStateOf(liquidGlassPreviewTabs.first().id) }
    var settings by remember { mutableStateOf(LiquidGlassConfig()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071018),
                        Color(0xFF16251D),
                        Color(0xFF10141F),
                    ),
                ),
            )
            .safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LabTopBar(onBack = onBack)
        }
        item {
            PreviewTabStrip(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
            )
        }
        item {
            HermesReplicaPanel(
                selectedTab = selectedTab,
                settings = settings.coerced(),
                onSelectTab = { selectedTab = it },
            )
        }
        item {
            LiquidGlassControls(
                settings = settings.coerced(),
                onSettingsChange = { settings = it.coerced() },
            )
        }
        item {
            QmLiquidGlassPanel(settings = settings.coerced())
        }
        item {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun LabTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Liquid Glass Lab",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Full Hermes preview with live glass tuning",
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PreviewTabStrip(
    selectedTab: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        liquidGlassPreviewTabs.forEach { tab ->
            val selected = tab.id == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(tab.id) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    color = Color.White.copy(alpha = if (selected) 1f else 0.68f),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HermesReplicaPanel(
    selectedTab: String,
    settings: LiquidGlassConfig,
    onSelectTab: (String) -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "liquid-replica")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val backdrop = rememberLayerBackdrop {
        drawRect(Color(0xFF071018))
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(690.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF081018))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(34.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(backdrop),
        ) {
            ReplicaBackdropArt(drift)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassTopStatus(backdrop, settings)
            AnimatedContent(
                targetState = selectedTab,
                label = "liquid-screen-content",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { tab ->
                when (tab) {
                    "home" -> ReplicaHome(backdrop, settings)
                    "chat" -> ReplicaChat(backdrop, settings)
                    "controls" -> ReplicaControls(backdrop, settings)
                    else -> ReplicaSettings(backdrop, settings)
                }
            }
            GlassBottomNav(backdrop, settings, selectedTab, onSelectTab)
        }
    }
}

@Composable
private fun GlassTopStatus(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .liquidGlass(
                backdrop = backdrop,
                settings = settings,
                type = hermesGlassTypeForRole(HermesGlassRole.Status),
                radius = settings.cornerRadius.dp,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text("Hermes", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Liquid preview shell", color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp, maxLines = 1)
        }
        GlassMiniButton(backdrop, settings, Icons.Rounded.Search)
        Spacer(Modifier.width(8.dp))
        GlassMiniButton(backdrop, settings, Icons.Rounded.Add)
    }
}

@Composable
private fun ReplicaHome(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Text("Chats", color = Color.White, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Resume or start a conversation", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassMetric("Live", "2", Modifier.weight(1f))
                GlassMetric("Unread", "4", Modifier.weight(1f))
                GlassMetric("Chats", "18", Modifier.weight(1f))
            }
        }
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Text("Continue", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            ConversationPreviewRow("H", "Hermes Agent", "Draft automation workflow ready", "Now", active = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassActionTile(backdrop, settings, "New", Icons.Rounded.Add, Modifier.weight(1f))
            GlassActionTile(backdrop, settings, "Workflow", Icons.Rounded.Route, Modifier.weight(1f))
        }
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Text("Recent", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            ConversationPreviewRow("R", "Research", "Found three server health regressions", "2m")
            Spacer(Modifier.height(10.dp))
            ConversationPreviewRow("C", "Coding", "Patch applied and tests are green", "9m")
        }
    }
}

@Composable
private fun ReplicaChat(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF41D6A8).copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("H", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Hermes Agent", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Reasoning live", color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp)
                }
            }
        }
        ChatBubble(backdrop, settings, "Can you review the automations and continue the chat?", outgoing = true)
        ChatBubble(backdrop, settings, "I found the session scope issue. The URL changed but the agent key stayed the same, so history should follow the key.", outgoing = false)
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Color(0xFF67E8F9))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("Reasoning", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Checking local scope, remote messages, and route state...", color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .liquidGlass(
                    backdrop = backdrop,
                    settings = settings,
                    type = hermesGlassTypeForRole(HermesGlassRole.Action),
                    radius = 24.dp,
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = Color.White.copy(alpha = 0.76f))
            Text("Ask Hermes...", color = Color.White.copy(alpha = 0.58f), modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White.copy(alpha = 0.76f))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = Color(0xFF41D6A8))
        }
    }
}

@Composable
private fun ReplicaControls(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Text("Controls", color = Color.White, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Server status and capabilities", color = Color.White.copy(alpha = 0.68f))
        }
        ControlPreviewCard(backdrop, settings, "Health", "Server reachable", Icons.Rounded.CloudDone, selected = true)
        ControlPreviewCard(backdrop, settings, "Models", "Provider and model routing", Icons.Rounded.Memory)
        ControlPreviewCard(backdrop, settings, "Storage", "Session sync and files", Icons.Rounded.Storage)
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Text("Rendered output", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassMetric("HTTP", "200", Modifier.weight(1f))
                GlassMetric("Latency", "84ms", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReplicaSettings(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassSection(backdrop, settings, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Profile", color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Connected Hermes agent", color = Color.White.copy(alpha = 0.68f))
                }
            }
        }
        SettingsPreviewRow(backdrop, settings, "Connection", "Same agent across server URLs", Icons.Rounded.Wifi)
        SettingsPreviewRow(backdrop, settings, "Agents", "Profiles and instructions", Icons.Rounded.Memory)
        SettingsPreviewRow(backdrop, settings, "Appearance", "Liquid glass experiment", Icons.Rounded.Settings)
        SettingsPreviewRow(backdrop, settings, "Automation", "Parallel workflows", Icons.Rounded.Route)
    }
}

@Composable
private fun GlassBottomNav(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .liquidGlass(
                backdrop = backdrop,
                settings = settings,
                type = hermesGlassTypeForRole(HermesGlassRole.Navigation),
                radius = 28.dp,
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ElasticBottomNavItem(backdrop, settings, "home", selectedTab, "Chats", Icons.Rounded.ChatBubble, onSelectTab)
        ElasticBottomNavItem(backdrop, settings, "chat", selectedTab, "Chat", Icons.Rounded.AutoAwesome, onSelectTab)
        ElasticBottomNavItem(backdrop, settings, "controls", selectedTab, "Library", Icons.Rounded.Build, onSelectTab)
        ElasticBottomNavItem(backdrop, settings, "settings", selectedTab, "Profile", Icons.Rounded.Person, onSelectTab)
    }
}

@Composable
private fun RowScope.ElasticBottomNavItem(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    id: String,
    selectedTab: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelectTab: (String) -> Unit,
) {
    val selected = id == selectedTab
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember(id) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleX by animateFloatAsState(
        targetValue = when {
            pressed -> 1.10f
            selected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "elasticNavScaleX",
    )
    val scaleY by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "elasticNavScaleY",
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelectTab(id)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .liquidGlass(
                        backdrop = backdrop,
                        settings = settings.copy(surfaceAlpha = (settings.surfaceAlpha + 0.08f).coerceAtMost(0.75f)),
                        type = hermesGlassTypeForRole(HermesGlassRole.Navigation),
                        radius = 24.dp,
                    ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else Color.White.copy(alpha = 0.54f),
                modifier = Modifier.size(if (selected) 22.dp else 20.dp),
            )
            Text(
                label,
                color = Color.White.copy(alpha = if (selected) 1f else 0.54f),
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GlassSection(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .liquidGlass(backdrop, settings)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun GlassActionTile(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(68.dp)
            .liquidGlass(
                backdrop = backdrop,
                settings = settings,
                type = hermesGlassTypeForRole(HermesGlassRole.Action),
                radius = 24.dp,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun GlassMiniButton(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .liquidGlass(
                backdrop = backdrop,
                settings = settings,
                type = hermesGlassTypeForRole(HermesGlassRole.Action),
                radius = 18.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun GlassMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(10.dp),
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.66f), fontSize = 11.sp)
    }
}

@Composable
private fun ConversationPreviewRow(
    initial: String,
    title: String,
    preview: String,
    time: String,
    active: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background((if (active) Color(0xFF41D6A8) else Color.White).copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(preview, color = Color.White.copy(alpha = 0.64f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(time, color = Color.White.copy(alpha = 0.56f), fontSize = 11.sp)
    }
}

@Composable
private fun ChatBubble(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    text: String,
    outgoing: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .liquidGlass(
                    backdrop = backdrop,
                    settings = settings.copy(surfaceAlpha = if (outgoing) settings.surfaceAlpha + 0.04f else settings.surfaceAlpha),
                    type = hermesGlassTypeForRole(HermesGlassRole.ChatBubble),
                    radius = 24.dp,
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun ControlPreviewCard(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .liquidGlass(
                backdrop = backdrop,
                settings = settings.copy(surfaceAlpha = if (selected) settings.surfaceAlpha + 0.08f else settings.surfaceAlpha),
                type = hermesGlassTypeForRole(HermesGlassRole.ReadablePanel),
                radius = 24.dp,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsPreviewRow(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .liquidGlass(backdrop, settings, radius = 23.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun Modifier.liquidGlass(
    backdrop: Backdrop,
    settings: LiquidGlassConfig,
    type: HermesGlassType = hermesGlassTypeForRole(HermesGlassRole.ReadablePanel),
    radius: Dp = settings.cornerRadius.dp,
): Modifier {
    val typedRadius = radius + type.radiusDelta.dp
    return liquidGlassSurface(
        backdrop = backdrop,
        config = settings,
        shape = RoundedCornerShape(typedRadius),
        type = type,
    )
}

@Composable
private fun ReplicaBackdropArt(drift: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF103B3F),
                        Color(0xFF41275F),
                        Color(0xFF84622E),
                    ),
                ),
            ),
    ) {
        val circles = listOf(
            GlassOrb(Color(0xFF45F0C8), 140.dp, 0.08f + 0.12f * drift, 0.08f),
            GlassOrb(Color(0xFFFFD166), 184.dp, 0.54f - 0.08f * drift, 0.18f),
            GlassOrb(Color(0xFF9A7BFF), 210.dp, 0.68f, 0.48f + 0.08f * drift),
            GlassOrb(Color(0xFFFF5E7E), 120.dp, 0.18f, 0.70f - 0.05f * drift),
        )
        circles.forEach { orb ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = size.width * orb.x
                        translationY = size.height * orb.y
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(orb.size)
                        .clip(CircleShape)
                        .background(orb.color.copy(alpha = 0.56f)),
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassControls(
    settings: LiquidGlassConfig,
    onSettingsChange: (LiquidGlassConfig) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Realtime Glass Controls",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            Text(
                "Live",
                color = Color(0xFF41D6A8),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
        GlassTypeLegend()
        ControlSlider("Blur", settings.blur, 0f..24f, "dp") {
            onSettingsChange(settings.copy(blur = it))
        }
        ControlSlider("Refraction height", settings.refractionHeight, 12f..50f, "dp") {
            onSettingsChange(settings.copy(refractionHeight = it))
        }
        ControlSlider("Refraction amount", settings.refractionAmount, 18f..80f, "dp") {
            onSettingsChange(settings.copy(refractionAmount = it))
        }
        ControlSlider("Corner radius", settings.cornerRadius, 12f..44f, "dp") {
            onSettingsChange(settings.copy(cornerRadius = it))
        }
        ControlSlider("Surface opacity", settings.surfaceAlpha, 0f..0.75f, "") {
            onSettingsChange(settings.copy(surfaceAlpha = it))
        }
        ControlSlider("Qm dispersion", settings.dispersion, 0f..1f, "") {
            onSettingsChange(settings.copy(dispersion = it))
        }
        ToggleRow("Chromatic aberration", settings.chromaticAberration) {
            onSettingsChange(settings.copy(chromaticAberration = it))
        }
        ToggleRow("Depth effect", settings.depthEffect) {
            onSettingsChange(settings.copy(depthEffect = it))
        }
    }
}

@Composable
private fun GlassTypeLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "Clear glass roles",
            color = Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.labelLarge,
        )
        hermesGlassTypes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.White.copy(alpha = if (type.elastic) 0.14f else 0.09f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(17.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (type.elastic) 0.92f else 0.58f)),
                        )
                        Text(
                            type.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(formatControlValue(value, suffix), color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun QmLiquidGlassPanel(settings: LiquidGlassConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "QmDeve LiquidGlassView",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "Native View renderer bound to the same live controls"
            } else {
                "Native renderer fallback on Android ${Build.VERSION.SDK_INT}"
            },
            color = Color.White.copy(alpha = 0.70f),
            style = MaterialTheme.typography.bodyMedium,
        )
        AndroidLiquidGlassSample(
            settings = settings,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(24.dp)),
        )
    }
}

@Composable
private fun AndroidLiquidGlassSample(
    settings: LiquidGlassConfig,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val root = FrameLayout(context).apply {
                clipToOutline = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(24.dp.dpPx(density), 24.dp.dpPx(density), 24.dp.dpPx(density), 24.dp.dpPx(density))
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        AndroidColor.rgb(17, 44, 48),
                        AndroidColor.rgb(82, 47, 104),
                        AndroidColor.rgb(184, 130, 62),
                    ),
                ).apply {
                    cornerRadius = 24.dp.dpPx(density).toFloat()
                }
            }
            root.addView(
                content,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )

            listOf("Home replica", "Chat composer", "Controls output", "Settings rows").forEachIndexed { index, label ->
                content.addView(
                    nativeLabel(context, label, index),
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        44.dp.dpPx(density),
                    ).apply {
                        bottomMargin = if (index == 3) 0 else 10.dp.dpPx(density)
                    },
                )
            }

            val glass = LiquidGlassView(context).apply {
                tag = QM_GLASS_TAG
                setTouchEffectEnabled(true)
                setElasticEnabled(true)
                bind(content)
            }
            val glassLabel = TextView(context).apply {
                text = "Hermes"
                gravity = Gravity.CENTER
                setTextColor(AndroidColor.WHITE)
                textSize = 24f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                includeFontPadding = false
                setShadowLayer(12f, 0f, 3f, AndroidColor.argb(120, 0, 0, 0))
            }
            glass.addView(
                glassLabel,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )

            val size = 168.dp.dpPx(density)
            root.addView(glass, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            applyQmSettings(glass, settings, density)
            root
        },
        update = { root ->
            val glass = root.findViewWithTag<LiquidGlassView>(QM_GLASS_TAG)
            if (glass != null) {
                applyQmSettings(glass, settings, density)
            }
        },
    )
}

private data class GlassOrb(
    val color: Color,
    val size: Dp,
    val x: Float,
    val y: Float,
)

private fun nativeLabel(
    context: android.content.Context,
    label: String,
    index: Int,
): TextView {
    return TextView(context).apply {
        text = label
        gravity = Gravity.CENTER_VERTICAL
        setPadding(18, 0, 18, 0)
        setTextColor(AndroidColor.WHITE)
        textSize = 15f
        includeFontPadding = false
        alpha = 0.78f - index * 0.06f
        background = GradientDrawable().apply {
            cornerRadius = 18f
            setColor(AndroidColor.argb(44, 255, 255, 255))
            setStroke(1, AndroidColor.argb(42, 255, 255, 255))
        }
    }
}

private fun applyQmSettings(
    glass: LiquidGlassView,
    settings: LiquidGlassConfig,
    density: androidx.compose.ui.unit.Density,
) {
    val clean = settings.coerced()
    glass.setCornerRadius(clean.cornerRadius.dp.dpPx(density).toFloat())
    glass.setRefractionHeight(clean.refractionHeight.dp.dpPx(density).toFloat())
    glass.setRefractionOffset(clean.refractionAmount.dp.dpPx(density).toFloat())
    glass.setTintColorRed(0.86f)
    glass.setTintColorGreen(0.95f)
    glass.setTintColorBlue(1.0f)
    glass.setTintAlpha(0f)
    glass.setDispersion(clean.dispersion)
    glass.setBlurRadius(clean.blur)
}

private fun formatControlValue(value: Float, suffix: String): String {
    val text = if (value >= 10f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
    return if (suffix.isBlank()) text else "$text$suffix"
}

private fun Dp.dpPx(density: androidx.compose.ui.unit.Density): Int {
    return with(density) { toPx().roundToInt() }
}

private const val QM_GLASS_TAG = "qm_liquid_glass_view"



