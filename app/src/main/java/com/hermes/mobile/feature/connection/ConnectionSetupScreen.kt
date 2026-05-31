package com.hermes.mobile.feature.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hermes.mobile.R
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.ui.components.hermesGlass

@Composable
fun ConnectionSetupScreen(
    onContinue: () -> Unit,
    onCancel: (() -> Unit)? = null,
    viewModel: ConnectionSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(72.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = "Hermes connection",
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (state.isEditingExistingConnection) "Edit connection" else "Connect Hermes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                Text(
                    text = if (state.isEditingExistingConnection) "Update endpoint or keep the current key." else "Set endpoint and optional key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            onCancel?.let { cancel ->
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable(onClick = cancel)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hermesGlass(
                    shape = RoundedCornerShape(26.dp),
                    role = HermesGlassRole.ReadablePanel,
                    normalContainerAlpha = 0.56f,
                    normalBorderAlpha = 0.12f,
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                "Server details",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            HermesTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChanged,
                label = "Server URL",
                keyboardType = KeyboardType.Uri,
            )
            Spacer(Modifier.height(12.dp))
            HermesTextField(
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChanged,
                label = "API key (optional)",
                keyboardType = KeyboardType.Password,
                secret = true,
            )
            if (state.isEditingExistingConnection) {
                Text(
                    text = "Leave blank to keep current key",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                state.error?.let {
                    val errorComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.error_shake))
                    val errorProgress by animateLottieCompositionAsState(errorComp, isPlaying = true)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.errorContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        LottieAnimation(
                            errorComp,
                            { errorProgress },
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        )
                        Text(
                            text = it,
                            color = colors.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = state.isHealthy,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                val successComp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
                val successProgress by animateLottieCompositionAsState(successComp, isPlaying = true)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    LottieAnimation(
                        successComp,
                        { successProgress },
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    )
                    Text(
                        text = "Connection verified",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        val haptic = LocalHapticFeedback.current
        val buttonInteraction = remember { MutableInteractionSource() }
        val btnPressed by buttonInteraction.collectIsPressedAsState()
        val btnScale by animateFloatAsState(
            targetValue = if (btnPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
            label = "btnScale",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .graphicsLayer { scaleX = btnScale; scaleY = btnScale }
                .clip(RoundedCornerShape(50.dp))
                .background(
                    if (state.isChecking) colors.primary.copy(alpha = 0.6f) else colors.primary,
                )
                .clickable(
                    enabled = !state.isChecking,
                    interactionSource = buttonInteraction,
                    indication = null,
                    onClickLabel = if (state.isEditingExistingConnection) "Save connection" else "Connect",
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.checkAndSave(onContinue)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (state.isChecking) {
                    "Checking..."
                } else if (state.isEditingExistingConnection) {
                    "Save connection"
                } else {
                    "Connect"
                },
                color = colors.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Text(
            "Add API key only if your server requires authentication.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HermesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    secret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.62f),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

