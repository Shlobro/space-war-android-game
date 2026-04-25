package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCanvasTest {
    companion object {
        private const val REPRESENTATIVE_SMALL_RENDERED_RADIUS = 12.96f
    }

    private val upgradeNodeFallbackButtonSize = IntSize(
        UPGRADE_NODE_BUTTON_FALLBACK_WIDTH_DP,
        UPGRADE_NODE_BUTTON_FALLBACK_HEIGHT_DP
    )

    @Test
    fun baseLabelLayout_placesLevelBadgeAcrossBottomEdgeOfBase() {
        val baseRadius = 36f
        val layout = baseLabelLayout(baseRadius)

        assertTrue(layout.levelBadgeOffsetFromCenter > layout.unitsOffsetY)
        assertTrue(layout.levelBadgeOffsetFromCenter < baseRadius + layout.levelBadgeRadius)
        assertTrue(layout.levelBadgeOffsetFromCenter + layout.levelBadgeRadius > baseRadius)
    }

    @Test
    fun baseLabelLayout_keepsBottomBadgeReadableForSmallRenderedBase() {
        val layout = baseLabelLayout(REPRESENTATIVE_SMALL_RENDERED_RADIUS)

        assertTrue(layout.levelBadgeOffsetFromCenter > layout.unitsOffsetY)
        assertTrue(layout.levelBadgeRadius >= 9f)
        assertTrue(layout.levelBadgeStrokeWidth >= 2f)
        assertTrue(layout.levelBadgeOffsetFromCenter + layout.levelBadgeRadius > REPRESENTATIVE_SMALL_RENDERED_RADIUS)
        assertTrue(layout.levelBadgeOffsetFromCenter < REPRESENTATIVE_SMALL_RENDERED_RADIUS + layout.levelBadgeRadius)
    }

    @Test
    fun upgradeIndicatorLayout_placesArrowAboveBaseWithReadableSize() {
        val layout = upgradeIndicatorLayout(baseRadius = 36f)

        assertTrue(layout.offsetFromCenter < 0f)
        assertTrue(layout.halfHeight >= 7f)
        assertTrue(layout.halfWidth >= 5.5f)
        assertTrue(layout.strokeWidth >= 1.5f)
    }

    @Test
    fun resolveUpgradeIndicatorCenterY_keepsPreferredPositionWhenArrowFitsOnScreen() {
        val layout = upgradeIndicatorLayout(baseRadius = 36f)

        val centerY = resolveUpgradeIndicatorCenterY(
            baseCenterY = 300f,
            indicatorLayout = layout
        )

        assertEquals(300f + layout.offsetFromCenter, centerY, 0.001f)
    }

    @Test
    fun resolveUpgradeIndicatorCenterY_clampsArrowInsideTopCanvasEdge() {
        val layout = upgradeIndicatorLayout(baseRadius = 36f)

        val centerY = resolveUpgradeIndicatorCenterY(
            baseCenterY = 20f,
            indicatorLayout = layout
        )

        assertEquals(layout.halfHeight + 2f, centerY, 0.001f)
    }

    @Test
    fun showUpgradeIndicator_requiresAffordablePlayerBaseBelowMaxLevel() {
        val upgradeablePlayerBase = BaseState(
            id = 1,
            position = Offset.Zero,
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )

        assertTrue(showUpgradeIndicator(upgradeablePlayerBase, playerMoney = 30f))
        assertFalse(showUpgradeIndicator(upgradeablePlayerBase, playerMoney = 29f))
        assertFalse(showUpgradeIndicator(upgradeablePlayerBase.copy(maxLevel = 2), playerMoney = 30f))
        assertFalse(showUpgradeIndicator(upgradeablePlayerBase.copy(owner = Owner.AI_1), playerMoney = 30f))
    }

    @Test
    fun levelBadgeTextSize_usesDensityAwareMinimumAndMaximumBounds() {
        val smallLayout = baseLabelLayout(baseRadius = REPRESENTATIVE_SMALL_RENDERED_RADIUS)
        val mediumLayout = baseLabelLayout(baseRadius = 36f)
        val minTextSizePx = 10f
        val maxTextSizePx = 30f

        val smallTextSize = levelBadgeTextSize(smallLayout, minTextSizePx, maxTextSizePx)
        val mediumTextSize = levelBadgeTextSize(mediumLayout, minTextSizePx, maxTextSizePx)

        assertTrue(smallTextSize > minTextSizePx)
        assertTrue(smallTextSize < maxTextSizePx)
        assertTrue(mediumTextSize > smallTextSize)
        assertTrue(mediumTextSize < maxTextSizePx)
    }

    @Test
    fun levelBadgeTextSize_capsAtProvidedMaximumForLargeBases() {
        val layout = baseLabelLayout(baseRadius = 80f)

        assertEquals(15f, levelBadgeTextSize(layout, minTextSizePx = 10f, maxTextSizePx = 15f), 0.001f)
    }

    @Test
    fun resolveLevelBadgeCenterY_keepsPreferredPositionWhenBadgeFitsOnScreen() {
        val layout = baseLabelLayout(baseRadius = 36f)

        val centerY = resolveLevelBadgeCenterY(
            baseCenterY = 300f,
            canvasHeight = 1000f,
            labelLayout = layout
        )

        assertEquals(300f + layout.levelBadgeOffsetFromCenter, centerY, 0.001f)
    }

    @Test
    fun resolveLevelBadgeCenterY_clampsBadgeInsideBottomCanvasEdge() {
        val layout = baseLabelLayout(baseRadius = 36f)

        val centerY = resolveLevelBadgeCenterY(
            baseCenterY = 996f,
            canvasHeight = 1000f,
            labelLayout = layout
        )

        assertEquals(1000f - layout.levelBadgeRadius - 2f, centerY, 0.001f)
    }

    @Test
    fun resolveLevelBadgeCenterY_keepsBadgeVisibleInTinyViewport() {
        val layout = baseLabelLayout(baseRadius = REPRESENTATIVE_SMALL_RENDERED_RADIUS)

        val centerY = resolveLevelBadgeCenterY(
            baseCenterY = 40f,
            canvasHeight = 10f,
            labelLayout = layout
        )

        assertTrue(centerY >= layout.levelBadgeRadius + 2f)
    }

    @Test
    fun upgradeNodeButtonOffset_clampsOverlayInsideTopRightEdge() {
        val offset = upgradeNodeButtonOffset(
            center = Offset(982f, 20f),
            radius = 24f,
            viewportSize = IntSize(1000, 1000),
            buttonSize = upgradeNodeFallbackButtonSize,
            baseMarginPx = 8,
            horizontalGapPx = 6,
            verticalGapPx = 4
        )

        assertEquals(888, offset.x)
        assertEquals(8, offset.y)
    }

    @Test
    fun upgradeNodeButtonOffset_keepsPreferredPositionWhenThereIsRoom() {
        val offset = upgradeNodeButtonOffset(
            center = Offset(300f, 500f),
            radius = 24f,
            viewportSize = IntSize(1000, 1000),
            buttonSize = upgradeNodeFallbackButtonSize,
            baseMarginPx = 8,
            horizontalGapPx = 6,
            verticalGapPx = 4
        )

        assertEquals(330, offset.x)
        assertEquals(472, offset.y)
    }

    @Test
    fun upgradeNodeButtonOffset_clampsOverlayInsideBottomEdge() {
        val offset = upgradeNodeButtonOffset(
            center = Offset(300f, 995f),
            radius = 24f,
            viewportSize = IntSize(1000, 1000),
            buttonSize = upgradeNodeFallbackButtonSize,
            baseMarginPx = 8,
            horizontalGapPx = 6,
            verticalGapPx = 4
        )

        assertEquals(330, offset.x)
        assertEquals(952, offset.y)
    }

    @Test
    fun upgradeNodeButtonOffset_clampsOverlayInsideLeftSafeInset() {
        val offset = upgradeNodeButtonOffset(
            center = Offset(20f, 500f),
            radius = 24f,
            viewportSize = IntSize(1000, 1000),
            buttonSize = upgradeNodeFallbackButtonSize,
            baseMarginPx = 8,
            horizontalGapPx = 6,
            verticalGapPx = 4,
            edgeInsets = EdgeInsets(left = 80)
        )

        assertEquals(88, offset.x)
        assertEquals(472, offset.y)
    }

    @Test
    fun upgradeNodeButtonOffset_handlesSmallViewportByPinningToVisibleBounds() {
        val offset = upgradeNodeButtonOffset(
            center = Offset(20f, 20f),
            radius = 16f,
            viewportSize = IntSize(80, 70),
            buttonSize = upgradeNodeFallbackButtonSize,
            baseMarginPx = 8,
            horizontalGapPx = 6,
            verticalGapPx = 4
        )

        assertEquals(8, offset.x)
        assertEquals(8, offset.y)
    }

    @Test
    fun upgradeNodeButtonOffset_respectsSafeDrawingInsets() {
        val offset = upgradeNodeButtonOffset(
            center = Offset(982f, 20f),
            radius = 24f,
            viewportSize = IntSize(1000, 1000),
            buttonSize = upgradeNodeFallbackButtonSize,
            baseMarginPx = 8,
            horizontalGapPx = 6,
            verticalGapPx = 4,
            edgeInsets = EdgeInsets(top = 32, right = 24, bottom = 48)
        )

        assertEquals(864, offset.x)
        assertEquals(40, offset.y)
    }
}
