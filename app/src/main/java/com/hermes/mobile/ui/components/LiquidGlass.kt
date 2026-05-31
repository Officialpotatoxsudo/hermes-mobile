package com.hermes.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.core.settings.HermesGlassType
import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.VisualStyle
import com.hermes.mobile.core.settings.hermesGlassTypeForRole
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

val LocalHermesVisualStyle = staticCompositionLocalOf { VisualStyle.Normal }
val LocalHermesLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }
val LocalHermesLiquidGlassBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
fun Modifier.hermesGlass(
    shape: Shape,
    role: HermesGlassRole = HermesGlassRole.ReadablePanel,
    colors: ColorScheme = MaterialTheme.colorScheme,
    normalContainerAlpha: Float = 0.72f,
    normalBorderAlpha: Float = 0.16f,
): Modifier {
    val style = LocalHermesVisualStyle.current
    val config = LocalHermesLiquidGlassConfig.current.coerced()
    val backdrop = LocalHermesLiquidGlassBackdrop.current
    if (style != VisualStyle.LiquidGlass) {
        return frostedGlass(
            colors = colors,
            shape = shape,
            containerAlpha = normalContainerAlpha,
            borderAlpha = normalBorderAlpha,
        )
    }
    return liquidGlassSurface(
        backdrop = backdrop,
        config = config,
        shape = shape,
        type = hermesGlassTypeForRole(role),
    )
}

@Composable
fun HermesGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    role: HermesGlassRole = HermesGlassRole.ReadablePanel,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.hermesGlass(
            shape = shape,
            role = role,
        ),
    ) {
        content()
    }
}

fun Modifier.liquidGlassSurface(
    backdrop: Backdrop?,
    config: LiquidGlassConfig,
    shape: Shape,
    type: HermesGlassType = hermesGlassTypeForRole(HermesGlassRole.ReadablePanel),
): Modifier {
    val clean = config.forType(type)
    return if (backdrop == null) {
        clip(shape)
            .background(Color.White.copy(alpha = clean.surfaceAlpha))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
    } else {
        clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(clean.blur.dp.toPx())
                    lens(
                        refractionHeight = clean.refractionHeight.dp.toPx(),
                        refractionAmount = clean.refractionAmount.dp.toPx(),
                        depthEffect = clean.depthEffect,
                        chromaticAberration = clean.chromaticAberration,
                    )
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = clean.surfaceAlpha))
                    drawRect(Color.White.copy(alpha = 0.035f))
                },
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
    }
}

private fun LiquidGlassConfig.forType(type: HermesGlassType): LiquidGlassConfig {
    return copy(
        blur = blur * type.blurScale,
        refractionHeight = refractionHeight * type.refractionHeightScale,
        refractionAmount = refractionAmount * type.refractionAmountScale,
        cornerRadius = cornerRadius + type.radiusDelta,
        surfaceAlpha = surfaceAlpha * type.surfaceAlphaScale,
        tintsGlass = false,
        themeTintAlpha = 0f,
    ).coerced()
}
