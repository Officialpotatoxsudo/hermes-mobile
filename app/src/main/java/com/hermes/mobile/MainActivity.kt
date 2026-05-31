package com.hermes.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.hermes.mobile.core.auth.AppLockManager
import com.hermes.mobile.core.auth.SavedConnection
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.auth.hasSavedConnection
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.settings.ThemeMode
import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.VisualStyle
import com.hermes.mobile.feature.chat.ChatStreamCoordinator
import com.hermes.mobile.navigation.HermesNavGraph
import com.hermes.mobile.ui.theme.HermesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var chatStreamCoordinator: ChatStreamCoordinator

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        isUnlocked = appLockManager.isUnlocked
        setContent {
            val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val visualStyle by appPreferences.visualStyle.collectAsStateWithLifecycle(initialValue = VisualStyle.Normal)
            val liquidGlassConfig by appPreferences.liquidGlassConfig.collectAsStateWithLifecycle(initialValue = LiquidGlassConfig())
            val appLockEnabled by appPreferences.appLockEnabled.collectAsStateWithLifecycle(initialValue = true)
            val lockTimeout by appPreferences.lockTimeout.collectAsStateWithLifecycle(
                initialValue = com.hermes.mobile.core.settings.LockTimeout.FiveMinutes,
            )
            val lastOpenedChatSessionId by appPreferences.lastOpenedChatSessionId.collectAsStateWithLifecycle(initialValue = "")
            val savedConnection by tokenStore.savedConnection
                .map<SavedConnection, SavedConnection?> { it }
                .collectAsStateWithLifecycle(initialValue = null)
            SideEffect {
                appLockManager.setEnabled(appLockEnabled)
                appLockManager.setLockTimeout(lockTimeout.millis)
            }
            HermesTheme(
                themeMode = themeMode,
                visualStyle = visualStyle,
                liquidGlassConfig = liquidGlassConfig,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    Box {
                        HermesNavGraph(
                            connectionLoaded = savedConnection != null,
                            hasCredentials = savedConnection?.let { hasSavedConnection(it.serverUrl, it.apiKey) } == true,
                            appLockEnabled = appLockEnabled,
                            isUnlocked = !appLockEnabled || isUnlocked,
                            lastOpenedChatSessionId = lastOpenedChatSessionId,
                            onUnlocked = {
                                appLockManager.unlock()
                                isUnlocked = true
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        chatStreamCoordinator.markAppBackgrounded()
        appLockManager.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        chatStreamCoordinator.markAppForegrounded()
        if (appLockManager.isSessionExpired()) {
            appLockManager.lock()
            isUnlocked = false
        } else {
            appLockManager.onAppForegrounded()
            isUnlocked = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 41021
    }
}
