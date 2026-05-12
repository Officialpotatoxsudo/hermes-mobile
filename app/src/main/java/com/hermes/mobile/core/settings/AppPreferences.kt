package com.hermes.mobile.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hermes_app_prefs",
)

enum class ThemeMode {
    System,
    Light,
    Dark,
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
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val lockTimeoutKey = stringPreferencesKey("lock_timeout")
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

    val agents: Flow<List<AgentProfile>> = context.appPreferencesDataStore.data.map { prefs ->
        val stored = prefs[agentsKey]
        runCatching {
            if (stored.isNullOrBlank()) {
                defaultAgents
            } else {
                val parsed = json.decodeFromString<List<AgentProfile>>(stored)
                (defaultAgents + parsed.filterNot { it.id == defaultAgents.first().id })
                    .distinctBy { it.id }
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

    suspend fun addAgent(name: String, subtitle: String) {
        val cleanName = name.trim().ifBlank { return }
        context.appPreferencesDataStore.edit { prefs ->
            val current = runCatching {
                json.decodeFromString<List<AgentProfile>>(prefs[agentsKey].orEmpty())
            }.getOrDefault(emptyList())
            val agent = AgentProfile(
                id = "agent-${System.currentTimeMillis()}",
                name = cleanName.take(40),
                subtitle = subtitle.trim().ifBlank { "Custom Hermes agent" }.take(96),
                initial = cleanName.first().uppercase(),
            )
            prefs[agentsKey] = json.encodeToString((current + agent).distinctBy { it.id })
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
