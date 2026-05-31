package com.hermes.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hermes.mobile.core.settings.HermesGlassRole

@Composable
fun HermesCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    background: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    animated: Boolean = false,
    frosted: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (animated) {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            if (pressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "pressScale",
        )
        Box(
            modifier = modifier
                .size(size)
                .minimumInteractiveComponentSize()
                .scale(scale)
                .clip(CircleShape)
                .background(background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    } else {
        val baseModifier = if (frosted) {
            modifier
                .size(size)
                .minimumInteractiveComponentSize()
                .hermesGlass(
                    shape = CircleShape,
                    role = HermesGlassRole.Action,
                    normalContainerAlpha = 0.72f,
                    normalBorderAlpha = 0.18f,
                )
        } else {
            modifier
                .size(size)
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(background)
        }
        IconButton(
            onClick = onClick,
            modifier = baseModifier,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}
