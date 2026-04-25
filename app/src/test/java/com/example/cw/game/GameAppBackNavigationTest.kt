package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import com.example.cw.game.levels.StarThresholds
import com.example.cw.game.levels.WorldBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameAppBackNavigationTest {
    @Test
    fun resolveBackNavigation_homeLeavesSystemBackUnhandled() {
        val result = resolveBackNavigation(AppScreen.HOME, matchState = null)

        assertFalse(result.handled)
        assertEquals(AppScreen.HOME, result.screen)
        assertNull(result.matchState)
    }

    @Test
    fun resolveBackNavigation_levelsReturnsToHome() {
        val result = resolveBackNavigation(AppScreen.LEVELS, matchState = null)

        assertTrue(result.handled)
        assertEquals(AppScreen.HOME, result.screen)
        assertNull(result.matchState)
    }

    @Test
    fun resolveBackNavigation_upgradesReturnsToHome() {
        val result = resolveBackNavigation(AppScreen.UPGRADES, matchState = null)

        assertTrue(result.handled)
        assertEquals(AppScreen.HOME, result.screen)
        assertNull(result.matchState)
    }

    @Test
    fun resolveBackNavigation_abilitiesReturnsToUpgrades() {
        val result = resolveBackNavigation(AppScreen.ABILITIES, matchState = null)

        assertTrue(result.handled)
        assertEquals(AppScreen.UPGRADES, result.screen)
        assertNull(result.matchState)
    }

    @Test
    fun resolveBackNavigation_runningMissionOpensPauseMenu() {
        val runningMatch = matchState(isPaused = false, status = MatchStatus.RUNNING)

        val result = resolveBackNavigation(AppScreen.IN_GAME, runningMatch)

        assertTrue(result.handled)
        assertEquals(AppScreen.IN_GAME, result.screen)
        assertEquals(true, result.matchState?.isPaused)
    }

    @Test
    fun resolveBackNavigation_pausedMissionQuitsToLevels() {
        val pausedMatch = matchState(isPaused = true, status = MatchStatus.RUNNING)

        val result = resolveBackNavigation(AppScreen.IN_GAME, pausedMatch)

        assertTrue(result.handled)
        assertEquals(AppScreen.LEVELS, result.screen)
        assertNull(result.matchState)
    }

    @Test
    fun resolveBackNavigation_finishedMissionQuitsToLevels() {
        val finishedMatch = matchState(isPaused = false, status = MatchStatus.PLAYER_WON)

        val result = resolveBackNavigation(AppScreen.IN_GAME, finishedMatch)

        assertTrue(result.handled)
        assertEquals(AppScreen.LEVELS, result.screen)
        assertNull(result.matchState)
    }

    @Test
    fun resolveBackNavigation_missingInGameMatchFallsBackToLevels() {
        val result = resolveBackNavigation(AppScreen.IN_GAME, matchState = null)

        assertTrue(result.handled)
        assertEquals(AppScreen.LEVELS, result.screen)
        assertNull(result.matchState)
    }

    private fun matchState(
        isPaused: Boolean,
        status: MatchStatus
    ): MatchState {
        return MatchState(
            worldBounds = WorldBounds(),
            bases = listOf(
                BaseState(
                    id = 1,
                    position = Offset(100f, 100f),
                    owner = Owner.PLAYER,
                    type = BaseType.COMMAND,
                    units = 10f,
                    capLevel = 2
                )
            ),
            fleets = emptyList(),
            obstacles = emptyList(),
            playerMoney = 0f,
            aiStates = emptyMap(),
            nextFleetId = 1,
            selectedBaseIds = emptySet(),
            message = "",
            status = status,
            levelId = 1,
            levelName = "Test",
            starThresholds = StarThresholds(),
            elapsedSeconds = 0f,
            isPaused = isPaused
        )
    }
}
