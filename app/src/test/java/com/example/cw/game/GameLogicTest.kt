package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.example.cw.game.levels.LevelAiDefinition
import com.example.cw.game.levels.LevelBaseDefinition
import com.example.cw.game.levels.LevelDefinition
import com.example.cw.game.levels.LevelObstacleDefinition
import com.example.cw.game.levels.WorldBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {
    private val viewportSize = IntSize(1000, 1000)
    private val testWorldBounds = WorldBounds(width = 1000f, height = 1600f)
    private val upgradeNodeFallbackButtonSize = IntSize(
        UPGRADE_NODE_BUTTON_FALLBACK_WIDTH_DP,
        UPGRADE_NODE_BUTTON_FALLBACK_HEIGHT_DP
    )

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
        val state = matchState(bases = listOf(base), playerMoney = 40f)

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
        val state = matchState(
            bases = listOf(playerBase, enemyBase),
            fleets = listOf(arrivingEnemyFleet),
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, 1f)),
            nextFleetId = 2,
            selectedBaseIds = setOf(playerBase.id)
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
        val state = matchState(
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
            aiStates = mapOf(
                Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, ENEMY_AI_THINK_INTERVAL_SECONDS)
            ),
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

    @Test
    fun stepMatch_accruesReducedFundsPerOwnedBaseForPlayerAndAi() {
        val aiStartingMoney = 0f
        val state = matchState(
            bases = listOf(
                BaseState(
                    id = 1,
                    position = Offset(100f, 100f),
                    owner = Owner.PLAYER,
                    type = BaseType.COMMAND,
                    units = 10f,
                    capLevel = 2
                ),
                BaseState(
                    id = 2,
                    position = Offset(200f, 100f),
                    owner = Owner.PLAYER,
                    type = BaseType.COMMAND,
                    units = 10f,
                    capLevel = 2
                ),
                BaseState(
                    id = 3,
                    position = Offset(300f, 100f),
                    owner = Owner.AI_1,
                    type = BaseType.COMMAND,
                    units = 10f,
                    capLevel = 2
                )
            ),
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, aiStartingMoney, 10f))
        )

        val updated = stepMatch(state, dt = 10f, cashIncomeMultiplier = 1f)

        // Player: (0.6 + 2 * 0.25) * 10s = 11.0, AI: 0f + (0.6 + 1 * 0.25) * 10s = 8.5.
        assertEquals(11f, updated.playerMoney, 0.001f)
        assertEquals(8.5f, updated.aiStates.getValue(Owner.AI_1).money, 0.001f)
    }

    @Test
    fun stepMatch_captureDropsBaseByTwoLevels() {
        val targetBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 8f,
            capLevel = 4
        )
        val playerBase = BaseState(
            id = 2,
            position = Offset(200f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val arrivingFleet = FleetState(
            id = 1,
            owner = Owner.PLAYER,
            sourceId = 2,
            targetId = 1,
            position = targetBase.position,
            path = listOf(targetBase.position),
            pathIndex = 1,
            units = 18f,
            speed = 120f,
            arrivalMultiplier = 1f,
            fleetDamageMultiplier = 1f,
            type = BaseType.COMMAND
        )
        val state = matchState(
            bases = listOf(targetBase, playerBase),
            fleets = listOf(arrivingFleet),
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, 1f))
        )

        val updated = stepMatch(state, dt = 0f, cashIncomeMultiplier = 1f)
        val capturedBase = updated.bases.first { it.id == targetBase.id }

        assertEquals(Owner.PLAYER, capturedBase.owner)
        assertEquals(2, capturedBase.capLevel)
        assertEquals(10f, capturedBase.units)
    }

    @Test
    fun stepMatch_captureFromLevelTwoDropsToMinimumLevelOne() {
        val targetBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 6f,
            capLevel = 2
        )
        val playerBase = BaseState(
            id = 2,
            position = Offset(200f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val arrivingFleet = FleetState(
            id = 1,
            owner = Owner.PLAYER,
            sourceId = 2,
            targetId = 1,
            position = targetBase.position,
            path = listOf(targetBase.position),
            pathIndex = 1,
            units = 12f,
            speed = 120f,
            arrivalMultiplier = 1f,
            fleetDamageMultiplier = 1f,
            type = BaseType.COMMAND
        )
        val state = matchState(
            bases = listOf(targetBase, playerBase),
            fleets = listOf(arrivingFleet),
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, 1f))
        )

        val updated = stepMatch(state, dt = 0f, cashIncomeMultiplier = 1f)
        val capturedBase = updated.bases.first { it.id == targetBase.id }

        assertEquals(Owner.PLAYER, capturedBase.owner)
        assertEquals(1, capturedBase.capLevel)
        assertEquals(6f, capturedBase.units)
    }

    @Test
    fun stepMatch_captureAtMinimumLevelKeepsLevelOneAndRemainingGarrison() {
        val targetBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 3f,
            capLevel = 1
        )
        val playerBase = BaseState(
            id = 2,
            position = Offset(200f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val arrivingFleet = FleetState(
            id = 1,
            owner = Owner.PLAYER,
            sourceId = 2,
            targetId = 1,
            position = targetBase.position,
            path = listOf(targetBase.position),
            pathIndex = 1,
            units = 9f,
            speed = 120f,
            arrivalMultiplier = 1f,
            fleetDamageMultiplier = 1f,
            type = BaseType.COMMAND
        )
        val state = matchState(
            bases = listOf(targetBase, playerBase),
            fleets = listOf(arrivingFleet),
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, 1f))
        )

        val updated = stepMatch(state, dt = 0f, cashIncomeMultiplier = 1f)
        val capturedBase = updated.bases.first { it.id == targetBase.id }

        assertEquals(Owner.PLAYER, capturedBase.owner)
        assertEquals(1, capturedBase.capLevel)
        assertEquals(6f, capturedBase.units)
    }

    @Test
    fun stepMatch_friendlyArrivalClampsReinforcementsToCurrentCap() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 18f,
            capLevel = 2
        )
        val reinforcingFleet = FleetState(
            id = 1,
            owner = Owner.PLAYER,
            sourceId = 2,
            targetId = 1,
            position = playerBase.position,
            path = listOf(playerBase.position),
            pathIndex = 1,
            units = 8f,
            speed = 120f,
            arrivalMultiplier = 1f,
            fleetDamageMultiplier = 1f,
            type = BaseType.COMMAND
        )
        val state = matchState(
            bases = listOf(
                playerBase,
                BaseState(
                    id = 2,
                    position = Offset(200f, 100f),
                    owner = Owner.PLAYER,
                    type = BaseType.COMMAND,
                    units = 10f,
                    capLevel = 2
                )
            ),
            fleets = listOf(reinforcingFleet)
        )

        val updated = stepMatch(state, dt = 0f, cashIncomeMultiplier = 1f)

        assertEquals(20f, updated.bases.first { it.id == playerBase.id }.units)
        assertTrue(updated.fleets.isEmpty())
    }

    @Test
    fun onScreenTap_selectingPlayerBaseKeepsExistingMessage() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(500f, 500f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(playerBase),
            message = "Testing"
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(playerBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertEquals(setOf(playerBase.id), updated.selectedBaseIds)
        assertEquals("Testing", updated.message)
    }

    @Test
    fun onScreenTap_deselectingPlayerBaseKeepsExistingMessage() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(500f, 500f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(playerBase),
            selectedBaseIds = setOf(playerBase.id),
            message = "Testing"
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(playerBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Testing", updated.message)
    }

    @Test
    fun onScreenTap_doubleTapWithOnlyNonPlayerSelectionClearsSelectionWithoutMessageChange() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(500f, 500f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 2,
            position = Offset(650f, 500f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(playerBase, enemyBase),
            selectedBaseIds = setOf(enemyBase.id),
            message = "Testing"
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(playerBase.position),
            viewportSize = viewportSize,
            isDoubleTap = true
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Testing", updated.message)
    }

    @Test
    fun onScreenTap_enemyTargetWithOnlyNonPlayerSelectionClearsSelectionWithoutMessageChange() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(500f, 500f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 2,
            position = Offset(650f, 500f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(playerBase, enemyBase),
            selectedBaseIds = setOf(enemyBase.id),
            message = "Testing"
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(enemyBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Testing", updated.message)
    }

    @Test
    fun selectedUpgradablePlayerBase_hidesUpgradeButtonAtMaxLevel() {
        val maxedBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 4,
            maxLevel = 4
        )
        val state = matchState(
            bases = listOf(maxedBase),
            playerMoney = 100f,
            selectedBaseIds = setOf(maxedBase.id)
        )

        val upgradeBase = selectedUpgradablePlayerBase(state)

        assertNull(upgradeBase)
    }

    @Test
    fun selectedUpgradablePlayerBase_returnsSelectedPlayerBaseBelowMaxLevel() {
        val upgradableBase = BaseState(
            id = 1,
            position = Offset(100f, 100f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 10f,
            capLevel = 3,
            maxLevel = 4
        )
        val state = matchState(
            bases = listOf(upgradableBase),
            playerMoney = 100f,
            selectedBaseIds = setOf(upgradableBase.id)
        )

        val upgradeBase = selectedUpgradablePlayerBase(state)

        assertEquals(upgradableBase, upgradeBase)
    }

    @Test
    fun baseLabelLayout_placesLevelLabelBelowUnitCount() {
        val baseRadius = 36f
        val layout = baseLabelLayout(baseRadius)

        assertTrue(layout.levelOffsetY > layout.unitsOffsetY)
        assertTrue(layout.levelOffsetY <= baseRadius - 4f)
        assertEquals(-(baseRadius + 16f), layout.selectedOffsetY, 0.001f)
    }

    @Test
    fun baseLabelLayout_keepsLevelLabelInsideSmallRenderedBase() {
        val smallRenderedRadius = 12.96f

        val layout = baseLabelLayout(smallRenderedRadius)

        assertTrue(layout.levelOffsetY > layout.unitsOffsetY)
        assertTrue(layout.levelOffsetY < smallRenderedRadius)
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

    private fun sampleLevel(): LevelDefinition {
        return LevelDefinition(
            schemaVersion = 2,
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

    private fun matchState(
        bases: List<BaseState>,
        fleets: List<FleetState> = emptyList(),
        playerMoney: Float = 0f,
        aiStates: Map<Owner, AiRuntimeState> = emptyMap(),
        nextFleetId: Int = 1,
        selectedBaseIds: Set<Int> = emptySet(),
        message: String = ""
    ): MatchState {
        return MatchState(
            worldBounds = testWorldBounds,
            bases = bases,
            fleets = fleets,
            obstacles = emptyList(),
            playerMoney = playerMoney,
            aiStates = aiStates,
            nextFleetId = nextFleetId,
            selectedBaseIds = selectedBaseIds,
            message = message,
            status = MatchStatus.RUNNING,
            levelId = 1,
            levelName = "Test",
            isPaused = false
        )
    }

    private fun tapAt(worldPosition: Offset): Offset {
        return worldToScreen(
            offset = worldPosition,
            canvasSize = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat()),
            worldBounds = testWorldBounds
        )
    }
}
