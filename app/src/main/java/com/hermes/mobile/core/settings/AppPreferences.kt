package com.hermes.mobile.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hermes_app_prefs",
)

enum class ThemeMode {
    System,
    Light,
    Dark,
    PureBlack,
    PureWhite,
    Sepia,
    Nord,
    Catppuccin,
}

enum class LockTimeout(val label: String, val millis: Long) {
    Immediate("Immediate", 0L),
    FiveMinutes("5 min", 5 * 60 * 1000L),
    FifteenMinutes("15 min", 15 * 60 * 1000L),
    OneHour("1 hr", 60 * 60 * 1000L),
}

@Serializable
data class AgentProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val initial: String,
    val avatarUri: String? = null,
)

@Singleton
class AppPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val lockTimeoutKey = stringPreferencesKey("lock_timeout")
    private val hideChatPreviewsKey = booleanPreferencesKey("hide_chat_previews")
    private val lastOpenedChatSessionIdKey = stringPreferencesKey("last_opened_chat_session_id")
    private val agentsKey = stringPreferencesKey("agent_profiles")
    private val json = Json { ignoreUnknownKeys = true }

    val themeMode: Flow<ThemeMode> = context.appPreferencesDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.System.name) }
            .getOrDefault(ThemeMode.System)
    }

    val lockTimeout: Flow<LockTimeout> = context.appPreferencesDataStore.data.map { prefs ->
        runCatching { LockTimeout.valueOf(prefs[lockTimeoutKey] ?: LockTimeout.FiveMinutes.name) }
            .getOrDefault(LockTimeout.FiveMinutes)
    }

    val appLockEnabled: Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[appLockEnabledKey] ?: true
    }

    val hideChatPreviews: Flow<Boolean> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[hideChatPreviewsKey] ?: true
    }

    val lastOpenedChatSessionId: Flow<String> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[lastOpenedChatSessionIdKey].orEmpty()
    }

    val agents: Flow<List<AgentProfile>> = context.appPreferencesDataStore.data.map { prefs ->
        val stored = prefs[agentsKey]
        runCatching {
            if (stored.isNullOrBlank()) {
                defaultAgents
            } else {
                val parsed = json.decodeFromString<List<AgentProfile>>(stored)
                mergeStoredAgentProfiles(parsed)
            }
        }.getOrDefault(defaultAgents)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setLockTimeout(timeout: LockTimeout) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[lockTimeoutKey] = timeout.name
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[appLockEnabledKey] = enabled
        }
    }

    suspend fun setHideChatPreviews(enabled: Boolean) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[hideChatPreviewsKey] = enabled
        }
    }

    suspend fun setLastOpenedChatSessionId(sessionId: String) {
        context.appPreferencesDataStore.edit { prefs ->
            val cleanSessionId = sessionId.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            if (cleanSessionId.isBlank()) {
                prefs.remove(lastOpenedChatSessionIdKey)
            } else {
                prefs[lastOpenedChatSessionIdKey] = cleanSessionId
            }
        }
    }

    suspend fun addAgent(name: String, subtitle: String) {
        val agent = buildCustomAgentProfile(
            id = newCustomAgentId(),
            name = name,
            subtitle = subtitle,
        ) ?: return
        context.appPreferencesDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<AgentProfile>>(prefs[agentsKey].orEmpty())
            }.getOrDefault(emptyList())
            prefs[agentsKey] = json.encodeToString(upsertCustomAgent(current, agent))
        }
    }

    suspend fun updateAgent(id: String, name: String, subtitle: String, avatarUri: String? = null) {
        val agent = buildCustomAgentProfile(id, name, subtitle, avatarUri) ?: return
        context.appPreferencesDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<AgentProfile>>(prefs[agentsKey].orEmpty())
            }.getOrDefault(emptyList())
            prefs[agentsKey] = json.encodeToString(upsertCustomAgent(current, agent))
        }
    }

    suspend fun updateAgentAvatar(id: String, avatarUri: String?) {
        context.appPreferencesDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<AgentProfile>>(prefs[agentsKey].orEmpty())
            }.getOrDefault(emptyList())
            val updated = current.map { agent ->
                if (agent.id == id) agent.copy(avatarUri = avatarUri?.takeIf { it.isNotBlank() }) else agent
            }
            prefs[agentsKey] = json.encodeToString(updated)
        }
    }

    suspend fun removeAgent(id: String) {
        context.appPreferencesDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<AgentProfile>>(prefs[agentsKey].orEmpty())
            }.getOrDefault(emptyList())
            prefs[agentsKey] = json.encodeToString(removeCustomAgent(current, id))
        }
    }

    companion object {
        val defaultAgents = listOf(
            AgentProfile(
                id = "hermes",
                name = "Hermes Agent",
                subtitle = "Private agent chat",
                initial = "H",
            ),
        )
    }
}

internal fun newCustomAgentId(uuid: UUID = UUID.randomUUID()): String {
    return "agent-$uuid"
}
