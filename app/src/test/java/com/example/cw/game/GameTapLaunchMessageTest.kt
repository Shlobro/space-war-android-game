package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.example.cw.game.levels.StarThresholds
import com.example.cw.game.levels.WorldBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTapLaunchMessageTest {
    private val viewportSize = IntSize(1000, 1000)
    private val testWorldBounds = WorldBounds(width = 1000f, height = 1600f)

    @Test
    fun onScreenTap_enemyTargetUsesShipCountMessageInsteadOfBatchLaunchText() {
        val playerBase = BaseState(
            id = 1,
            position = Offset(200f, 1200f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 20f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 2,
            position = Offset(800f, 400f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 8f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(playerBase, enemyBase),
            selectedBaseIds = setOf(playerBase.id)
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(enemyBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Launched 10 ships", updated.message)
    }

    @Test
    fun onScreenTap_doubleTapFriendlyTargetUsesShipCountMessageInsteadOfBatchReinforceText() {
        val sourceBase = BaseState(
            id = 1,
            position = Offset(200f, 1200f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 20f,
            capLevel = 2
        )
        val targetBase = BaseState(
            id = 2,
            position = Offset(400f, 1000f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 12f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(sourceBase, targetBase),
            selectedBaseIds = setOf(sourceBase.id)
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(targetBase.position),
            viewportSize = viewportSize,
            isDoubleTap = true
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Launched 10 ships", updated.message)
    }

    @Test
    fun onScreenTap_enemyTargetWithMultiSelectionUsesCombinedShipCountMessage() {
        val firstSource = BaseState(
            id = 1,
            position = Offset(200f, 1200f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 20f,
            capLevel = 2
        )
        val secondSource = BaseState(
            id = 2,
            position = Offset(320f, 1080f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 12f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 3,
            position = Offset(800f, 400f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 8f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(firstSource, secondSource, enemyBase),
            selectedBaseIds = setOf(firstSource.id, secondSource.id)
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(enemyBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Launched 16 ships", updated.message)
    }

    @Test
    fun onScreenTap_doubleTapFriendlyTargetWithMultiSelectionUsesCombinedShipCountMessage() {
        val firstSource = BaseState(
            id = 1,
            position = Offset(200f, 1200f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 20f,
            capLevel = 2
        )
        val secondSource = BaseState(
            id = 2,
            position = Offset(320f, 1080f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 12f,
            capLevel = 2
        )
        val targetBase = BaseState(
            id = 3,
            position = Offset(480f, 940f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 12f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(firstSource, secondSource, targetBase),
            selectedBaseIds = setOf(firstSource.id, secondSource.id)
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(targetBase.position),
            viewportSize = viewportSize,
            isDoubleTap = true
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Launched 16 ships", updated.message)
    }

    @Test
    fun onScreenTap_enemyTargetWithMixedSelectionKeepsCombinedShipCountMessage() {
        val firstSource = BaseState(
            id = 1,
            position = Offset(200f, 1200f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 20f,
            capLevel = 2
        )
        val secondSource = BaseState(
            id = 2,
            position = Offset(320f, 1080f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 1f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 3,
            position = Offset(800f, 400f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 8f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(firstSource, secondSource, enemyBase),
            selectedBaseIds = setOf(firstSource.id, secondSource.id)
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(enemyBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Launched 10 ships", updated.message)
    }

    @Test
    fun onScreenTap_enemyTargetWithNoEligibleShipsShowsFailureMessage() {
        val firstSource = BaseState(
            id = 1,
            position = Offset(200f, 1200f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 1f,
            capLevel = 2
        )
        val secondSource = BaseState(
            id = 2,
            position = Offset(320f, 1080f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 1f,
            capLevel = 2
        )
        val enemyBase = BaseState(
            id = 3,
            position = Offset(800f, 400f),
            owner = Owner.AI_1,
            type = BaseType.COMMAND,
            units = 8f,
            capLevel = 2
        )
        val state = matchState(
            bases = listOf(firstSource, secondSource, enemyBase),
            selectedBaseIds = setOf(firstSource.id, secondSource.id)
        )

        val updated = onScreenTap(
            state = state,
            screenTap = tapAt(enemyBase.position),
            viewportSize = viewportSize,
            isDoubleTap = false
        )

        assertTrue(updated.selectedBaseIds.isEmpty())
        assertEquals("Not enough ships to send", updated.message)
    }

    private fun matchState(
        bases: List<BaseState>,
        selectedBaseIds: Set<Int> = emptySet()
    ): MatchState {
        return MatchState(
            worldBounds = testWorldBounds,
            bases = bases,
            fleets = emptyList(),
            obstacles = emptyList(),
            playerMoney = 0f,
            aiStates = mapOf(Owner.AI_1 to AiRuntimeState(AiType.STANDARD, 0f, 5f)),
            nextFleetId = 1,
            selectedBaseIds = selectedBaseIds,
            message = "",
            messageExpiresAtSeconds = null,
            status = MatchStatus.RUNNING,
            levelId = 1,
            levelName = "Test",
            starThresholds = StarThresholds(),
            elapsedSeconds = 0f,
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
