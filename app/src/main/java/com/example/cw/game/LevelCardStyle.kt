package com.example.cw.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

internal data class LevelCardPalette(
    val borderColor: Color,
    val stripeColor: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val titleColor: Color,
    val descriptionColor: Color,
    val statusTextColor: Color,
    val statusContainerColor: Color,
    val statusBorderColor: Color,
    val statusLetterSpacing: TextUnit
)

internal enum class LevelCardState {
    LOCKED,
    AVAILABLE,
    COMPLETED
}

internal fun levelCardState(
    unlocked: Boolean,
    completed: Boolean
): LevelCardState {
    require(unlocked || !completed) { "Completed level cards must also be unlocked." }
    return when {
        completed -> LevelCardState.COMPLETED
        unlocked -> LevelCardState.AVAILABLE
        else -> LevelCardState.LOCKED
    }
}

internal fun levelCardPalette(state: LevelCardState): LevelCardPalette = when (state) {
    LevelCardState.COMPLETED -> LevelCardPalette(
        borderColor = AccentCyan.copy(alpha = 0.35f),
        stripeColor = AccentCyan,
        backgroundTop = LevelCardCompletedTop,
        backgroundBottom = LevelCardCompletedBottom,
        titleColor = TextPrimary,
        descriptionColor = TextPrimary.copy(alpha = 0.82f),
        statusTextColor = AccentGreen,
        statusContainerColor = AccentGreen.copy(alpha = 0.10f),
        statusBorderColor = AccentGreen.copy(alpha = 0.35f),
        statusLetterSpacing = 1.sp
    )

    LevelCardState.AVAILABLE -> LevelCardPalette(
        borderColor = BorderGlow,
        stripeColor = BorderGlow,
        backgroundTop = LevelCardUnlockedTop,
        backgroundBottom = LevelCardUnlockedBottom,
        titleColor = TextPrimary,
        descriptionColor = TextPrimary.copy(alpha = 0.78f),
        statusTextColor = AccentCyan,
        statusContainerColor = AccentCyan.copy(alpha = 0.10f),
        statusBorderColor = AccentCyan.copy(alpha = 0.35f),
        statusLetterSpacing = 1.sp
    )

    LevelCardState.LOCKED -> LevelCardPalette(
        borderColor = BorderDim,
        stripeColor = BorderDim,
        backgroundTop = LevelCardLockedTop,
        backgroundBottom = LevelCardLockedBottom,
        titleColor = TextPrimary.copy(alpha = 0.76f),
        descriptionColor = TextPrimary.copy(alpha = 0.64f),
        statusTextColor = TextPrimary.copy(alpha = 0.82f),
        statusContainerColor = TextSecond.copy(alpha = 0.12f),
        statusBorderColor = BorderDim.copy(alpha = 0.75f),
        statusLetterSpacing = 0.5.sp
    )
}

internal fun levelCardStatusText(
    state: LevelCardState,
    unlockAfterLevelId: Int?
): String = when (state) {
    LevelCardState.COMPLETED -> "COMPLETED"
    LevelCardState.AVAILABLE -> "AVAILABLE"
    LevelCardState.LOCKED -> unlockAfterLevelId?.let { "LOCKED - Complete Mission $it" } ?: "LOCKED"
}
