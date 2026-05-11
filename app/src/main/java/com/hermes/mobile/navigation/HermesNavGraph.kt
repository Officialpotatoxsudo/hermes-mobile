package com.hermes.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hermes.mobile.feature.agent.AgentControlScreen
import com.hermes.mobile.feature.chat.ChatShellScreen
import com.hermes.mobile.feature.connection.ConnectionSetupScreen
import com.hermes.mobile.feature.lock.AppLockScreen
import com.hermes.mobile.feature.settings.SettingsScreen
import com.hermes.mobile.feature.sessions.SessionHistoryScreen
import com.hermes.mobile.feature.sessions.SessionListScreen
import com.hermes.mobile.feature.wallet.SendScreen

object Routes {
    const val ConnectionSetup = "connection_setup"
    const val AppLock = "app_lock"
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
        else -> Routes.Chat
    }

    LaunchedEffect(hasCredentials, isUnlocked) {
        if (hasCredentials && !isUnlocked) {
            navController.navigate(Routes.AppLock) {
                popUpTo(Routes.Chat) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ConnectionSetup) {
            ConnectionSetupScreen(
                onContinue = {
                    navController.navigate(Routes.Chat) {
                        popUpTo(Routes.ConnectionSetup) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.AppLock) {
            AppLockScreen(
                onUnlocked = {
                    onUnlocked()
                    navController.navigate(Routes.Chat) {
                        popUpTo(Routes.AppLock) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Chat) {
            ChatShellScreen(
                onSessionsClick = { navController.navigate(Routes.Sessions) },
                onSettingsClick = { navController.navigate(Routes.Settings) },
                onSendPaymentClick = { navController.navigate(Routes.SendPayment) },
            )
        }
        composable(Routes.Sessions) {
            SessionListScreen(
                onBack = { navController.navigate(Routes.Chat) },
                onSessionClick = { sessionId -> navController.navigate("${Routes.SessionHistory}/$sessionId") },
            )
        }
        composable("${Routes.SessionHistory}/{sessionId}") {
            SessionHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditConnection = { navController.navigate(Routes.ConnectionSetup) },
                onAgentControl = { navController.navigate(Routes.AgentControl) },
                onSendPayment = { navController.navigate(Routes.SendPayment) },
                onLogout = {
                    navController.navigate(Routes.ConnectionSetup) {
                        popUpTo(Routes.Chat) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.AgentControl) {
            AgentControlScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SendPayment) {
            SendScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
