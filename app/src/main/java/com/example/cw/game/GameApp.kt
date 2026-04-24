package com.example.cw.game

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.example.cw.game.levels.AssetLevelRepository
import com.example.cw.game.levels.LevelRepository

@Composable
fun GameApp() {
    val context = LocalContext.current
    val levelRepository: LevelRepository = remember(context) { AssetLevelRepository(context.assets) }
    val levelLoadResult = remember(levelRepository) {
        runCatching { levelRepository.listLevels() }
    }
    val levels = levelLoadResult.getOrElse { emptyList() }

    var appScreen by remember { mutableStateOf(AppScreen.HOME) }
    var campaign by remember { mutableStateOf(CampaignState()) }
    var matchState by remember { mutableStateOf<MatchState?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var levelLoadError by remember { mutableStateOf(levelLoadResult.exceptionOrNull()?.message) }

    LaunchedEffect(appScreen, matchState?.status, matchState?.isPaused, campaign.cashRateLevel) {
        var previousTime = 0L
        while (appScreen == AppScreen.IN_GAME && matchState?.status == MatchStatus.RUNNING) {
            withFrameNanos { frameTime ->
                val currentMatch = matchState ?: return@withFrameNanos
                if (currentMatch.isPaused) {
                    previousTime = frameTime
                    return@withFrameNanos
                }
                if (previousTime == 0L) {
                    previousTime = frameTime
                } else {
                    val dt = ((frameTime - previousTime) / 1_000_000_000f).coerceIn(0.0f, 0.033f)
                    previousTime = frameTime
                    val stepped = stepMatch(currentMatch, dt, campaign.cashIncomeMultiplier())
                    val earnedUpgradePoint = currentMatch.status == MatchStatus.RUNNING &&
                        stepped.status == MatchStatus.PLAYER_WON &&
                        stepped.levelId !in campaign.completedLevels
                    matchState = stepped.copy(earnedUpgradePoint = earnedUpgradePoint)
                    if (earnedUpgradePoint) {
                        campaign = campaign.completeLevel(stepped.levelId)
                    }
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF06111C)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF02050A), Color(0xFF081C29), Color(0xFF0A2331))
                    )
                )
        ) {
            when (appScreen) {
                AppScreen.HOME -> HomeScreen(
                    onLevels = { appScreen = AppScreen.LEVELS },
                    onUpgrades = { appScreen = AppScreen.UPGRADES }
                )

                AppScreen.LEVELS -> LevelSelectScreen(
                    campaign = campaign,
                    levels = levels,
                    loadError = levelLoadError,
                    onBack = { appScreen = AppScreen.HOME },
                    onPlayLevel = { levelId ->
                        runCatching { createMatch(levelRepository.loadLevel(levelId)) }
                            .onSuccess { level ->
                                levelLoadError = null
                                matchState = level
                                appScreen = AppScreen.IN_GAME
                            }
                            .onFailure { error ->
                                levelLoadError = error.message ?: "Failed to load level $levelId."
                            }
                    }
                )

                AppScreen.UPGRADES -> UpgradesScreen(
                    campaign = campaign,
                    onBack = { appScreen = AppScreen.HOME },
                    onUpgradeCashRate = {
                        if (campaign.upgradePoints > 0) {
                            campaign = campaign.copy(
                                upgradePoints = campaign.upgradePoints - 1,
                                cashRateLevel = campaign.cashRateLevel + 1
                            )
                        }
                    }
                )

                AppScreen.IN_GAME -> {
                    val activeMatch = matchState
                    if (activeMatch != null) {
                        val latestMatch by rememberUpdatedState(matchState)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { viewportSize = it }
                                .pointerInput(viewportSize) {
                                    detectTapGestures(
                                        onTap = { tap: Offset ->
                                            val currentMatch = latestMatch ?: return@detectTapGestures
                                            if (
                                                currentMatch.status != MatchStatus.RUNNING ||
                                                currentMatch.isPaused ||
                                                viewportSize == IntSize.Zero
                                            ) {
                                                return@detectTapGestures
                                            }
                                            matchState = onScreenTap(currentMatch, tap, viewportSize, false)
                                        },
                                        onDoubleTap = { tap: Offset ->
                                            val currentMatch = latestMatch ?: return@detectTapGestures
                                            if (
                                                currentMatch.status != MatchStatus.RUNNING ||
                                                currentMatch.isPaused ||
                                                viewportSize == IntSize.Zero
                                            ) {
                                                return@detectTapGestures
                                            }
                                            matchState = onScreenTap(currentMatch, tap, viewportSize, true)
                                        }
                                    )
                                }
                        ) {
                            GameCanvas(
                                state = activeMatch,
                                modifier = Modifier.fillMaxSize()
                            )
                            UpgradeNodeButton(
                                state = activeMatch,
                                viewportSize = viewportSize,
                                onUpgrade = { baseId ->
                                    matchState = upgradeBase(activeMatch, baseId)
                                }
                            )
                        }

                        InGameHud(
                            state = activeMatch,
                            onOpenMenu = {
                                matchState = togglePause(activeMatch)
                            }
                        )

                        if (activeMatch.isPaused && activeMatch.status == MatchStatus.RUNNING) {
                            PauseOverlay(
                                onResume = { matchState = activeMatch.copy(isPaused = false) },
                                onQuit = {
                                    matchState = null
                                    appScreen = AppScreen.LEVELS
                                }
                            )
                        }

                        if (activeMatch.status == MatchStatus.PLAYER_WON || activeMatch.status == MatchStatus.PLAYER_LOST) {
                            LevelEndOverlay(
                                state = activeMatch,
                                onLevels = {
                                    matchState = null
                                    appScreen = AppScreen.LEVELS
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun togglePause(state: MatchState): MatchState = state.copy(isPaused = !state.isPaused)
