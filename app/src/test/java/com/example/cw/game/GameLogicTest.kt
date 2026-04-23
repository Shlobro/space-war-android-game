package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import com.example.cw.game.levels.LevelAiDefinition
import com.example.cw.game.levels.LevelBaseDefinition
import com.example.cw.game.levels.LevelDefinition
import com.example.cw.game.levels.LevelObstacleDefinition
import com.example.cw.game.levels.WorldBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {
    @Test
    fun sendFleet_launchesHalfTheUnitsAndCreatesFleet() {
        val state = createMatch(sampleLevel())

        val updated = sendFleet(state, sourceId = 1, targetId = 3, sender = Owner.PLAYER)

        assertEquals(1, updated.fleets.size)
        assertEquals(16f, updated.bases.first { it.id == 1 }.units)
        assertEquals(16f, updated.fleets.single().units)
        assertEquals("Launched 16 ships", updated.message)
    }

    @Test
    fun upgradeBase_spendsFundsAndRaisesCap() {
        val base = BaseState(
            id = 1,
            position = Offset.Zero,
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 1
        )
        val state = MatchState(
            worldBounds = WorldBounds(),
            bases = listOf(base),
            fleets = emptyList(),
            obstacles = emptyList(),
            playerMoney = 40f,
            aiStates = emptyMap(),
            nextFleetId = 1,
            selectedBaseIds = emptySet(),
            message = "",
            status = MatchStatus.RUNNING,
            levelId = 1,
            levelName = "Test",
            isPaused = false
        )

        val updated = upgradeBase(state, baseId = 1)

        assertEquals(12f, updated.playerMoney)
        assertEquals(20, updated.bases.single().cap)
        assertEquals(2, updated.bases.single().capLevel)
        assertEquals("Base upgraded", updated.message)
    }

    @Test
    fun buildRoute_addsDetourWhenObstacleBlocksDirectPath() {
        val obstacle = Obstacle(position = Offset(500f, 500f), radius = 100f)

        val route = buildRoute(
            start = Offset(100f, 500f),
            end = Offset(900f, 500f),
            obstacles = listOf(obstacle),
            worldBounds = WorldBounds()
        )

        assertTrue(route.size > 1)
        assertTrue(route.first().y != 500f)
        assertEquals(Offset(900f, 500f), route.last())
    }

    @Test
    fun stepMatch_clearsSelectionForBasesThePlayerNoLongerOwns() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 5f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 2,
            position = Offset(200f, 100f),
            owner = Owner.AI_1,
            type = BaseType.ASSAULT,
            units = 20f,
            capLevel = 2
        )
        val arrivingEnemyFleet = FleetState(
            id = 1,
            owner = Owner.AI_1,
            sourceId = 2,
            targetId = 1,
            position = playerBase.position,
            path = listOf(playerBase.position),
            pathIndex = 1,
            units = 10f,
            speed = 120f,
            arrivalMultiplier = 1f,
            fleetDamageMultiplier = 1f,
            type = BaseType.ASSAULT
        )
        val state = MatchState(
            worldBounds = WorldBounds(),
            bases = listOf(playerBase, enemyBase),
            fleets = listOf(arrivingEnemyFleet),
            obstacles = emptyList(),
            playerMoney = 0f,
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, 1f)),
            nextFleetId = 2,
            selectedBaseIds = setOf(playerBase.id),
            message = "",
            status = MatchStatus.RUNNING,
            levelId = 1,
            levelName = "Test",
            isPaused = false
        )

        val updated = stepMatch(state, dt = 0f, cashIncomeMultiplier = 1f)

        assertEquals(Owner.AI_1, updated.bases.first { it.id == playerBase.id }.owner)
        assertTrue(updated.selectedBaseIds.isEmpty())
    }

    @Test
    fun stepMatch_enemyAiWaitsForFiveSecondThinkInterval() {
        val enemyBase = BaseState(
            id = 2,
            position = Offset(200f, 100f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 20f,
            capLevel = 2
        )
        val targetBase = BaseState(
            id = 3,
            position = Offset(260f, 100f),
            owner = Owner.NEUTRAL,
            type = BaseType.COMMAND,
            units = 4f,
            capLevel = 2
        )
        val state = MatchState(
            worldBounds = WorldBounds(),
            bases = listOf(
                BaseState(
                    id = 1,
                    position = Offset(100f, 100f),
                    owner = Owner.PLAYER,
                    type = BaseType.COMMAND,
                    units = 10f,
                    capLevel = 2
                ),
                enemyBase,
                targetBase
            ),
            fleets = emptyList(),
            obstacles = emptyList(),
            playerMoney = 0f,
            aiStates = mapOf(
                Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, ENEMY_AI_THINK_INTERVAL_SECONDS)
            ),
            nextFleetId = 1,
            selectedBaseIds = emptySet(),
            message = "",
            status = MatchStatus.RUNNING,
            levelId = 1,
            levelName = "Test",
            isPaused = false
        )

        val beforeThink = stepMatch(state, dt = 4f, cashIncomeMultiplier = 1f)
        val sourceAfterWaiting = beforeThink.bases.first { it.id == 2 }

        assertTrue(beforeThink.fleets.isEmpty())
        assertEquals(20f, sourceAfterWaiting.units)
        assertEquals(1f, beforeThink.aiStates.getValue(Owner.AI_1).thinkCountdown)

        val afterThink = stepMatch(beforeThink, dt = 1f, cashIncomeMultiplier = 1f)

        assertEquals(ENEMY_AI_THINK_INTERVAL_SECONDS, afterThink.aiStates.getValue(Owner.AI_1).thinkCountdown)
        assertEquals(1, afterThink.fleets.size)
        assertEquals(10f, afterThink.fleets.single().units)
        assertEquals(10f, afterThink.bases.first { it.id == 2 }.units)
    }

    private fun sampleLevel(): LevelDefinition {
        return LevelDefinition(
            schemaVersion = 1,
            levelId = 1,
            name = "Sample",
            description = "Sample",
            sortOrder = 1,
            unlockAfterLevelId = null,
            worldBounds = WorldBounds(),
            introMessage = "Start",
            aiControllers = listOf(LevelAiDefinition(Owner.AI_1, AiType.STANDARD)),
            bases = listOf(
                LevelBaseDefinition(1, 180f, 1350f, Owner.PLAYER, BaseType.COMMAND, 32f, 4),
                LevelBaseDefinition(2, 820f, 250f, Owner.AI_1, BaseType.COMMAND, 32f, 4),
                LevelBaseDefinition(3, 260f, 1100f, Owner.NEUTRAL, BaseType.COMMAND, 16f, 2)
            ),
            obstacles = listOf(LevelObstacleDefinition(500f, 520f, 95f))
        )
    }
}
