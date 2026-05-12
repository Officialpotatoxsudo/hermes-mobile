package com.hermes.mobile.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
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
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hermes.mobile.feature.agent.AgentControlScreen
import com.hermes.mobile.feature.chat.ChatShellScreen
import com.hermes.mobile.feature.connection.ConnectionSetupScreen
import com.hermes.mobile.feature.home.HomeScreen
import com.hermes.mobile.feature.lock.AppLockScreen
import com.hermes.mobile.feature.settings.SettingsScreen
import com.hermes.mobile.feature.sessions.SessionHistoryScreen
import com.hermes.mobile.feature.sessions.SessionListScreen

object Routes {
    const val ConnectionSetup = "connection_setup"
    const val AppLock = "app_lock"
    const val Home = "home"
    const val Chat = "chat"
    const val Sessions = "sessions"
    const val SessionHistory = "session_history"
    const val Settings = "settings"
    const val AgentControl = "agent_control"
}

private data class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val mainTabs = listOf(
    MainTab(Routes.Home, "Chats", Icons.Rounded.ChatBubble),
    MainTab(Routes.AgentControl, "Agent", Icons.Rounded.Memory),
    MainTab(Routes.Settings, "Profile", Icons.Rounded.Person),
)

@Composable
fun HermesNavGraph(
    hasCredentials: Boolean,
    isUnlocked: Boolean,
    onUnlocked: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val startDestination = when {
        !hasCredentials -> Routes.ConnectionSetup
        !isUnlocked -> Routes.AppLock
        else -> Routes.Home
    }

    LaunchedEffect(hasCredentials, isUnlocked) {
        if (hasCredentials && !isUnlocked) {
            navController.navigate(Routes.AppLock) {
                popUpTo(Routes.Home) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
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
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(34.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                                RoundedCornerShape(34.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                        } else {
                                            androidx.compose.ui.graphics.Color.Transparent
                                        },
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { navController.navigateMainTab(tab.route) },
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.padding(bottom = 3.dp))
                                Text(tab.label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp), color = color)
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
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                }
            ) {
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
                        onNewChat = { navController.navigate(Routes.Chat) },
                        onSessionClick = { sessionId -> navController.navigate("${Routes.SessionHistory}/$sessionId") },
                        onAgentControl = { navController.navigateMainTab(Routes.AgentControl) },
                        onSettings = { navController.navigateMainTab(Routes.Settings) },
                    )
                }
                composable(Routes.Chat) {
                    ChatShellScreen(
                        onSessionsClick = { navController.navigate(Routes.Sessions) },
                        onSettingsClick = { navController.navigateMainTab(Routes.Settings) },
                    )
                }
                composable("${Routes.Chat}/{sessionId}") {
                    ChatShellScreen(
                        onSessionsClick = { navController.navigate(Routes.Sessions) },
                        onSettingsClick = { navController.navigateMainTab(Routes.Settings) },
                    )
                }
                composable(Routes.Sessions) {
                    SessionListScreen(
                        onBack = { navController.navigate(Routes.Home) },
                        onSessionClick = { sessionId -> navController.navigate("${Routes.SessionHistory}/$sessionId") },
                    )
                }
                composable("${Routes.SessionHistory}/{sessionId}") {
                    SessionHistoryScreen(
                        onBack = { navController.popBackStack() },
                        onContinue = { sessionId -> navController.navigate("${Routes.Chat}/$sessionId") },
                    )
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        onBack = { navController.navigateMainTab(Routes.Home) },
                        onEditConnection = { navController.navigate(Routes.ConnectionSetup) },
                        onAgentControl = { navController.navigateMainTab(Routes.AgentControl) },
                        onLogout = {
                            navController.navigate(Routes.ConnectionSetup) {
                                popUpTo(Routes.Home) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.AgentControl) {
                    AgentControlScreen(onBack = { navController.navigateMainTab(Routes.Home) })
                }
            }
        }
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
