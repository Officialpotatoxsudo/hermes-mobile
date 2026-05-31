package com.hermes.mobile.navigation

import com.hermes.mobile.core.settings.AgentProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HermesNavGraphRouteTest {
    @Test
    fun mainTabsAreChatsLibraryProfile() {
        assertEquals(listOf("Chats", "Library", "Profile"), mainTabLabels())
    }

    @Test
    fun liquidGlassRouteIsAvailableAsLibraryExperiment() {
        assertEquals("liquid_glass_lab", Routes.LiquidGlassLab)
    }

    @Test
    fun appLockGateRequiresCredentialsEnabledLockAndLockedSession() {
        assertEquals(
            true,
            shouldShowAppLock(
                hasCredentials = true,
                appLockEnabled = true,
                isUnlocked = false,
                currentRoute = Routes.Home,
            ),
        )
        assertEquals(
            false,
            shouldShowAppLock(
                hasCredentials = true,
                appLockEnabled = false,
                isUnlocked = false,
                currentRoute = Routes.Home,
            ),
        )
        assertEquals(
            false,
            shouldShowAppLock(
                hasCredentials = true,
                appLockEnabled = true,
                isUnlocked = true,
                currentRoute = Routes.Home,
            ),
        )
        assertEquals(
            false,
            shouldShowAppLock(
                hasCredentials = true,
                appLockEnabled = true,
                isUnlocked = false,
                currentRoute = Routes.AppLock,
            ),
        )
    }

    @Test
    fun appLockBackIsBlockedOnlyWhenProtectedRouteIsBehindIt() {
        assertEquals(true, shouldBlockAppLockBack(Routes.Home))
        assertEquals(true, shouldBlockAppLockBack("${Routes.Chat}/{sessionId}?agentName={agentName}"))
        assertEquals(false, shouldBlockAppLockBack(null))
        assertEquals(false, shouldBlockAppLockBack(Routes.Splash))
        assertEquals(false, shouldBlockAppLockBack(Routes.AppLock))
    }

    @Test
    fun splashWaitsForCredentialStoreBeforeChoosingConnectionSetup() {
        assertEquals(
            Routes.Splash,
            splashNextRoute(
                connectionLoaded = false,
                hasCredentials = false,
                appLockEnabled = true,
                isUnlocked = false,
            ),
        )
    }

    @Test
    fun splashSkipsAppLockWhenSettingDisabled() {
        assertEquals(
            Routes.Home,
            splashNextRoute(
                connectionLoaded = true,
                hasCredentials = true,
                appLockEnabled = false,
                isUnlocked = false,
            ),
        )
    }

    @Test
    fun chatRouteEncodesSessionIdPathSegment() {
        assertEquals(
            "chat/telegram%3Atopic%2F42%20A",
            chatRoute(" telegram:topic/42 A "),
        )
    }

    @Test
    fun chatRouteUsesFirstCleanPastedLine() {
        assertEquals(
            "chat/telegram%3Atopic%2F42%20A?agentName=Hermes",
            chatRoute("\n telegram:topic/42 A\nignored ", "\n Hermes\nIgnored "),
        )
    }

    @Test
    fun chatRouteEncodesAgentNameQuery() {
        assertEquals(
            "chat/agent-chat-hermes?agentName=Hermes%20%2F%20Default",
            chatRoute("agent-chat-hermes", " Hermes / Default "),
        )
    }

    @Test
    fun blankChatRouteFallsBackToFreshChat() {
        assertEquals("chat", chatRoute("   "))
    }

    @Test
    fun latestChatRouteUsesStoredSessionWhenNoRouteSession() {
        assertEquals(
            "chat/agent-chat-hermes--2000",
            latestChatRoute(routeSessionId = null, storedLastChatSessionId = " agent-chat-hermes--2000 "),
        )
        assertEquals(
            "chat/route-session",
            latestChatRoute(routeSessionId = "route-session", storedLastChatSessionId = "agent-chat-hermes--2000"),
        )
        assertEquals("chat", latestChatRoute(routeSessionId = null, storedLastChatSessionId = "   "))
    }

    @Test
    fun sessionHistoryRouteEncodesSessionIdPathSegment() {
        assertEquals(
            "session_history/telegram%3Atopic%2F42%20A",
            sessionHistoryRoute(" telegram:topic/42 A "),
        )
    }

    @Test
    fun sessionHistoryRouteUsesFirstCleanPastedLine() {
        assertEquals(
            "session_history/telegram%3Atopic%2F42%20A",
            sessionHistoryRoute("\n telegram:topic/42 A\nignored "),
        )
    }

    @Test
    fun blankSessionHistoryRouteFallsBackToSessionList() {
        assertEquals("sessions", sessionHistoryRoute("   "))
    }

    @Test
    fun sessionListClickOpensChatRoute() {
        assertEquals(
            "chat/telegram%3Atopic%2F42%20A",
            sessionListClickRoute(" telegram:topic/42 A "),
        )
    }

    @Test
    fun resumeChatRouteUsesExistingSessionWithoutCreatingAgentThread() {
        assertEquals(
            "chat/agent-chat-hermes--2000",
            resumeChatRoute(" agent-chat-hermes--2000 "),
        )
        assertEquals("chat", resumeChatRoute("   "))
    }

    @Test
    fun agentHistoryRouteCarriesAgentId() {
        assertEquals(
            "sessions?agentId=hermes%2Fresearch",
            agentHistoryRoute(" hermes/research "),
        )
        assertEquals("sessions", agentHistoryRoute("   "))
    }

    @Test
    fun agentChatRouteUsesLatestAgentSessionWhenAvailable() {
        val agent = AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private chat", initial = "H")

        assertEquals(
            "chat/agent-chat-hermes--2000?agentName=Hermes%20Agent",
            agentChatRoute(agent, latestSessionId = "agent-chat-hermes--2000"),
        )
    }

    @Test
    fun agentChatRouteCreatesFreshThreadWhenNoLatestSession() {
        val agent = AgentProfile(id = "hermes", name = "Hermes Agent", subtitle = "Private chat", initial = "H")
        val route = agentChatRoute(agent)

        assertTrue(route.startsWith("chat/agent-chat-hermes--"))
        assertTrue(route.endsWith("?agentName=Hermes%20Agent"))
    }
}
