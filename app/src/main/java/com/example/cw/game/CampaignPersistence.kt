package com.example.cw.game

import android.content.SharedPreferences

private const val CAMPAIGN_SCHEMA_VERSION_KEY = "schema_version"
private const val CAMPAIGN_COMPLETED_LEVELS_KEY = "completed_levels"
private const val CAMPAIGN_STARS_BY_LEVEL_KEY = "stars_by_level"
private const val CAMPAIGN_UPGRADE_POINTS_KEY = "upgrade_points"
private const val CAMPAIGN_BONUS_STAR_CREDITS_KEY = "bonus_star_credits"
private const val CAMPAIGN_SPENT_STARS_KEY = "spent_stars"
private const val CAMPAIGN_CASH_RATE_LEVEL_KEY = "cash_rate_level"
private const val CAMPAIGN_REFILL_RATE_LEVEL_KEY = "refill_rate_level"
private const val CAMPAIGN_FLEET_SPEED_LEVEL_KEY = "fleet_speed_level"
private const val CAMPAIGN_SCHEMA_VERSION = 3

internal const val CAMPAIGN_PREFERENCES_NAME = "campaign_state"

internal fun loadCampaignState(preferences: SharedPreferences): CampaignState {
    return decodeCampaignState(
        schemaVersion = preferences.getInt(CAMPAIGN_SCHEMA_VERSION_KEY, 0),
        completedLevels = preferences.getString(CAMPAIGN_COMPLETED_LEVELS_KEY, null),
        starsByLevel = preferences.getString(CAMPAIGN_STARS_BY_LEVEL_KEY, null),
        upgradePoints = preferences.getInt(CAMPAIGN_UPGRADE_POINTS_KEY, 0),
        bonusStarCredits = preferences.getInt(CAMPAIGN_BONUS_STAR_CREDITS_KEY, 0),
        spentStars = preferences.getInt(CAMPAIGN_SPENT_STARS_KEY, 0),
        cashRateLevel = preferences.getInt(CAMPAIGN_CASH_RATE_LEVEL_KEY, 0),
        refillRateLevel = preferences.getInt(CAMPAIGN_REFILL_RATE_LEVEL_KEY, 0),
        fleetSpeedLevel = preferences.getInt(CAMPAIGN_FLEET_SPEED_LEVEL_KEY, 0)
    )
}

internal fun saveCampaignState(preferences: SharedPreferences, campaign: CampaignState) {
    preferences.edit()
        .putInt(CAMPAIGN_SCHEMA_VERSION_KEY, CAMPAIGN_SCHEMA_VERSION)
        .putString(CAMPAIGN_COMPLETED_LEVELS_KEY, encodeIntSet(campaign.completedLevels))
        .putString(CAMPAIGN_STARS_BY_LEVEL_KEY, encodeStarsByLevel(campaign.starsByLevel))
        .putInt(CAMPAIGN_BONUS_STAR_CREDITS_KEY, campaign.bonusStarCredits)
        .putInt(CAMPAIGN_SPENT_STARS_KEY, campaign.spentStars)
        .remove(CAMPAIGN_UPGRADE_POINTS_KEY)
        .putInt(CAMPAIGN_CASH_RATE_LEVEL_KEY, campaign.cashRateLevel)
        .putInt(CAMPAIGN_REFILL_RATE_LEVEL_KEY, campaign.refillRateLevel)
        .putInt(CAMPAIGN_FLEET_SPEED_LEVEL_KEY, campaign.fleetSpeedLevel)
        .apply()
}

internal fun decodeCampaignState(
    schemaVersion: Int,
    completedLevels: String?,
    starsByLevel: String?,
    upgradePoints: Int,
    bonusStarCredits: Int,
    spentStars: Int,
    cashRateLevel: Int,
    refillRateLevel: Int,
    fleetSpeedLevel: Int
): CampaignState {
    if (!isSupportedCampaignSchemaVersion(schemaVersion)) {
        return CampaignState()
    }

    val migratedBonusStars = if (schemaVersion == 0 || schemaVersion == 1) 0 else bonusStarCredits.coerceAtLeast(0)
    val migratedSpentStars = if (schemaVersion == 0 || schemaVersion == 1) 0 else spentStars.coerceAtLeast(0)

    val migratedRefillRateLevel = if (schemaVersion < 3) 0 else refillRateLevel.coerceAtLeast(0)
    val migratedFleetSpeedLevel = if (schemaVersion < 3) 0 else fleetSpeedLevel.coerceAtLeast(0)

    return CampaignState(
        completedLevels = decodeIntSet(completedLevels),
        starsByLevel = decodeStarsByLevel(starsByLevel),
        bonusStarCredits = migratedBonusStars,
        spentStars = migratedSpentStars,
        cashRateLevel = cashRateLevel.coerceAtLeast(0),
        refillRateLevel = migratedRefillRateLevel,
        fleetSpeedLevel = migratedFleetSpeedLevel
    )
}

internal fun isSupportedCampaignSchemaVersion(schemaVersion: Int): Boolean {
    return schemaVersion in 0..CAMPAIGN_SCHEMA_VERSION
}

internal fun encodeIntSet(values: Set<Int>): String {
    return values
        .sorted()
        .joinToString(",")
}

internal fun decodeIntSet(encoded: String?): Set<Int> {
    if (encoded.isNullOrBlank()) return emptySet()
    return encoded.split(",")
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .toSet()
}

internal fun encodeStarsByLevel(values: Map<Int, Int>): String {
    return values.entries
        .sortedBy { it.key }
        .joinToString(";") { (levelId, stars) -> "$levelId:$stars" }
}

internal fun decodeStarsByLevel(encoded: String?): Map<Int, Int> {
    if (encoded.isNullOrBlank()) return emptyMap()
    return encoded.split(";")
        .mapNotNull { entry ->
            val parts = entry.split(":")
            val levelId = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            val stars = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            if (stars !in 1..3) {
                return@mapNotNull null
            }
            levelId to stars
        }
        .toMap()
}
