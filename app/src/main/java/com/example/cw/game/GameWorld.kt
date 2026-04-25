package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import com.example.cw.game.levels.LevelDefinition
import com.example.cw.game.levels.WorldBounds
import java.util.PriorityQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val ROUTE_COLLISION_PADDING = 34f
private const val ROUTE_WAYPOINT_PADDING = 64f
private const val ROUTE_RING_POINT_COUNT = 16
private const val ROUTE_WORLD_SIDE_MARGIN = 80f
private const val ROUTE_WORLD_VERTICAL_MARGIN = 120f

internal fun createMatch(
    level: LevelDefinition,
    playerShipProductionMultiplier: Float = 1f,
    playerFleetSpeedMultiplier: Float = 1f,
    selectedSpecialAbility: SpecialAbilityType? = null,
    selectedSpecialAbilityLevel: Int = 0
): MatchState {
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
        message = "",
        messageExpiresAtSeconds = null,
        status = MatchStatus.RUNNING,
        levelId = level.levelId,
        levelName = level.name,
        starThresholds = level.starThresholds,
        elapsedSeconds = 0f,
        isPaused = false,
        playerShipProductionMultiplier = playerShipProductionMultiplier,
        playerFleetSpeedMultiplier = playerFleetSpeedMultiplier,
        selectedSpecialAbility = selectedSpecialAbility,
        selectedSpecialAbilityLevel = selectedSpecialAbilityLevel
    )
}

internal fun upgradeCost(base: BaseState): Float = 10f * (base.capLevel + 1)

internal fun formatFunds(amount: Float): String = amount.toInt().toString()

internal fun buildRoute(
    start: Offset,
    end: Offset,
    obstacles: List<Obstacle>,
    worldBounds: WorldBounds
): List<Offset> {
    if (obstacles.none { obstacle ->
            segmentHitsCircle(start, end, obstacle.position, obstacle.radius + ROUTE_COLLISION_PADDING)
        }
    ) {
        return listOf(end)
    }

    val nodes = buildList {
        add(RouteNode(start))
        add(RouteNode(end))
        obstacles.forEachIndexed { obstacleIndex, obstacle ->
            addAll(buildRouteRing(obstacleIndex, obstacle, worldBounds))
        }
    }
    val graph = Array(nodes.size) { mutableListOf<RouteEdge>() }
    for (i in nodes.indices) {
        for (j in i + 1 until nodes.size) {
            if (!canConnect(nodes[i], nodes[j], obstacles)) {
                continue
            }
            val distance = distance(nodes[i].position, nodes[j].position)
            graph[i] += RouteEdge(j, distance)
            graph[j] += RouteEdge(i, distance)
        }
    }

    val nodePath = shortestPath(graph, nodes.size)
    if (nodePath.isEmpty()) {
        return listOf(end)
    }
    return nodePath.drop(1).map { nodes[it].position }
}

private fun buildRouteRing(
    obstacleIndex: Int,
    obstacle: Obstacle,
    worldBounds: WorldBounds
): List<RouteNode> {
    val radius = obstacle.radius + ROUTE_WAYPOINT_PADDING
    return List(ROUTE_RING_POINT_COUNT) { ringIndex ->
        val angle = 2.0 * PI * ringIndex / ROUTE_RING_POINT_COUNT
        val rawPoint = Offset(
            x = obstacle.position.x + (cos(angle) * radius).toFloat(),
            y = obstacle.position.y + (sin(angle) * radius).toFloat()
        )
        RouteNode(
            position = clampToWorld(rawPoint, worldBounds),
            obstacleIndex = obstacleIndex,
            ringIndex = ringIndex
        )
    }
}

private fun canConnect(
    from: RouteNode,
    to: RouteNode,
    obstacles: List<Obstacle>
): Boolean {
    if (from.obstacleIndex != null && from.obstacleIndex == to.obstacleIndex) {
        if (!areNeighboringRingNodes(from, to)) {
            return false
        }
    }

    return obstacles.none { obstacle ->
        segmentHitsCircle(
            from.position,
            to.position,
            obstacle.position,
            obstacle.radius + ROUTE_COLLISION_PADDING
        )
    }
}

private fun areNeighboringRingNodes(a: RouteNode, b: RouteNode): Boolean {
    val first = a.ringIndex ?: return false
    val second = b.ringIndex ?: return false
    val delta = kotlin.math.abs(first - second)
    return delta == 1 || delta == ROUTE_RING_POINT_COUNT - 1
}

private fun shortestPath(graph: Array<MutableList<RouteEdge>>, nodeCount: Int): List<Int> {
    val distances = FloatArray(nodeCount) { Float.POSITIVE_INFINITY }
    val previous = IntArray(nodeCount) { -1 }
    val queue = PriorityQueue(compareBy<RouteState> { it.distance })

    distances[0] = 0f
    queue += RouteState(index = 0, distance = 0f)

    while (queue.isNotEmpty()) {
        val current = queue.remove()
        if (current.distance > distances[current.index]) {
            continue
        }
        if (current.index == 1) {
            break
        }
        graph[current.index].forEach { edge ->
            val nextDistance = current.distance + edge.distance
            if (nextDistance >= distances[edge.to]) {
                return@forEach
            }
            distances[edge.to] = nextDistance
            previous[edge.to] = current.index
            queue += RouteState(index = edge.to, distance = nextDistance)
        }
    }

    if (distances[1] == Float.POSITIVE_INFINITY) {
        return emptyList()
    }

    val path = mutableListOf<Int>()
    var cursor = 1
    while (cursor != -1) {
        path += cursor
        cursor = previous[cursor]
    }
    return path.asReversed()
}

private fun clampToWorld(point: Offset, worldBounds: WorldBounds): Offset {
    return Offset(
        point.x.coerceIn(ROUTE_WORLD_SIDE_MARGIN, worldBounds.width - ROUTE_WORLD_SIDE_MARGIN),
        point.y.coerceIn(ROUTE_WORLD_VERTICAL_MARGIN, worldBounds.height - ROUTE_WORLD_VERTICAL_MARGIN)
    )
}

internal fun segmentHitsCircle(a: Offset, b: Offset, center: Offset, radius: Float): Boolean {
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

private data class RouteNode(
    val position: Offset,
    val obstacleIndex: Int? = null,
    val ringIndex: Int? = null
)

private data class RouteEdge(
    val to: Int,
    val distance: Float
)

private data class RouteState(
    val index: Int,
    val distance: Float
)
