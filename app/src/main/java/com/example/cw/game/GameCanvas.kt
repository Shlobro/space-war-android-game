package com.example.cw.game

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

private const val LEVEL_BADGE_EDGE_OVERLAP_RATIO = 0.15f
private const val LEVEL_BADGE_VIEWPORT_MARGIN_PX = 2f
private const val LEVEL_BADGE_TEXT_BASELINE_OFFSET_RATIO = 0.33f

@Composable
internal fun GameCanvas(state: MatchState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 14.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
    }
    val levelPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.argb(235, 255, 255, 255)
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
    }
    val selectedPaint = remember(density) {
        Paint(labelPaint).apply {
            textSize = with(density) { 10.sp.toPx() }
        }
    }
    val fleetPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier) {
        drawStarfield(state)
        state.obstacles.forEach { obstacle -> drawObstacle(obstacle, state) }
        state.bases.forEach { base -> drawBaseAura(base, base.id in state.selectedBaseIds, state) }
        drawFleetTrails(state)
        state.bases.forEach { base ->
            drawBase(base, labelPaint, levelPaint, selectedPaint, base.id in state.selectedBaseIds, state)
        }
        state.fleets.forEach { fleet -> drawFleet(fleet, fleetPaint, state) }
    }
}

private fun DrawScope.drawStarfield(state: MatchState) {
    val stars = listOf(
        Offset(80f, 120f), Offset(240f, 210f), Offset(480f, 140f), Offset(720f, 260f),
        Offset(910f, 180f), Offset(160f, 420f), Offset(600f, 380f), Offset(870f, 520f),
        Offset(200f, 740f), Offset(760f, 780f), Offset(120f, 1120f), Offset(510f, 1280f),
        Offset(880f, 1460f)
    )
    stars.forEachIndexed { index, offset ->
        drawCircle(
            color = if (index % 3 == 0) Color(0x99FFFFFF) else Color(0x66BEE8FF),
            radius = if (index % 4 == 0) 3.6f else 2.1f,
            center = worldToScreen(offset, size, state.worldBounds)
        )
    }
}

private fun DrawScope.drawObstacle(obstacle: Obstacle, state: MatchState) {
    val center = worldToScreen(obstacle.position, size, state.worldBounds)
    val radius = obstacle.radius * scale(size, state.worldBounds)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF283543), Color(0xFF111A23), Color(0xAA0B1117)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0x5587A3BE),
        radius = radius + 8f,
        center = center,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawBaseAura(base: BaseState, selected: Boolean, state: MatchState) {
    val center = worldToScreen(base.position, size, state.worldBounds)
    val baseRadius = base.radius * scale(size, state.worldBounds)
    if (selected) {
        drawCircle(
            color = Color(0x22FFF3BF),
            radius = baseRadius + 28f,
            center = center
        )
        drawCircle(
            color = Color(0xFFF6CB7D),
            radius = baseRadius + 12f,
            center = center,
            style = Stroke(width = 5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = baseRadius + 20f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawBase(
    base: BaseState,
    labelPaint: Paint,
    levelPaint: Paint,
    selectedPaint: Paint,
    selected: Boolean,
    state: MatchState
) {
    val center = worldToScreen(base.position, size, state.worldBounds)
    val baseRadius = base.radius * scale(size, state.worldBounds)
    val labelLayout = baseLabelLayout(baseRadius)
    val levelBadgeCenterY = resolveLevelBadgeCenterY(
        baseCenterY = center.y,
        canvasHeight = size.height,
        labelLayout = labelLayout
    )
    val fillColor = base.owner.color

    if (base.type == BaseType.FAST) {
        drawPath(
            path = diamondPath(center, baseRadius),
            brush = Brush.radialGradient(
                listOf(
                    if (selected) fillColor.copy(alpha = 1f) else fillColor.copy(alpha = 0.95f),
                    fillColor.copy(alpha = if (selected) 0.68f else 0.42f),
                    Color(0xAA0D1621)
                ),
                center = center,
                radius = baseRadius
            )
        )
        drawPath(
            path = diamondPath(center, baseRadius),
            color = if (selected) Color(0xFFF6CB7D) else base.owner.color.copy(alpha = 0.95f),
            style = Stroke(width = 4f)
        )
    } else {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (selected) fillColor.copy(alpha = 1f) else fillColor.copy(alpha = 0.95f),
                    fillColor.copy(alpha = if (selected) 0.68f else 0.42f),
                    Color(0xAA0D1621)
                ),
                center = center,
                radius = baseRadius
            ),
            radius = baseRadius,
            center = center
        )
        drawCircle(
            color = if (selected) Color(0xFFF6CB7D) else base.owner.color.copy(alpha = 0.95f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 4f)
        )
    }

    drawContext.canvas.nativeCanvas.drawText(
        base.units.toInt().toString(),
        center.x,
        center.y + labelLayout.unitsOffsetY,
        labelPaint
    )
    drawCircle(
        color = Color(0xFF0E1722),
        radius = labelLayout.levelBadgeRadius,
        center = Offset(center.x, levelBadgeCenterY)
    )
    drawCircle(
        color = if (selected) Color(0xFFF6CB7D) else Color.White.copy(alpha = 0.92f),
        radius = labelLayout.levelBadgeRadius,
        center = Offset(center.x, levelBadgeCenterY),
        style = Stroke(width = 2f)
    )
    drawContext.canvas.nativeCanvas.drawText(
        base.capLevel.toString(),
        center.x,
        // Paint draws text from the baseline, so nudging down by ~1/3 text size
        // centers the numeral visually inside the circular badge.
        levelBadgeCenterY + (levelPaint.textSize * LEVEL_BADGE_TEXT_BASELINE_OFFSET_RATIO),
        levelPaint
    )
    if (selected) {
        drawContext.canvas.nativeCanvas.drawText(
            "SELECTED",
            center.x,
            center.y + labelLayout.selectedOffsetY,
            selectedPaint
        )
    }
}

private fun DrawScope.drawFleetTrails(state: MatchState) {
    val canvasSize = size
    state.fleets.forEach { fleet ->
        val color = fleet.owner.color.copy(alpha = 0.3f)
        val points = buildList {
            add(worldToScreen(fleet.position, canvasSize, state.worldBounds))
            fleet.path.drop(fleet.pathIndex).forEach {
                add(worldToScreen(it, canvasSize, state.worldBounds))
            }
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path = path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawFleet(fleet: FleetState, fleetPaint: Paint, state: MatchState) {
    val center = worldToScreen(fleet.position, size, state.worldBounds)
    val radius = (6f + fleet.units * 0.16f).coerceIn(7f, 18f)
    if (fleet.type == BaseType.FAST) {
        drawPath(
            path = arrowPath(center, radius),
            color = fleet.owner.color.copy(alpha = 0.92f)
        )
    } else {
        drawCircle(
            color = fleet.owner.color.copy(alpha = 0.9f),
            radius = radius,
            center = center
        )
    }
    drawContext.canvas.nativeCanvas.drawText(
        fleet.units.toInt().toString(),
        center.x,
        center.y + 4f,
        fleetPaint
    )
}

internal data class BaseLabelLayout(
    val unitsOffsetY: Float,
    val levelBadgeOffsetFromCenter: Float,
    val levelBadgeRadius: Float,
    val selectedOffsetY: Float
)

internal fun baseLabelLayout(baseRadius: Float): BaseLabelLayout {
    val unitsOffsetY = (baseRadius * 0.08f).coerceIn(2f, 5f)
    val levelBadgeRadius = (baseRadius * 0.28f).coerceIn(7f, 12f)
    val levelBadgeOffsetFromCenter = baseRadius - (levelBadgeRadius * LEVEL_BADGE_EDGE_OVERLAP_RATIO)
    return BaseLabelLayout(
        unitsOffsetY = unitsOffsetY,
        levelBadgeOffsetFromCenter = levelBadgeOffsetFromCenter,
        levelBadgeRadius = levelBadgeRadius,
        selectedOffsetY = -baseRadius - 16f
    )
}

internal fun resolveLevelBadgeCenterY(
    baseCenterY: Float,
    canvasHeight: Float,
    labelLayout: BaseLabelLayout
): Float {
    val preferredCenterY = baseCenterY + labelLayout.levelBadgeOffsetFromCenter
    val minCenterY = labelLayout.levelBadgeRadius + LEVEL_BADGE_VIEWPORT_MARGIN_PX
    val maxCenterY = (canvasHeight - labelLayout.levelBadgeRadius - LEVEL_BADGE_VIEWPORT_MARGIN_PX)
        .coerceAtLeast(minCenterY)
    return preferredCenterY.coerceIn(minCenterY, maxCenterY)
}
