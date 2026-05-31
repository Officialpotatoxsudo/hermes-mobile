package com.hermes.mobile.core.settings

import kotlinx.serialization.Serializable

enum class VisualStyle(val label: String) {
    Normal("Normal"),
    LiquidGlass("Liquid Glass"),
}

@Serializable
data class LiquidGlassConfig(
    val blur: Float = 5f,
    val refractionHeight: Float = 22f,
    val refractionAmount: Float = 44f,
    val cornerRadius: Float = 30f,
    val surfaceAlpha: Float = 0.24f,
    val dispersion: Float = 0.62f,
    val chromaticAberration: Boolean = true,
    val depthEffect: Boolean = true,
    val navElasticity: Float = 1f,
    val tintsGlass: Boolean = false,
    val themeTintAlpha: Float = 0f,
) {
    fun coerced(): LiquidGlassConfig {
        return copy(
            blur = blur.coerceIn(0f, 24f),
            refractionHeight = refractionHeight.coerceIn(12f, 50f),
            refractionAmount = refractionAmount.coerceIn(18f, 80f),
            cornerRadius = cornerRadius.coerceIn(12f, 44f),
            surfaceAlpha = surfaceAlpha.coerceIn(0f, 0.75f),
            dispersion = dispersion.coerceIn(0f, 1f),
            navElasticity = navElasticity.coerceIn(0.4f, 1.5f),
            tintsGlass = false,
            themeTintAlpha = 0f,
        )
    }
}
