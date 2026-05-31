package com.hermes.mobile.feature.lab

import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.HermesGlassRole
import com.hermes.mobile.core.settings.hermesGlassTypeForRole
import com.hermes.mobile.core.settings.hermesGlassTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LiquidGlassLabScreenTest {
    @Test
    fun liquidGlassLabTabsMirrorMainAppAreas() {
        assertEquals(
            listOf("Home", "Chat", "Controls", "Settings"),
            liquidGlassPreviewTabs.map { it.label },
        )
    }

    @Test
    fun liquidGlassSettingsClampToUsableRanges() {
        val settings = LiquidGlassConfig(
            blur = -4f,
            refractionHeight = 90f,
            refractionAmount = 120f,
            cornerRadius = 4f,
            surfaceAlpha = 2f,
            dispersion = 4f,
            chromaticAberration = false,
            depthEffect = true,
        ).coerced()

        assertEquals(0f, settings.blur)
        assertEquals(50f, settings.refractionHeight)
        assertEquals(80f, settings.refractionAmount)
        assertEquals(12f, settings.cornerRadius)
        assertEquals(0.75f, settings.surfaceAlpha)
        assertEquals(1f, settings.dispersion)
        assertEquals(false, settings.chromaticAberration)
        assertEquals(true, settings.depthEffect)
    }

    @Test
    fun liquidGlassTypesUseClearUntintedMaterialForTheirRoles() {
        assertEquals(
            listOf("Elastic nav", "Readable panel", "Chat bubble", "Action glass", "Status glass"),
            hermesGlassTypes.map { it.label },
        )
        assertEquals(HermesGlassRole.Navigation, hermesGlassTypeForRole(HermesGlassRole.Navigation).role)
        assertEquals(true, hermesGlassTypeForRole(HermesGlassRole.Navigation).elastic)
        assertEquals(false, hermesGlassTypes.any { it.tintsGlass })
    }
}

