package com.example.cw.game

import androidx.compose.ui.graphics.Color

internal data class LevelCardPalette(
    val borderColor: Color,
    val stripeColor: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val titleColor: Color,
    val descriptionColor: Color,
    val statusTextColor: Color,
    val statusContainerColor: Color,
    val statusBorderColor: Color
)

internal fun levelCardPalette(
    unlocked: Boolean,
    completed: Boolean
): LevelCardPalette = when {
    completed -> LevelCardPalette(
        borderColor = AccentCyan.copy(alpha = 0.35f),
        stripeColor = AccentCyan,
        backgroundTop = LevelCardCompletedTop,
        backgroundBottom = LevelCardCompletedBottom,
        titleColor = TextPrimary,
        descriptionColor = TextPrimary.copy(alpha = 0.82f),
        statusTextColor = AccentGreen,
        statusContainerColor = AccentGreen.copy(alpha = 0.10f),
        statusBorderColor = AccentGreen.copy(alpha = 0.35f)
    )

    unlocked -> LevelCardPalette(
        borderColor = BorderGlow,
        stripeColor = BorderGlow,
        backgroundTop = LevelCardUnlockedTop,
        backgroundBottom = LevelCardUnlockedBottom,
        titleColor = TextPrimary,
        descriptionColor = TextPrimary.copy(alpha = 0.78f),
        statusTextColor = AccentCyan,
        statusContainerColor = AccentCyan.copy(alpha = 0.10f),
        statusBorderColor = AccentCyan.copy(alpha = 0.35f)
    )

    else -> LevelCardPalette(
        borderColor = BorderDim,
        stripeColor = BorderDim,
        backgroundTop = LevelCardLockedTop,
        backgroundBottom = LevelCardLockedBottom,
        titleColor = TextPrimary.copy(alpha = 0.76f),
        descriptionColor = TextPrimary.copy(alpha = 0.64f),
        statusTextColor = TextPrimary.copy(alpha = 0.82f),
        statusContainerColor = TextSecond.copy(alpha = 0.12f),
        statusBorderColor = BorderDim.copy(alpha = 0.75f)
    )
}

internal fun levelCardStatusText(
    unlockAfterLevelId: Int?,
    unlocked: Boolean,
    completed: Boolean
): String = when {
    completed -> "COMPLETED"
    unlocked -> "AVAILABLE"
    else -> "LOCKED - Complete Mission $unlockAfterLevelId"
}
