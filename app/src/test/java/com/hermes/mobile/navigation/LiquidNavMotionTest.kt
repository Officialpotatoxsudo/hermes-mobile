package com.hermes.mobile.navigation

import com.hermes.mobile.core.settings.LiquidGlassConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LiquidNavMotionTest {
    @Test
    fun liquidNavMotionStretchesAndFloatsFromScrollPressure() {
        val motion = liquidNavMotion(
            scrollPressure = 1f,
            dragPressure = 0f,
            config = LiquidGlassConfig(navElasticity = 1f),
        )

        assertEquals(1.026f, motion.scaleX)
        assertEquals(0.968f, motion.scaleY)
        assertEquals(-8f, motion.translationY)
    }

    @Test
    fun liquidNavMotionClampsExtremeScrollAndDragPressure() {
        val motion = liquidNavMotion(
            scrollPressure = 20f,
            dragPressure = 10f,
            config = LiquidGlassConfig(navElasticity = 1.5f),
        )

        assertEquals(1.078f, motion.scaleX)
        assertEquals(0.904f, motion.scaleY)
        assertEquals(-24f, motion.translationY)
    }
}
