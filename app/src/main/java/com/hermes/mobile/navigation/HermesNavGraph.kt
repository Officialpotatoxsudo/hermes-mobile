package com.hermes.mobile.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hermes.mobile.feature.agent.AgentControlScreen
import com.hermes.mobile.feature.chat.ChatShellScreen
import com.hermes.mobile.feature.connection.ConnectionSetupScreen
import com.hermes.mobile.feature.home.HomeScreen
import com.hermes.mobile.feature.lock.AppLockScreen
import com.hermes.mobile.feature.settings.SettingsScreen
import com.hermes.mobile.feature.sessions.SessionHistoryScreen
import com.hermes.mobile.feature.sessions.SessionListScreen
import com.hermes.mobile.feature.wallet.SendScreen

object Routes {
    const val ConnectionSetup = "connection_setup"
    const val AppLock = "app_lock"
    const val Home = "home"
    const val Chat = "chat"
    const val Sessions = "sessions"
    const val SessionHistory = "session_history"
    const val Settings = "settings"
    const val AgentControl = "agent_control"
    const val SendPayment = "send_payment"
}

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

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
            MainTabScaffold(navController = navController, selectedRoute = Routes.Home) {
                HomeScreen(
                    onNewChat = { navController.navigate(Routes.Chat) },
                    onSessionClick = { sessionId -> navController.navigate("${Routes.SessionHistory}/$sessionId") },
                    onAgentControl = { navController.navigateMainTab(Routes.AgentControl) },
                    onSendPayment = { navController.navigateMainTab(Routes.SendPayment) },
                    onSettings = { navController.navigateMainTab(Routes.Settings) },
                )
            }
        }
        composable(Routes.Chat) {
            MainTabScaffold(navController = navController, selectedRoute = Routes.Chat) {
                ChatShellScreen(
                    onSessionsClick = { navController.navigate(Routes.Sessions) },
                    onSettingsClick = { navController.navigateMainTab(Routes.Settings) },
                    onSendPaymentClick = { navController.navigateMainTab(Routes.SendPayment) },
                )
            }
        }
        composable(Routes.Sessions) {
            SessionListScreen(
                onBack = { navController.navigate(Routes.Home) },
                onSessionClick = { sessionId -> navController.navigate("${Routes.SessionHistory}/$sessionId") },
            )
        }
        composable("${Routes.SessionHistory}/{sessionId}") {
            SessionHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            MainTabScaffold(navController = navController, selectedRoute = Routes.Settings) {
                SettingsScreen(
                    onBack = { navController.navigateMainTab(Routes.Home) },
                    onEditConnection = { navController.navigate(Routes.ConnectionSetup) },
                    onAgentControl = { navController.navigateMainTab(Routes.AgentControl) },
                    onSendPayment = { navController.navigateMainTab(Routes.SendPayment) },
                    onLogout = {
                        navController.navigate(Routes.ConnectionSetup) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    },
                )
            }
        }
        composable(Routes.AgentControl) {
            MainTabScaffold(navController = navController, selectedRoute = Routes.AgentControl) {
                AgentControlScreen(onBack = { navController.navigateMainTab(Routes.Home) })
            }
        }
        composable(Routes.SendPayment) {
            MainTabScaffold(navController = navController, selectedRoute = Routes.SendPayment) {
                SendScreen(onBack = { navController.navigateMainTab(Routes.Home) })
            }
        }
    }
}

private data class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val mainTabs = listOf(
    MainTab(Routes.Home, "Chats", Icons.Rounded.Home),
    MainTab(Routes.Chat, "Live", Icons.Rounded.ChatBubble),
    MainTab(Routes.AgentControl, "Agent", Icons.Rounded.Memory),
    MainTab(Routes.SendPayment, "Pay", Icons.Rounded.Payment),
    MainTab(Routes.Settings, "Settings", Icons.Rounded.Settings),
)

@Composable
private fun MainTabScaffold(
    navController: NavHostController,
    selectedRoute: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(32.dp),
                        )
                        .padding(horizontal = 8.dp),
                ) {
                    mainTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedRoute == tab.route,
                            onClick = { navController.navigateMainTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
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
