package com.hermes.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.frostedGlass(
    colors: ColorScheme,
    shape: Shape,
    containerAlpha: Float = 0.72f,
    borderAlpha: Float = 0.18f,
): Modifier {
    return clip(shape)
        .background(colors.surface.copy(alpha = containerAlpha))
        .border(1.dp, colors.outline.copy(alpha = borderAlpha), shape)
}
