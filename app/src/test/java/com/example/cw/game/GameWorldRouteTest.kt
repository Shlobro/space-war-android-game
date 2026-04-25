package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import com.example.cw.game.levels.WorldBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameWorldRouteTest {
    private val worldBounds = WorldBounds(width = 1000f, height = 1600f)

    @Test
    fun buildRoute_keepsEverySegmentOutsideSingleObstacleBuffer() {
        val obstacle = Obstacle(position = Offset(500f, 500f), radius = 100f)

        val route = buildRoute(
            start = Offset(100f, 500f),
            end = Offset(900f, 500f),
            obstacles = listOf(obstacle),
            worldBounds = worldBounds
        )

        assertTrue(route.size > 1)
        assertEquals(Offset(900f, 500f), route.last())
        assertRouteClearsObstacle(start = Offset(100f, 500f), route = route, obstacle = obstacle)
    }

    @Test
    fun buildRoute_threadsBetweenMultipleObstaclesWithoutReenteringTheirBuffers() {
        val obstacles = listOf(
            Obstacle(position = Offset(420f, 520f), radius = 90f),
            Obstacle(position = Offset(620f, 520f), radius = 90f)
        )
        val start = Offset(120f, 520f)
        val end = Offset(920f, 520f)

        val route = buildRoute(
            start = start,
            end = end,
            obstacles = obstacles,
            worldBounds = worldBounds
        )

        assertTrue(route.size > 2)
        assertEquals(end, route.last())
        obstacles.forEach { obstacle ->
            assertRouteClearsObstacle(start = start, route = route, obstacle = obstacle)
        }
    }

    @Test
    fun buildRoute_returnsDirectEndpointWhenNoObstacleBlocksTheSegment() {
        val route = buildRoute(
            start = Offset(100f, 100f),
            end = Offset(900f, 100f),
            obstacles = listOf(Obstacle(position = Offset(500f, 500f), radius = 100f)),
            worldBounds = worldBounds
        )

        assertEquals(listOf(Offset(900f, 100f)), route)
    }

    @Test
    fun buildRoute_nearWorldClampStillKeepsSegmentsOutsideObstacleBuffer() {
        val obstacle = Obstacle(position = Offset(160f, 140f), radius = 40f)
        val start = Offset(80f, 140f)
        val end = Offset(520f, 140f)

        val route = buildRoute(
            start = start,
            end = end,
            obstacles = listOf(obstacle),
            worldBounds = worldBounds
        )

        assertTrue(route.size > 1)
        assertTrue(route.all { it.x in 80f..920f })
        assertTrue(route.all { it.y in 120f..1480f })
        assertRouteClearsObstacle(start = start, route = route, obstacle = obstacle)
    }

    @Test
    fun buildRoute_returnsEndpointFallbackWhenNoGraphPathCanBeBuilt() {
        val constrainedBounds = WorldBounds(width = 200f, height = 240f)

        val route = buildRoute(
            start = Offset(0f, 120f),
            end = Offset(200f, 120f),
            obstacles = listOf(Obstacle(position = Offset(100f, 120f), radius = 50f)),
            worldBounds = constrainedBounds
        )

        assertEquals(listOf(Offset(200f, 120f)), route)
    }

    private fun assertRouteClearsObstacle(
        start: Offset,
        route: List<Offset>,
        obstacle: Obstacle
    ) {
        val collisionRadius = obstacle.radius + 34f
        listOf(start, *route.toTypedArray()).zipWithNext().forEach { (from, to) ->
            assertFalse(
                "Segment $from -> $to still intersects ${obstacle.position} with radius $collisionRadius",
                segmentHitsCircle(from, to, obstacle.position, collisionRadius)
            )
        }
    }
}
