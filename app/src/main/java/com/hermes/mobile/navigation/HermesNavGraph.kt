package com.hermes.mobile.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.VisualStyle
import com.hermes.mobile.core.util.newAgentChatSessionId
import com.hermes.mobile.feature.agent.AgentControlScreen
import com.hermes.mobile.feature.agent.AgentProfilesScreen
import com.hermes.mobile.feature.agent.ParallelAgentWorkflowScreen
import com.hermes.mobile.feature.chat.ChatShellScreen
import com.hermes.mobile.feature.connection.ConnectionSetupScreen
import com.hermes.mobile.feature.home.HomeScreen
import com.hermes.mobile.feature.lab.LiquidGlassLabScreen
import com.hermes.mobile.feature.lock.AppLockScreen
import com.hermes.mobile.feature.settings.SettingsScreen
import com.hermes.mobile.feature.sessions.SessionHistoryScreen
import com.hermes.mobile.feature.sessions.SessionListScreen
import java.net.URLEncoder
import com.hermes.mobile.feature.splash.SplashScreen
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.ui.components.LocalHermesLiquidGlassBackdrop
import com.hermes.mobile.ui.components.LocalHermesLiquidGlassConfig
import com.hermes.mobile.ui.components.LocalHermesVisualStyle
import com.hermes.mobile.ui.components.hermesGlass
import com.hermes.mobile.core.settings.hermesGlassTypeForRole
import com.hermes.mobile.ui.components.liquidGlassSurface
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay

object Routes {
    const val Splash = "splash"
    const val ConnectionSetup = "connection_setup"
    const val AppLock = "app_lock"
    const val Home = "home"
    const val Chat = "chat"
    const val Library = "library"
    const val Sessions = "sessions"
    const val SessionHistory = "session_history"
    const val Settings = "settings"
    const val AgentProfiles = "agent_profiles"
    const val AgentControl = "agent_control"
    const val ParallelAgents = "parallel_agents"
    const val LiquidGlassLab = "liquid_glass_lab"
}

private data class MainTab(
    val route: String,
    val label: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val mainTabs = listOf(
    MainTab(Routes.Home, "Chats", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    MainTab(Routes.Library, "Library", Icons.Rounded.Folder, Icons.Outlined.FolderOpen),
    MainTab(Routes.Settings, "Profile", Icons.Rounded.Person, Icons.Outlined.PersonOutline),
)

internal data class LiquidNavMotion(
    val scaleX: Float,
    val scaleY: Float,
    val translationY: Float,
)

internal fun liquidNavMotion(
    scrollPressure: Float,
    dragPressure: Float,
    config: LiquidGlassConfig,
): LiquidNavMotion {
    val cleanConfig = config.coerced()
    val pressure = (kotlin.math.abs(scrollPressure).coerceIn(0f, 1f) + kotlin.math.abs(dragPressure).coerceIn(0f, 1f))
        .coerceIn(0f, 2f) * cleanConfig.navElasticity
    val clamped = pressure.coerceIn(0f, 3f)
    return LiquidNavMotion(
        scaleX = 1f + 0.026f * clamped,
        scaleY = 1f - 0.032f * clamped,
        translationY = -8f * clamped,
    )
}

internal fun contentBackdropEnabledForStyle(style: VisualStyle): Boolean = false

internal fun bottomNavBackdropEnabledForStyle(style: VisualStyle): Boolean {
    return style == VisualStyle.LiquidGlass
}

@Composable
fun HermesNavGraph(
    connectionLoaded: Boolean,
    hasCredentials: Boolean,
    appLockEnabled: Boolean = true,
    isUnlocked: Boolean,
    lastOpenedChatSessionId: String = "",
    onUnlocked: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = Routes.Splash

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(connectionLoaded, hasCredentials, appLockEnabled, isUnlocked, currentRoute) {
        if (currentRoute == Routes.Splash && connectionLoaded) {
            navController.navigate(splashNextRoute(connectionLoaded, hasCredentials, appLockEnabled, isUnlocked)) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
            return@LaunchedEffect
        }
        if (shouldShowAppLock(hasCredentials, appLockEnabled, isUnlocked, currentRoute)) {
            navController.navigate(Routes.AppLock) {
                launchSingleTop = true
            }
        }
    }
    val isMainTab = currentRoute in mainTabs.map { it.route }
    val visualStyle = LocalHermesVisualStyle.current
    val liquidGlassConfig = LocalHermesLiquidGlassConfig.current.coerced()
    val liquidGlassEnabled = visualStyle == VisualStyle.LiquidGlass
    val contentBackdropEnabled = contentBackdropEnabledForStyle(visualStyle)
    val bottomNavBackdropEnabled = bottomNavBackdropEnabledForStyle(visualStyle)
    val shellBackdrop = rememberLayerBackdrop {
        drawContent()
    }
    var navScrollPressure by remember { mutableFloatStateOf(0f) }
    var navDragPressure by remember { mutableFloatStateOf(0f) }
    val navMotion = liquidNavMotion(
        scrollPressure = navScrollPressure,
        dragPressure = navDragPressure,
        config = liquidGlassConfig,
    )
    LaunchedEffect(navScrollPressure, liquidGlassEnabled) {
        if (liquidGlassEnabled && navScrollPressure != 0f) {
            delay(130)
            navScrollPressure = 0f
        }
    }
    LaunchedEffect(navDragPressure, liquidGlassEnabled) {
        if (liquidGlassEnabled && navDragPressure != 0f) {
            delay(120)
            navDragPressure = 0f
        }
    }
    val navScrollConnection = remember(liquidGlassEnabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (liquidGlassEnabled && source == NestedScrollSource.UserInput && available.y != 0f) {
                    navScrollPressure = (navScrollPressure + (-available.y / 420f)).coerceIn(-1f, 1f)
                }
                return Offset.Zero
            }
        }
    }

    CompositionLocalProvider(
        LocalHermesLiquidGlassBackdrop provides if (contentBackdropEnabled) shellBackdrop else null,
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                AnimatedVisibility(
                    visible = isMainTab,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 26.dp, vertical = 9.dp)
                            .graphicsLayer {
                                if (liquidGlassEnabled) {
                                    scaleX = navMotion.scaleX
                                    scaleY = navMotion.scaleY
                                    translationY = navMotion.translationY
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                }
                            },
                    ) {
                    val navShape = RoundedCornerShape(40.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                if (liquidGlassEnabled) 24.dp else 18.dp,
                                navShape,
                                ambientColor = if (liquidGlassEnabled) {
                                    Color.Black.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                                spotColor = if (liquidGlassEnabled) {
                                    Color.Black.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                            )
                            .then(
                                if (bottomNavBackdropEnabled) {
                                    Modifier.liquidGlassSurface(
                                        backdrop = shellBackdrop,
                                        config = liquidGlassConfig,
                                        shape = navShape,
                                        type = hermesGlassTypeForRole(HermesGlassRole.Navigation),
                                    )
                                } else {
                                    Modifier
                                        .clip(navShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), navShape)
                                },
                            )
                            .pointerInput(liquidGlassEnabled) {
                                if (liquidGlassEnabled) {
                                    detectDragGestures(
                                        onDragCancel = { navDragPressure = 0f },
                                        onDragEnd = { navDragPressure = 0f },
                                        onDrag = { _, dragAmount ->
                                            navDragPressure = (navDragPressure + dragAmount.y / 180f).coerceIn(-1f, 1f)
                                        },
                                    )
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val haptic = LocalHapticFeedback.current
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val interactionSource = remember(tab.route) { MutableInteractionSource() }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (pressed) 0.88f else 1f,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                                label = "tabScale",
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(
                                        if (selected) {
                                            if (liquidGlassEnabled) {
                                                Color.White.copy(alpha = 0.16f)
                                            } else {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            }
                                        } else {
                                            Color.Transparent
                                        },
                                    )
                                    .semantics { this.selected = selected }
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClickLabel = tab.label,
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navController.navigateMainTab(tab.route)
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Crossfade(
                                    targetState = selected,
                                    animationSpec = tween(200),
                                    label = "iconMorph",
                                ) { isSelected ->
                                    Icon(
                                        if (isSelected) tab.filledIcon else tab.outlinedIcon,
                                        contentDescription = tab.label,
                                        tint = color,
                                        modifier = Modifier.size(24.dp).padding(bottom = 2.dp),
                                    )
                                }
                                Text(
                                    tab.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = color,
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(navScrollConnection)
                .then(if (bottomNavBackdropEnabled) Modifier.layerBackdrop(shellBackdrop) else Modifier),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
            val navHostModifier = if (isMainTab) {
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            } else {
                Modifier.fillMaxSize()
            }
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = navHostModifier,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f)) + fadeOut(animationSpec = tween(300))
                }
            ) {
                composable(Routes.Splash) {
                    SplashScreen(
                        onFinished = {
                            val next = splashNextRoute(connectionLoaded, hasCredentials, appLockEnabled, isUnlocked)
                            if (next == Routes.Splash) return@SplashScreen
                            navController.navigate(next) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.ConnectionSetup) {
                    ConnectionSetupScreen(
                        onContinue = {
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.ConnectionSetup) { inclusive = true }
                            }
                        },
                        onCancel = navController.previousBackStackEntry?.let {
                            { navController.popBackStack() }
                        },
                    )
                }
                composable(Routes.AppLock) {
                    AppLockScreen(
                        blockBack = shouldBlockAppLockBack(navController.previousBackStackEntry?.destination?.route),
                        onUnlocked = {
                            onUnlocked()
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.Home) {
                                    popUpTo(Routes.AppLock) { inclusive = true }
                                }
                            }
                        },
                    )
                }
                composable(Routes.Home) {
                    HomeScreen(
                        onAgentClick = { agent, latestSessionId -> navController.navigate(agentChatRoute(agent, latestSessionId)) },
                        onSessionClick = { sessionId -> navController.navigate(resumeChatRoute(sessionId)) },
                        onAllChatsClick = { navController.navigate(Routes.Sessions) },
                        onControlsClick = { navController.navigate(Routes.AgentControl) },
                        onWorkflowClick = { navController.navigate(Routes.ParallelAgents) },
                    )
                }
                composable(Routes.Library) {
                    LibraryScreen(
                        onAllChats = { navController.navigate(Routes.Sessions) },
                        onAgents = { navController.navigate(Routes.AgentProfiles) },
                        onAgentControls = { navController.navigate(Routes.AgentControl) },
                        onParallelAgents = { navController.navigate(Routes.ParallelAgents) },
                        onLiquidGlassLab = { navController.navigate(Routes.LiquidGlassLab) },
                    )
                }
                composable(Routes.Chat) {
                    ChatShellScreen(
                        onHistoryClick = { agentId -> navController.navigate(agentId?.let(::agentHistoryRoute) ?: Routes.Sessions) },
                        onBack = { navController.popChatBackStack() },
                    )
                }
                composable(
                    route = "${Routes.Chat}/{sessionId}?agentName={agentName}",
                    arguments = listOf(
                        navArgument("agentName") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    ChatShellScreen(
                        onHistoryClick = { agentId -> navController.navigate(agentId?.let(::agentHistoryRoute) ?: Routes.Sessions) },
                        onBack = { navController.popChatBackStack() },
                    )
                }
                composable(
                    route = "${Routes.Sessions}?agentId={agentId}",
                    arguments = listOf(
                        navArgument("agentId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    SessionListScreen(
                        onBack = { navController.navigateMainTab(Routes.Library) },
                        onSessionClick = { sessionId -> navController.navigate(sessionListClickRoute(sessionId)) },
                    )
                }
                composable("${Routes.SessionHistory}/{sessionId}") {
                    SessionHistoryScreen(
                        onBack = { navController.popBackStack() },
                        onContinue = { sessionId -> navController.navigate(chatRoute(sessionId)) },
                    )
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        onBack = { navController.navigateMainTab(Routes.Home) },
                        onEditConnection = { navController.navigate(Routes.ConnectionSetup) },
                        onLogout = {
                            navController.navigate(Routes.ConnectionSetup) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.AgentControl) {
                    AgentControlScreen(onBack = { navController.navigateMainTab(Routes.Library) })
                }
                composable(Routes.AgentProfiles) {
                    AgentProfilesScreen(
                        onBack = { navController.navigateMainTab(Routes.Library) },
                        onAgentOpen = { agent -> navController.navigate(agentChatRoute(agent)) },
                    )
                }
                composable(Routes.ParallelAgents) {
                    ParallelAgentWorkflowScreen(
                        onBack = { navController.navigateMainTab(Routes.Library) },
                        onOpenSession = { sessionId -> navController.navigate(chatRoute(sessionId)) },
                    )
                }
                composable(Routes.LiquidGlassLab) {
                    LiquidGlassLabScreen(onBack = { navController.navigateMainTab(Routes.Library) })
                }
            }
        }
    }
    }
}

@Composable
private fun LibraryScreen(
    onAllChats: () -> Unit,
    onAgents: () -> Unit,
    onAgentControls: () -> Unit,
    onParallelAgents: () -> Unit,
    onLiquidGlassLab: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Saved work and native controls",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LibraryRow(
            title = "All chats",
            subtitle = "Search and continue every saved conversation",
            icon = Icons.Rounded.ChatBubble,
            onClick = onAllChats,
        )
        LibraryRow(
            title = "Agents",
            subtitle = "Manage local chat profiles",
            icon = Icons.Rounded.Memory,
            onClick = onAgents,
        )
        LibraryRow(
            title = "Controls",
            subtitle = "Check server status and capabilities",
            icon = Icons.Rounded.Build,
            onClick = onAgentControls,
        )
        LibraryRow(
            title = "Parallel agents",
            subtitle = "Run one prompt across multiple agents",
            icon = Icons.Rounded.AutoAwesome,
            onClick = onParallelAgents,
        )
        LibraryRow(
            title = "Liquid Glass Lab",
            subtitle = "Preview glass UI components",
            icon = Icons.Rounded.AutoAwesome,
            onClick = onLiquidGlassLab,
        )
        Box(Modifier.weight(1f))
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 74.dp)
            .hermesGlass(
                shape = shape,
                role = HermesGlassRole.ReadablePanel,
                normalContainerAlpha = 0.62f,
                normalBorderAlpha = 0.18f,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 14.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun NavHostController.navigateMainTab(route: String) {
    navigate(route) {
        popUpTo(Routes.Home) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.popChatBackStack() {
    if (!popBackStack()) {
        navigateMainTab(Routes.Home)
    }
}

internal fun shouldShowAppLock(
    hasCredentials: Boolean,
    appLockEnabled: Boolean,
    isUnlocked: Boolean,
    currentRoute: String?,
): Boolean {
    return hasCredentials &&
        appLockEnabled &&
        !isUnlocked &&
        currentRoute != null &&
        currentRoute != Routes.Splash &&
        currentRoute != Routes.AppLock
}

internal fun shouldBlockAppLockBack(previousRoute: String?): Boolean {
    return previousRoute != null &&
        previousRoute != Routes.Splash &&
        previousRoute != Routes.AppLock &&
        previousRoute != Routes.ConnectionSetup
}

internal fun splashNextRoute(
    connectionLoaded: Boolean,
    hasCredentials: Boolean,
    appLockEnabled: Boolean,
    isUnlocked: Boolean,
): String {
    return when {
        !connectionLoaded -> Routes.Splash
        !hasCredentials -> Routes.ConnectionSetup
        appLockEnabled && !isUnlocked -> Routes.AppLock
        else -> Routes.Home
    }
}

internal fun mainTabLabels(): List<String> = mainTabs.map { it.label }

internal fun agentChatRoute(agent: AgentProfile, latestSessionId: String? = null): String {
    return chatRoute(latestSessionId ?: newAgentChatSessionId(agent.id), agent.name)
}

internal fun chatRoute(sessionId: String, agentName: String? = null): String {
    val cleanSessionId = sessionId.cleanRouteValue()
    if (cleanSessionId.isBlank()) return Routes.Chat
    val agentQuery = agentName
        ?.cleanRouteValue()
        ?.takeIf { it.isNotBlank() }
        ?.let { "?agentName=${it.routeComponent()}" }
        .orEmpty()
    return "${Routes.Chat}/${cleanSessionId.routeComponent()}$agentQuery"
}

internal fun latestChatRoute(routeSessionId: String?, storedLastChatSessionId: String): String {
    val cleanRouteSessionId = routeSessionId?.cleanRouteValue().orEmpty()
    return chatRoute(cleanRouteSessionId.ifBlank { storedLastChatSessionId.cleanRouteValue() })
}

internal fun sessionHistoryRoute(sessionId: String): String {
    val cleanSessionId = sessionId.cleanRouteValue()
    if (cleanSessionId.isBlank()) return Routes.Sessions
    return "${Routes.SessionHistory}/${cleanSessionId.routeComponent()}"
}

internal fun agentHistoryRoute(agentId: String): String {
    val cleanAgentId = agentId.cleanRouteValue()
    if (cleanAgentId.isBlank()) return Routes.Sessions
    return "${Routes.Sessions}?agentId=${cleanAgentId.routeComponent()}"
}

internal fun sessionListClickRoute(sessionId: String): String = chatRoute(sessionId)

internal fun resumeChatRoute(sessionId: String): String = chatRoute(sessionId)

private fun String.cleanRouteValue(): String {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
}

private fun String.routeComponent(): String {
    return URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}

