package com.hermes.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.auth.AppLockManager
import com.hermes.mobile.core.auth.TokenStore
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.settings.ThemeMode
import com.hermes.mobile.navigation.HermesNavGraph
import com.hermes.mobile.ui.theme.HermesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var appPreferences: AppPreferences

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isUnlocked = appLockManager.isUnlocked
        setContent {
            val themeMode by appPreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            HermesTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box {
                        HermesNavGraph(
                            hasCredentials = tokenStore.hasCredentials(),
                            isUnlocked = isUnlocked,
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
        appLockManager.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        if (appLockManager.isSessionExpired()) {
            appLockManager.lock()
            isUnlocked = false
        } else {
            appLockManager.onAppForegrounded()
            isUnlocked = true
        }
    }
}
