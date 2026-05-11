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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hermes_secure_prefs",
)

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
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

    val serverUrl: String
        get() = readString(KEY_SERVER_URL)

    val apiKey: String
        get() = runCatching {
            val encrypted = readString(KEY_API_KEY)
            if (encrypted.isEmpty()) return@runCatching ""
            val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
            String(aead.decrypt(bytes, ByteArray(0)), Charsets.UTF_8)
        }.getOrDefault("")

    fun hasCredentials(): Boolean = serverUrl.isNotBlank() && apiKey.isNotBlank()

    suspend fun saveCredentials(serverUrl: String, apiKey: String) {
        val encrypted = Base64.encodeToString(
            aead.encrypt(apiKey.toByteArray(Charsets.UTF_8), ByteArray(0)),
            Base64.NO_WRAP,
        )
        context.tokenDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl.trim()
            prefs[KEY_API_KEY] = encrypted
        }
    }

    suspend fun clearCredentials() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(KEY_SERVER_URL)
            prefs.remove(KEY_API_KEY)
        }
    }

    private fun readString(key: Preferences.Key<String>): String = runCatching {
        runBlocking {
            context.tokenDataStore.data.map { prefs -> prefs[key].orEmpty() }.first()
        }
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
