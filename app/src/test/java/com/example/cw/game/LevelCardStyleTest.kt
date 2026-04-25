package com.example.cw.game

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCardStyleTest {

    @Test
    fun levelCardPalette_completedUsesBrightReadableColors() {
        val palette = levelCardPalette(unlocked = false, completed = true)

        assertEquals(TextPrimary, palette.titleColor)
        assertEquals(AccentGreen, palette.statusTextColor)
        assertTrue(relativeLuminance(palette.descriptionColor) > relativeLuminance(TextSecond))
    }

    @Test
    fun levelCardPalette_unlockedKeepsDescriptionBrighterThanDefaultSecondaryText() {
        val palette = levelCardPalette(unlocked = true, completed = false)

        assertEquals(TextPrimary, palette.titleColor)
        assertEquals(AccentCyan, palette.statusTextColor)
        assertTrue(relativeLuminance(palette.descriptionColor) > relativeLuminance(TextSecond))
    }

    @Test
    fun levelCardPalette_lockedKeepsTextReadableAgainstLockedBackground() {
        val palette = levelCardPalette(unlocked = false, completed = false)
        val titleContrast = contrastRatio(palette.titleColor, palette.backgroundTop)
        val descriptionContrast = contrastRatio(palette.descriptionColor, palette.backgroundTop)

        assertTrue(titleContrast >= 4.5f)
        assertTrue(descriptionContrast >= 3f)
    }

    @Test
    fun levelCardStatusText_matchesCompletionAndUnlockState() {
        assertEquals(
            "COMPLETED",
            levelCardStatusText(unlockAfterLevelId = 2, unlocked = true, completed = true)
        )
        assertEquals(
            "AVAILABLE",
            levelCardStatusText(unlockAfterLevelId = 2, unlocked = true, completed = false)
        )
        assertEquals(
            "LOCKED - Complete Mission 2",
            levelCardStatusText(unlockAfterLevelId = 2, unlocked = false, completed = false)
        )
    }

    private fun relativeLuminance(color: Color): Float {
        return (0.2126f * color.red) + (0.7152f * color.green) + (0.0722f * color.blue)
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val compositedForeground = compositeOverOpaqueBackground(foreground, background)
        val foregroundLuminance = relativeLuminance(compositedForeground)
        val backgroundLuminance = relativeLuminance(background)
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private fun compositeOverOpaqueBackground(foreground: Color, background: Color): Color {
        val alpha = foreground.alpha
        val inverseAlpha = 1f - alpha
        return Color(
            red = foreground.red * alpha + background.red * inverseAlpha,
            green = foreground.green * alpha + background.green * inverseAlpha,
            blue = foreground.blue * alpha + background.blue * inverseAlpha,
            alpha = 1f
        )
    }
}
