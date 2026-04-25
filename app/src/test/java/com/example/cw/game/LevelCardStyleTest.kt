package com.example.cw.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelCardStyleTest {

    @Test
    fun levelCardPalette_completedUsesBrightReadableColors() {
        val palette = levelCardPalette(LevelCardState.COMPLETED)

        assertEquals(TextPrimary, palette.titleColor)
        assertEquals(AccentGreen, palette.statusTextColor)
        assertEquals(1.sp, palette.statusLetterSpacing)
        assertTrue(relativeLuminance(palette.descriptionColor) > relativeLuminance(TextSecond))
    }

    @Test
    fun levelCardPalette_unlockedKeepsDescriptionBrighterThanDefaultSecondaryText() {
        val palette = levelCardPalette(LevelCardState.AVAILABLE)

        assertEquals(TextPrimary, palette.titleColor)
        assertEquals(AccentCyan, palette.statusTextColor)
        assertEquals(1.sp, palette.statusLetterSpacing)
        assertTrue(relativeLuminance(palette.descriptionColor) > relativeLuminance(TextSecond))
    }

    @Test
    fun levelCardPalette_lockedKeepsTextReadableAgainstLockedBackground() {
        val palette = levelCardPalette(LevelCardState.LOCKED)
        val titleContrast = contrastRatio(palette.titleColor, palette.backgroundTop)
        val descriptionContrast = contrastRatio(palette.descriptionColor, palette.backgroundTop)

        assertTrue(titleContrast >= 4.5f)
        assertTrue(descriptionContrast >= 3f)
        assertEquals(0.5.sp, palette.statusLetterSpacing)
    }

    @Test
    fun levelCardStatusText_matchesCompletionAndUnlockState() {
        assertEquals(
            "COMPLETED",
            levelCardStatusText(state = LevelCardState.COMPLETED, unlockAfterLevelId = 2)
        )
        assertEquals(
            "AVAILABLE",
            levelCardStatusText(state = LevelCardState.AVAILABLE, unlockAfterLevelId = 2)
        )
        assertEquals(
            "LOCKED - Complete Mission 2",
            levelCardStatusText(state = LevelCardState.LOCKED, unlockAfterLevelId = 2)
        )
    }

    @Test
    fun levelCardStatusText_lockedWithoutPrerequisiteFallsBackToGenericLabel() {
        assertEquals(
            "LOCKED",
            levelCardStatusText(state = LevelCardState.LOCKED, unlockAfterLevelId = null)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun levelCardState_rejectsCompletedWithoutUnlockedState() {
        levelCardState(unlocked = false, completed = true)
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
