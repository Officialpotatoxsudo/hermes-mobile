package com.hermes.mobile.feature.chat

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatBubbleStyleTest {
    @Test
    fun assistantBubblePaletteSeparatesBubbleFromPageBackground() {
        val darkScheme = darkColorScheme(
            background = Color.Black,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceContainerHigh = Color(0xFF141414),
            outlineVariant = Color(0xFF2A2A2A),
        )
        val lightScheme = lightColorScheme(
            background = Color.White,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceContainerHigh = Color(0xFFEFEFEF),
            outlineVariant = Color(0xFFD6D6D6),
        )

        listOf(darkScheme, lightScheme).forEach { colors ->
            val palette = assistantBubblePalette(colors)

            assertNotEquals(colors.background, palette.container)
            assertEquals(colors.onSurface, palette.content)
            assertTrue(palette.border.alpha >= 0.32f)
        }
    }

    @Test
    fun outgoingBubblePaletteUsesThemePrimaryContrastPair() {
        val colors = darkColorScheme(
            primary = Color.White,
            primaryContainer = Color(0xFF1A1A1A),
            onPrimaryContainer = Color.White,
            outline = Color(0xFF2A2A2A),
        )

        val palette = outgoingBubblePalette(colors)

        assertEquals(colors.primaryContainer, palette.container)
        assertEquals(colors.onPrimaryContainer, palette.content)
        assertTrue(palette.border.alpha >= 0.22f)
        assertTrue(contrastRatio(palette.container, palette.content) >= 4.5f)
    }

    @Test
    fun chatBubbleMaxWidthKeepsShortMessagesDynamicAndLongMessagesReadable() {
        assertEquals(280.dp, chatBubbleMaxWidth(400.dp, isOutgoing = true))
        assertEquals(312.dp, chatBubbleMaxWidth(400.dp, isOutgoing = false))
        assertEquals(440.dp, chatBubbleMaxWidth(900.dp, isOutgoing = true))
        assertEquals(560.dp, chatBubbleMaxWidth(900.dp, isOutgoing = false))
    }

    private fun contrastRatio(background: Color, foreground: Color): Float {
        val backgroundLuminance = background.relativeLuminance()
        val foregroundLuminance = foreground.relativeLuminance()
        val lighter = maxOf(backgroundLuminance, foregroundLuminance)
        val darker = minOf(backgroundLuminance, foregroundLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun Color.relativeLuminance(): Float {
        fun channel(value: Float): Float {
            return if (value <= 0.03928f) {
                value / 12.92f
            } else {
                java.lang.Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
            }
        }
        return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
    }
}
