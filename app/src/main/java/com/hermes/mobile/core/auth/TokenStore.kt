package com.hermes.mobile.core.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hermes_secure_prefs",
)

data class SavedConnection(
    val serverUrl: String = "",
    val apiKey: String = "",
    val identity: String = "",
)

@Singleton
class TokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_API_KEY = stringPreferencesKey("encrypted_api_key")
        private const val KEYSET_NAME = "hermes_tink_master_key"
        private const val KEYSET_PREFS = "hermes_tink_keyset"
        private const val KEYSTORE_URI = "android-keystore://hermes_tink_master_key"

        init {
            AeadConfig.register()
        }
    }

    private val aead: Aead by lazy { createAead() }
    @Volatile private var cachedConnection = SavedConnection()

    val savedConnection: Flow<SavedConnection> = context.tokenDataStore.data
        .map(::savedConnectionFrom)
        .onEach { cachedConnection = it }

    val serverUrl: String
        get() = cachedConnection.serverUrl

    val apiKey: String
        get() = cachedConnection.apiKey

    fun hasCredentials(): Boolean = hasSavedConnection(serverUrl, apiKey)

    suspend fun savedConnectionOnce(): SavedConnection {
        return savedConnection.first()
    }

    suspend fun connectionIdentity(): String = savedConnectionOnce().identity

    suspend fun saveCredentials(serverUrl: String, apiKey: String) {
        val cleanServerUrl = serverUrl.cleanSavedCredentialLine()
        val cleanApiKey = apiKey.cleanSavedCredentialLine()
        val encrypted = Base64.encodeToString(
            aead.encrypt(cleanApiKey.toByteArray(Charsets.UTF_8), ByteArray(0)),
            Base64.NO_WRAP,
        )
        context.tokenDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = cleanServerUrl
            prefs[KEY_API_KEY] = encrypted
        }
        cachedConnection = SavedConnection(
            serverUrl = cleanServerUrl,
            apiKey = cleanApiKey,
            identity = connectionIdentityFor(cleanServerUrl, cleanApiKey),
        )
    }

    suspend fun clearCredentials() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(KEY_SERVER_URL)
            prefs.remove(KEY_API_KEY)
        }
        cachedConnection = SavedConnection()
    }

    private fun savedConnectionFrom(prefs: Preferences): SavedConnection {
        val serverUrl = prefs[KEY_SERVER_URL].orEmpty().cleanSavedCredentialLine()
        val apiKey = decryptApiKey(prefs[KEY_API_KEY].orEmpty())
        return SavedConnection(
            serverUrl = serverUrl,
            apiKey = apiKey,
            identity = connectionIdentityFor(serverUrl, apiKey),
        )
    }

    private fun decryptApiKey(encrypted: String): String = runCatching {
        if (encrypted.isEmpty()) return@runCatching ""
        val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
        String(aead.decrypt(bytes, ByteArray(0)), Charsets.UTF_8).cleanSavedCredentialLine()
    }.getOrDefault("")

    private fun createAead(): Aead {
        return runCatching {
            buildKeysetManager().keysetHandle.getPrimitive(Aead::class.java)
        }.getOrElse {
            context.getSharedPreferences(KEYSET_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            buildKeysetManager().keysetHandle.getPrimitive(Aead::class.java)
        }
    }

    private fun buildKeysetManager(): AndroidKeysetManager {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(KEYSTORE_URI)
            .build()
    }
}

internal fun hasSavedConnection(serverUrl: String, apiKey: String): Boolean {
    return serverUrl.cleanSavedCredentialLine().isNotBlank() &&
        apiKey.cleanSavedCredentialLine().isNotBlank()
}

internal fun connectionIdentityFor(serverUrl: String, apiKey: String): String {
    if (serverUrl.cleanSavedCredentialLine().isBlank()) return ""
    val cleanApiKey = apiKey.cleanSavedCredentialLine()
    if (cleanApiKey.isBlank()) return ""
    return "agent:${sha256Hex(cleanApiKey)}"
}

internal fun String.cleanSavedCredentialLine(): String {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
}

private fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
