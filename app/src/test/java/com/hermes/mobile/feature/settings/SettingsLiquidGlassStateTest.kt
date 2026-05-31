package com.hermes.mobile.feature.settings

import com.hermes.mobile.core.settings.LiquidGlassConfig
import com.hermes.mobile.core.settings.VisualStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsLiquidGlassStateTest {
    @Test
    fun settingsStateDefaultsToNormalInterfaceWithDefaultGlassControls() {
        val state = SettingsUiState()

        assertEquals(VisualStyle.Normal, state.visualStyle)
        assertEquals(LiquidGlassConfig(), state.liquidGlassConfig)
    }
}
