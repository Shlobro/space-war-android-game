package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.cw.game.levels.BASE_RADIUS_MIN
import com.example.cw.game.levels.DEFAULT_MAX_LEVEL
import com.example.cw.game.levels.RADIUS_PER_LEVEL
import com.example.cw.game.levels.StarThresholds
import com.example.cw.game.levels.WorldBounds

internal data class MatchState(
    val worldBounds: WorldBounds,
    val bases: List<BaseState>,
    val fleets: List<FleetState>,
    val obstacles: List<Obstacle>,
    val playerMoney: Float,
    val aiStates: Map<Owner, AiRuntimeState>,
    val nextFleetId: Int,
    val selectedBaseIds: Set<Int>,
    val message: String,
    val messageExpiresAtSeconds: Float? = null,
    val status: MatchStatus,
    val levelId: Int,
    val levelName: String,
    val starThresholds: StarThresholds,
    val elapsedSeconds: Float,
    val isPaused: Boolean,
    val earnedStars: Int = 0,
    val earnedStarReward: Int = 0,
    val improvedBestStars: Boolean = false
)

internal data class CampaignState(
    val completedLevels: Set<Int> = emptySet(),
    val starsByLevel: Map<Int, Int> = emptyMap(),
    val bonusStarCredits: Int = 0,
    val spentStars: Int = 0,
    val cashRateLevel: Int = 0
) {
    val totalStars: Int get() = starsByLevel.values.sum()
    val availableStars: Int get() = (totalStars + bonusStarCredits - spentStars).coerceAtLeast(0)

    fun cashIncomeMultiplier(): Float = 1f + cashRateLevel * 0.25f

    fun starsForLevel(levelId: Int): Int = starsByLevel[levelId] ?: 0

    fun completeLevel(levelId: Int, starsEarned: Int): CampaignState {
        require(starsEarned in 1..3) { "starsEarned must be between 1 and 3" }
        val previousBest = starsForLevel(levelId)
        val bestStars = maxOf(previousBest, starsEarned)
        val completed = levelId in completedLevels
        return copy(
            completedLevels = completedLevels + levelId,
            starsByLevel = starsByLevel + (levelId to bestStars)
        )
    }

    fun spendStars(cost: Int): CampaignState {
        require(cost >= 0) { "cost must be non-negative" }
        if (cost == 0) return this
        require(availableStars >= cost) { "Not enough stars to spend $cost" }
        return copy(spentStars = spentStars + cost)
    }
}

internal data class BaseState(
    val id: Int,
    val position: Offset,
    val owner: Owner,
    val type: BaseType,
    val units: Float,
    val capLevel: Int,
    val maxLevel: Int = DEFAULT_MAX_LEVEL
) {
    val cap: Int get() = capLevel * 10
    val radius: Float get() = BASE_RADIUS_MIN + (capLevel - 1) * RADIUS_PER_LEVEL
    val productionRate: Float get() = 0.9f
}

internal data class FleetState(
    val id: Int,
    val owner: Owner,
    val sourceId: Int,
    val targetId: Int,
    val position: Offset,
    val path: List<Offset>,
    val pathIndex: Int,
    val units: Float,
    val speed: Float,
    val arrivalMultiplier: Float,
    val fleetDamageMultiplier: Float,
    val type: BaseType
)

internal data class Obstacle(
    val position: Offset,
    val radius: Float
)

internal data class AiRuntimeState(
    val type: AiType,
    val money: Float,
    val thinkCountdown: Float
)

internal enum class AppScreen {
    HOME,
    LEVELS,
    UPGRADES,
    IN_GAME
}

internal enum class MatchStatus {
    RUNNING,
    PLAYER_WON,
    PLAYER_LOST
}

internal enum class Owner(
    val label: String,
    val color: Color,
    val isPlayer: Boolean = false,
    val isNeutral: Boolean = false,
    val isAi: Boolean = false
) {
    PLAYER(label = "Player", color = Color(0xFF59D0FF), isPlayer = true),
    AI_1(label = "AI 1", color = Color(0xFFFF7868), isAi = true),
    AI_2(label = "AI 2", color = Color(0xFFFFB347), isAi = true),
    AI_3(label = "AI 3", color = Color(0xFFBE8CFF), isAi = true),
    AI_4(label = "AI 4", color = Color(0xFF5DE2A5), isAi = true),
    NEUTRAL(label = "Neutral", color = Color(0xFF8999A8), isNeutral = true)
}

internal enum class AiType(
    val label: String,
    val description: String
) {
    STANDARD(
        label = "Standard",
        description = "Attacks nearby weaker targets and buys capacity upgrades when no good attack is available."
    )
}

internal enum class BaseType(
    val label: String,
    val shortLabel: String,
    val accent: Color,
    val description: String
) {
    COMMAND(
        label = "Command",
        shortLabel = "CMD",
        accent = Color(0xFFE8F3FF),
        description = "Balanced headquarters with no situational modifier."
    ),
    FAST(
        label = "Fast",
        shortLabel = "FST",
        accent = Color(0xFFBFE7FF),
        description = "Launches faster-moving ships."
    ),
    FACTORY(
        label = "Factory",
        shortLabel = "FAC",
        accent = Color(0xFFB8FFCF),
        description = "Produces ships faster and rewards early economic expansion."
    ),
    RELAY(
        label = "Relay",
        shortLabel = "RLY",
        accent = Color(0xFFFEDC8B),
        description = "Fleets sent to friendly bases travel faster and reinforce harder."
    ),
    ASSAULT(
        label = "Assault",
        shortLabel = "ATK",
        accent = Color(0xFFFFB1A1),
        description = "Fleets sent to neutral or enemy bases hit harder and skirmish better."
    ),
    BATTERY(
        label = "Battery",
        shortLabel = "BAT",
        accent = Color(0xFFC0A4FF),
        description = "Damages passing enemy fleets inside a large defensive radius."
    )
}
