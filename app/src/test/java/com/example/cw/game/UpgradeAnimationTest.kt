package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class UpgradeAnimationTest {

    @Test
    fun upgradeAnimationBase_returnsCurrentBaseWhenSelectedBaseLevelsUp() {
        val previous = playerBase(id = 4, capLevel = 2)
        val current = previous.copy(capLevel = 3)

        val animationBase = upgradeAnimationBase(previous, current)

        assertEquals(current, animationBase)
    }

    @Test
    fun upgradeAnimationBase_ignoresSelectionChanges() {
        val previous = playerBase(id = 4, capLevel = 2)
        val current = playerBase(id = 7, capLevel = 3)

        val animationBase = upgradeAnimationBase(previous, current)

        assertNull(animationBase)
    }

    @Test
    fun upgradeAnimationBase_ignoresUnchangedLevel() {
        val previous = playerBase(id = 4, capLevel = 2)
        val current = previous.copy(units = 18f)

        val animationBase = upgradeAnimationBase(previous, current)

        assertNull(animationBase)
    }

    @Test
    fun selectedSinglePlayerBase_returnsOnlySingleSelectedPlayerOwnedBase() {
        val selectedBase = playerBase(id = 4, capLevel = 2)
        val state = matchState(
            bases = listOf(selectedBase, playerBase(id = 7, capLevel = 3)),
            selectedBaseIds = setOf(selectedBase.id)
        )

        val resolvedBase = selectedSinglePlayerBase(state)

        assertEquals(selectedBase, resolvedBase)
    }

    @Test
    fun selectedSinglePlayerBase_returnsNullForMaxLevelPlayerBaseOnlyWhenUsingUpgradableHelper() {
        val maxedBase = playerBase(id = 4, capLevel = 3, maxLevel = 3)
        val state = matchState(
            bases = listOf(maxedBase),
            selectedBaseIds = setOf(maxedBase.id)
        )

        assertEquals(maxedBase, selectedSinglePlayerBase(state))
        assertNull(selectedUpgradablePlayerBase(state))
    }

    @Test
    fun runUpgradeAnimation_clearsAnimationStateAfterCompletion() = runBlocking {
        val upgradedBase = playerBase(id = 4, capLevel = 3)
        var activeAnimation: UpgradeAnimationState? = null

        runUpgradeAnimation(
            upgradedBase = upgradedBase,
            onAnimationStart = { activeAnimation = it },
            animateProgress = {},
            onAnimationFinish = { activeAnimation = null }
        )

        assertNull(activeAnimation)
    }

    @Test
    fun runUpgradeAnimation_clearsAnimationStateWhenCancelled() = runBlocking {
        val upgradedBase = playerBase(id = 4, capLevel = 3)
        var activeAnimation: UpgradeAnimationState? = null

        val job = launch {
            runUpgradeAnimation(
                upgradedBase = upgradedBase,
                onAnimationStart = { activeAnimation = it },
                animateProgress = { delay(60_000) },
                onAnimationFinish = { activeAnimation = null }
            )
        }

        yield()
        assertTrue(activeAnimation != null)

        job.cancel()
        job.join()

        assertNull(activeAnimation)
    }

    private fun playerBase(id: Int, capLevel: Int, maxLevel: Int = 4): BaseState {
        return BaseState(
            id = id,
            position = Offset(100f * id, 120f),
            owner = Owner.PLAYER,
            type = BaseType.COMMAND,
            units = 12f,
            capLevel = capLevel,
            maxLevel = maxLevel
        )
    }

    private fun matchState(
        bases: List<BaseState>,
        selectedBaseIds: Set<Int> = emptySet()
    ): MatchState {
        return MatchState(
            worldBounds = com.example.cw.game.levels.WorldBounds(),
            bases = bases,
            fleets = emptyList(),
            obstacles = emptyList(),
            playerMoney = 0f,
            aiStates = emptyMap(),
            nextFleetId = 1,
            selectedBaseIds = selectedBaseIds,
            message = "",
            status = MatchStatus.RUNNING,
            levelId = 1,
            levelName = "Test",
            starThresholds = com.example.cw.game.levels.StarThresholds(),
            elapsedSeconds = 0f,
            isPaused = false
        )
    }
}
