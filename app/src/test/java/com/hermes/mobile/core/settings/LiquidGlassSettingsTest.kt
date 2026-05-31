package com.hermes.mobile.core.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LiquidGlassSettingsTest {
    @Test
    fun visualStylesExposeNormalAndLiquidGlassOptions() {
        assertEquals(
            listOf("Normal", "Liquid Glass"),
            VisualStyle.entries.map { it.label },
        )
    }

    @Test
    fun liquidGlassConfigClampsToUsableOfficialRanges() {
        val config = LiquidGlassConfig(
            blur = -4f,
            refractionHeight = 90f,
            refractionAmount = 120f,
            cornerRadius = 4f,
            surfaceAlpha = 2f,
            dispersion = 4f,
            chromaticAberration = false,
            depthEffect = true,
            navElasticity = 4f,
        ).coerced()

        assertEquals(0f, config.blur)
        assertEquals(50f, config.refractionHeight)
        assertEquals(80f, config.refractionAmount)
        assertEquals(12f, config.cornerRadius)
        assertEquals(0.75f, config.surfaceAlpha)
        assertEquals(1f, config.dispersion)
        assertEquals(false, config.chromaticAberration)
        assertEquals(true, config.depthEffect)
        assertEquals(1.5f, config.navElasticity)
    }

    @Test
    fun defaultLiquidGlassConfigIsClearAndUntinted() {
        val config = LiquidGlassConfig()

        assertFalse(config.tintsGlass)
        assertEquals(0f, config.themeTintAlpha)
    }
}
