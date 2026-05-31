package com.hermes.mobile.navigation

import com.hermes.mobile.core.settings.VisualStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LiquidGlassBackdropScopeTest {
    @Test
    fun liquidGlassBackdropIsOnlyEnabledForShellChrome() {
        assertEquals(false, contentBackdropEnabledForStyle(VisualStyle.Normal))
        assertEquals(false, bottomNavBackdropEnabledForStyle(VisualStyle.Normal))

        assertEquals(false, contentBackdropEnabledForStyle(VisualStyle.LiquidGlass))
        assertEquals(true, bottomNavBackdropEnabledForStyle(VisualStyle.LiquidGlass))
    }
}
