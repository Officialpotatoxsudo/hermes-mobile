package com.hermes.mobile.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.settings.LockTimeout
import com.hermes.mobile.core.settings.ThemeMode
import com.hermes.mobile.ui.components.frostedGlass
import com.hermes.mobile.ui.components.HermesChip
import com.hermes.mobile.ui.components.HermesHeader
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditConnection: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        HermesHeader(title = "Settings", trailingAction = "Done", onTrailingAction = onBack)
        Spacer(Modifier.height(24.dp))
        val contentAlpha = remember { Animatable(0f) }
        val contentOffset = remember { Animatable(16f) }
        LaunchedEffect(Unit) {
            contentAlpha.animateTo(1f, animationSpec = spring(stiffness = 300f))
            contentOffset.animateTo(0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
        }
        Column(
            modifier = Modifier
                .graphicsLayer { alpha = contentAlpha.value; translationY = contentOffset.value }
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Appearance")
            SettingsSection {
                SettingItem("Theme", "Choose app color mode") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            HermesChip(
                                text = mode.name,
                                selected = state.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Security")
            SettingsSection {
                SettingItem("App lock", "Choose lock timeout") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LockTimeout.entries.forEach { timeout ->
                            val selected = state.lockTimeout == timeout
                            Text(
                                text = timeout.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { viewModel.setLockTimeout(timeout) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                SectionDivider()
                ToggleRow(
                    title = "Hide chat previews",
                    subtitle = "Block content in screenshots and app switcher",
                    checked = state.hideChatPreviews,
                    onCheckedChange = viewModel::setHideChatPreviews,
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Account")
            SettingsSection {
                ConnectionRow(state.serverUrl.ifBlank { "Not configured" }, onEditConnection)
            }

            Spacer(Modifier.height(20.dp))
            LogoutRow("Log out", "Clear credentials", viewModel::confirmLogout)

            AnimatedVisibility(
                visible = state.showLogoutConfirm,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HermesChip("Cancel", false, viewModel::dismissLogout)
                        HermesChip(
                            text = "Confirm logout",
                            selected = true,
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            danger = true,
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, danger: Boolean = false) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsSection(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(24.dp),
                containerAlpha = 0.72f,
                borderAlpha = 0.16f,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(vertical = 14.dp),
    )
}

@Composable
private fun SettingItem(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ClickRow(title: String, subtitle: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "Open $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectionRow(url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Connection", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "Copy URL",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun LogoutRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
            contentDescription = "Log out",
            tint = MaterialTheme.colorScheme.error,
        )
    }
}
