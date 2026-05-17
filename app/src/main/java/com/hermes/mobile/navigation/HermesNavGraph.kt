package com.hermes.mobile.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.hermes.mobile.core.util.agentChatSessionId
import com.hermes.mobile.feature.agent.AgentControlScreen
import com.hermes.mobile.feature.chat.ChatShellScreen
import com.hermes.mobile.feature.connection.ConnectionSetupScreen
import com.hermes.mobile.feature.home.HomeScreen
import com.hermes.mobile.feature.lock.AppLockScreen
import com.hermes.mobile.feature.settings.SettingsScreen
import com.hermes.mobile.feature.sessions.SessionHistoryScreen
import com.hermes.mobile.feature.sessions.SessionListScreen
import java.net.URLEncoder
import com.hermes.mobile.feature.splash.SplashScreen

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
    const val AgentControl = "agent_control"
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

@Composable
fun HermesNavGraph(
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

    LaunchedEffect(hasCredentials, isUnlocked, currentRoute) {
        if (shouldShowAppLock(hasCredentials, appLockEnabled, isUnlocked, currentRoute)) {
            navController.navigate(Routes.AppLock) {
                popUpTo(Routes.Home) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val isMainTab = currentRoute in mainTabs.map { it.route }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                        .padding(horizontal = 26.dp, vertical = 9.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(18.dp, RoundedCornerShape(40.dp), ambientColor = MaterialTheme.colorScheme.background, spotColor = MaterialTheme.colorScheme.background)
                            .clip(RoundedCornerShape(40.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), RoundedCornerShape(40.dp))
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
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        } else {
                                            androidx.compose.ui.graphics.Color.Transparent
                                        },
                                    )
                                    .semantics { this.selected = selected }
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
                                        fontSize = 13.sp,
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
        Box(modifier = Modifier.fillMaxSize()) {
             NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                            val next = splashNextRoute(hasCredentials, appLockEnabled, isUnlocked)
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
                    )
                }
                composable(Routes.AppLock) {
                    AppLockScreen(
                        onUnlocked = {
                            onUnlocked()
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.AppLock) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.Home) {
                    HomeScreen(
                        onAgentClick = { agent, latestSessionId -> navController.navigate(agentChatRoute(agent, latestSessionId)) },
                    )
                }
                composable(Routes.Library) {
                    LibraryScreen(
                        onAllChats = { navController.navigate(Routes.Sessions) },
                        onAgents = { navController.navigate(Routes.AgentControl) },
                    )
                }
                composable(Routes.Chat) {
                    val restoreRoute = latestChatRoute(
                        routeSessionId = null,
                        storedLastChatSessionId = lastOpenedChatSessionId,
                    )
                    LaunchedEffect(restoreRoute) {
                        if (restoreRoute != Routes.Chat) {
                            navController.navigate(restoreRoute) {
                                popUpTo(Routes.Chat) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    ChatShellScreen(
                        onHistoryClick = { agentId -> navController.navigate(agentId?.let(::agentHistoryRoute) ?: Routes.Sessions) },
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
            }
        }
    }
}

@Composable
private fun LibraryScreen(onAllChats: () -> Unit, onAgents: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        LibraryRow(
            title = "All chats",
            subtitle = "Search and continue every saved conversation",
            icon = Icons.Rounded.ChatBubble,
            onClick = onAllChats,
        )
        LibraryRow(
            title = "Agents",
            subtitle = "Manage profiles, tools, memory, and skills",
            icon = Icons.Rounded.Memory,
            onClick = onAgents,
        )
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 14.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
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
        currentRoute != Routes.Splash
}

internal fun splashNextRoute(
    hasCredentials: Boolean,
    appLockEnabled: Boolean,
    isUnlocked: Boolean,
): String {
    return when {
        !hasCredentials -> Routes.ConnectionSetup
        appLockEnabled && !isUnlocked -> Routes.AppLock
        else -> Routes.Home
    }
}

internal fun mainTabLabels(): List<String> = mainTabs.map { it.label }

internal fun agentChatRoute(agent: AgentProfile, latestSessionId: String? = null): String {
    return chatRoute(latestSessionId ?: agentChatSessionId(agent.id), agent.name)
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

internal fun sessionListClickRoute(sessionId: String): String = sessionHistoryRoute(sessionId)

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
