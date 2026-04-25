package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import com.example.cw.game.levels.BASE_RADIUS_MIN
import com.example.cw.game.levels.DEFAULT_MAX_LEVEL
import com.example.cw.game.levels.RADIUS_PER_LEVEL
import com.example.cw.game.levels.StarThresholds
import com.example.cw.game.levels.WorldBounds

internal const val CAMPAIGN_MAX_UPGRADE_LEVEL = 5
internal const val CAMPAIGN_SPECIAL_ABILITY_UPGRADE_COST = 1
internal const val SPECIAL_ABILITY_ACTIVE_DURATION_SECONDS = 5f
internal const val SPECIAL_ABILITY_COOLDOWN_SECONDS = 30f

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
    val playerShipProductionMultiplier: Float = 1f,
    val playerFleetSpeedMultiplier: Float = 1f,
    val selectedSpecialAbility: SpecialAbilityType? = null,
    val selectedSpecialAbilityLevel: Int = 0,
    val specialAbilityActiveSecondsRemaining: Float = 0f,
    val specialAbilityCooldownSecondsRemaining: Float = 0f,
    val specialAbilityTargetBaseId: Int? = null,
    val earnedStars: Int = 0,
    val earnedStarReward: Int = 0,
    val improvedBestStars: Boolean = false
)

internal data class CampaignState(
    val completedLevels: Set<Int> = emptySet(),
    val starsByLevel: Map<Int, Int> = emptyMap(),
    val bonusStarCredits: Int = 0,
    val spentStars: Int = 0,
    val cashRateLevel: Int = 0,
    val refillRateLevel: Int = 0,
    val fleetSpeedLevel: Int = 0,
    val selectedSpecialAbility: SpecialAbilityType? = null,
    val speedBoostLevel: Int = 0,
    val defenseBoostLevel: Int = 0,
    val instantRefillLevel: Int = 0,
    val attackBoostLevel: Int = 0
) {
    val totalStars: Int get() = starsByLevel.values.sum()
    val availableStars: Int get() = (totalStars + bonusStarCredits - spentStars).coerceAtLeast(0)

    fun cashIncomeMultiplier(): Float = 1f + cashRateLevel * 0.25f
    fun playerShipProductionMultiplier(): Float = 1f + refillRateLevel * 0.25f
    fun playerFleetSpeedMultiplier(): Float = 1f + fleetSpeedLevel * 0.2f
    fun canPurchaseCampaignUpgrade(currentLevel: Int): Boolean {
        return availableStars > 0 && currentLevel < CAMPAIGN_MAX_UPGRADE_LEVEL
    }

    fun starsForLevel(levelId: Int): Int = starsByLevel[levelId] ?: 0

    fun specialAbilityLevel(type: SpecialAbilityType): Int {
        return when (type) {
            SpecialAbilityType.SPEED_BOOST -> speedBoostLevel
            SpecialAbilityType.DEFENSE_BOOST -> defenseBoostLevel
            SpecialAbilityType.INSTANT_REFILL -> instantRefillLevel
            SpecialAbilityType.ATTACK_BOOST -> attackBoostLevel
        }
    }

    fun canUpgradeSpecialAbility(type: SpecialAbilityType): Boolean {
        return canPurchaseCampaignUpgrade(specialAbilityLevel(type))
    }

    fun equippedSpecialAbilityLevel(): Int {
        return selectedSpecialAbility?.let(::specialAbilityLevel) ?: 0
    }

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

    fun selectSpecialAbility(type: SpecialAbilityType): CampaignState {
        return copy(selectedSpecialAbility = type)
    }

    fun upgradeSpecialAbility(type: SpecialAbilityType): CampaignState {
        require(canUpgradeSpecialAbility(type)) {
            "Cannot upgrade ${type.title} at level ${specialAbilityLevel(type)} with $availableStars available stars"
        }
        return spendStars(cost = CAMPAIGN_SPECIAL_ABILITY_UPGRADE_COST).let { campaign ->
            when (type) {
                SpecialAbilityType.SPEED_BOOST -> campaign.copy(speedBoostLevel = campaign.speedBoostLevel + 1)
                SpecialAbilityType.DEFENSE_BOOST -> campaign.copy(defenseBoostLevel = campaign.defenseBoostLevel + 1)
                SpecialAbilityType.INSTANT_REFILL -> campaign.copy(instantRefillLevel = campaign.instantRefillLevel + 1)
                SpecialAbilityType.ATTACK_BOOST -> campaign.copy(attackBoostLevel = campaign.attackBoostLevel + 1)
            }
        }
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
    ABILITIES,
    IN_GAME
}

internal enum class MatchStatus {
    RUNNING,
    PLAYER_WON,
    PLAYER_LOST
}

internal enum class Owner(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val isPlayer: Boolean = false,
    val isNeutral: Boolean = false,
    val isAi: Boolean = false
) {
    PLAYER(label = "Player", color = androidx.compose.ui.graphics.Color(0xFF59D0FF), isPlayer = true),
    AI_1(label = "AI 1", color = androidx.compose.ui.graphics.Color(0xFFFF7868), isAi = true),
    AI_2(label = "AI 2", color = androidx.compose.ui.graphics.Color(0xFFFFB347), isAi = true),
    AI_3(label = "AI 3", color = androidx.compose.ui.graphics.Color(0xFFBE8CFF), isAi = true),
    AI_4(label = "AI 4", color = androidx.compose.ui.graphics.Color(0xFF5DE2A5), isAi = true),
    NEUTRAL(label = "Neutral", color = androidx.compose.ui.graphics.Color(0xFF8999A8), isNeutral = true)
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
    val accent: androidx.compose.ui.graphics.Color,
    val description: String
) {
    COMMAND(
        label = "Command",
        shortLabel = "CMD",
        accent = androidx.compose.ui.graphics.Color(0xFFE8F3FF),
        description = "Balanced headquarters with no situational modifier."
    ),
    FAST(
        label = "Fast",
        shortLabel = "FST",
        accent = androidx.compose.ui.graphics.Color(0xFFBFE7FF),
        description = "Launches faster-moving ships."
    ),
    FACTORY(
        label = "Factory",
        shortLabel = "FAC",
        accent = androidx.compose.ui.graphics.Color(0xFFB8FFCF),
        description = "Produces ships faster and rewards early economic expansion."
    ),
    RELAY(
        label = "Relay",
        shortLabel = "RLY",
        accent = androidx.compose.ui.graphics.Color(0xFFFEDC8B),
        description = "Fleets sent to friendly bases travel faster and reinforce harder."
    ),
    ASSAULT(
        label = "Assault",
        shortLabel = "ATK",
        accent = androidx.compose.ui.graphics.Color(0xFFFFB1A1),
        description = "Fleets sent to neutral or enemy bases hit harder and skirmish better."
    ),
    BATTERY(
        label = "Battery",
        shortLabel = "BAT",
        accent = androidx.compose.ui.graphics.Color(0xFFC0A4FF),
        description = "Damages passing enemy fleets inside a large defensive radius."
    )
}

internal enum class SpecialAbilityType(
    val title: String,
    val subtitle: String,
    val description: String,
    val upgradeDescription: String
) {
    SPEED_BOOST(
        title = "Speed Burst",
        subtitle = "Mobility Ability",
        description = "Short boost intended to accelerate your fleets while active.",
        upgradeDescription = "Improves the strength of the temporary fleet-speed burst."
    ),
    DEFENSE_BOOST(
        title = "Defense Field",
        subtitle = "Holdout Ability",
        description = "Short defensive boost intended to harden your currently owned nodes.",
        upgradeDescription = "Improves the strength of the temporary defense increase."
    ),
    INSTANT_REFILL(
        title = "Instant Refill",
        subtitle = "Recovery Ability",
        description = "Instantly tops up one of your nodes to the cap allowed by its current level.",
        upgradeDescription = "Improves the refill by adding a short production surge after the instant top-up."
    ),
    ATTACK_BOOST(
        title = "Attack Surge",
        subtitle = "Offense Ability",
        description = "Short boost intended to increase the damage dealt by your troops.",
        upgradeDescription = "Improves the strength of the temporary damage increase."
    )
}
