package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import com.example.cw.game.levels.LevelDefinition
import com.example.cw.game.levels.WorldBounds
import kotlin.math.max
import kotlin.math.min

internal fun createMatch(level: LevelDefinition): MatchState {
    return MatchState(
        worldBounds = level.worldBounds,
        bases = level.bases.map {
            BaseState(
                id = it.id,
                position = Offset(it.x, it.y),
                owner = it.owner,
                type = it.type,
                units = it.units,
                capLevel = it.capLevel,
                maxLevel = it.maxLevel
            )
        },
        fleets = emptyList(),
        obstacles = level.obstacles.map { Obstacle(Offset(it.x, it.y), it.radius) },
        playerMoney = 0f,
        aiStates = level.aiControllers.associate { controller ->
            controller.owner to AiRuntimeState(
                type = controller.type,
                money = 0f,
                thinkCountdown = ENEMY_AI_THINK_INTERVAL_SECONDS
            )
        },
        nextFleetId = 1,
        selectedBaseIds = emptySet(),
        message = level.introMessage,
        status = MatchStatus.RUNNING,
        levelId = level.levelId,
        levelName = level.name,
        isPaused = false
    )
}

internal fun upgradeCost(base: BaseState): Float = 18f + base.capLevel * 10f

internal fun formatFunds(amount: Float): String = amount.toInt().toString()

internal fun buildRoute(
    start: Offset,
    end: Offset,
    obstacles: List<Obstacle>,
    worldBounds: WorldBounds
): List<Offset> {
    var route = listOf(start, end)
    repeat(3) {
        val obstacle = obstacles.firstOrNull { current ->
            route.zipWithNext().any { (a, b) -> segmentHitsCircle(a, b, current.position, current.radius + 34f) }
        } ?: return@repeat

        val newRoute = mutableListOf<Offset>()
        route.zipWithNext().forEach { (a, b) ->
            newRoute += a
            if (segmentHitsCircle(a, b, obstacle.position, obstacle.radius + 34f)) {
                newRoute += computeDetourPoint(a, b, obstacle, worldBounds)
            }
        }
        newRoute += route.last()
        route = newRoute.distinctBy { "${it.x.roundKey()}-${it.y.roundKey()}" }
    }
    return route.drop(1)
}

private fun computeDetourPoint(
    start: Offset,
    end: Offset,
    obstacle: Obstacle,
    worldBounds: WorldBounds
): Offset {
    val center = obstacle.position
    val padding = obstacle.radius + 58f
    val toStart = normalize(start - center)
    val toEnd = normalize(end - center)
    val candidateA = center + normalize(toStart + toEnd).rotate90() * padding
    val candidateB = center + normalize(toStart + toEnd).rotateMinus90() * padding

    val routeA = distance(start, candidateA) + distance(candidateA, end)
    val routeB = distance(start, candidateB) + distance(candidateB, end)
    val chosen = if (routeA <= routeB) candidateA else candidateB

    return Offset(
        chosen.x.coerceIn(80f, worldBounds.width - 80f),
        chosen.y.coerceIn(120f, worldBounds.height - 120f)
    )
}

private fun segmentHitsCircle(a: Offset, b: Offset, center: Offset, radius: Float): Boolean {
    val ab = b - a
    val t = (((center - a).x * ab.x) + ((center - a).y * ab.y)) /
        max(0.0001f, ab.x * ab.x + ab.y * ab.y)
    val clampedT = t.coerceIn(0f, 1f)
    val closest = a + ab * clampedT
    return distance(closest, center) < radius
}

internal fun scale(size: Size, worldBounds: WorldBounds): Float {
    return min(size.width / worldBounds.width, size.height / worldBounds.height)
}

internal fun worldToScreen(offset: Offset, canvasSize: Size, worldBounds: WorldBounds): Offset {
    val scale = scale(canvasSize, worldBounds)
    val origin = Offset(
        (canvasSize.width - worldBounds.width * scale) / 2f,
        (canvasSize.height - worldBounds.height * scale) / 2f
    )
    return Offset(origin.x + offset.x * scale, origin.y + offset.y * scale)
}

internal fun distance(a: Offset, b: Offset): Float = (a - b).getDistance()

private fun normalize(offset: Offset): Offset {
    val value = offset.getDistance()
    return if (value <= 0.0001f) Offset(1f, 0f) else offset / value
}

internal fun diamondPath(center: Offset, radius: Float): Path {
    return Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
}

internal fun arrowPath(center: Offset, radius: Float): Path {
    return Path().apply {
        moveTo(center.x + radius, center.y)
        lineTo(center.x - radius * 0.45f, center.y - radius * 0.72f)
        lineTo(center.x - radius * 0.16f, center.y)
        lineTo(center.x - radius * 0.45f, center.y + radius * 0.72f)
        close()
    }
}

private fun Offset.rotate90(): Offset = Offset(-y, x)

private fun Offset.rotateMinus90(): Offset = Offset(y, -x)

private fun Float.roundKey(): Int = (this * 10).toInt()
