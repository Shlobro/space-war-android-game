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
private const val CAMPAIGN_SELECTED_SPECIAL_ABILITY_KEY = "selected_special_ability"
private const val CAMPAIGN_SPEED_BOOST_LEVEL_KEY = "speed_boost_level"
private const val CAMPAIGN_DEFENSE_BOOST_LEVEL_KEY = "defense_boost_level"
private const val CAMPAIGN_INSTANT_REFILL_LEVEL_KEY = "instant_refill_level"
private const val CAMPAIGN_ATTACK_BOOST_LEVEL_KEY = "attack_boost_level"
private const val CAMPAIGN_SCHEMA_VERSION = 4

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
        fleetSpeedLevel = preferences.getInt(CAMPAIGN_FLEET_SPEED_LEVEL_KEY, 0),
        selectedSpecialAbility = preferences.getString(CAMPAIGN_SELECTED_SPECIAL_ABILITY_KEY, null),
        speedBoostLevel = preferences.getInt(CAMPAIGN_SPEED_BOOST_LEVEL_KEY, 0),
        defenseBoostLevel = preferences.getInt(CAMPAIGN_DEFENSE_BOOST_LEVEL_KEY, 0),
        instantRefillLevel = preferences.getInt(CAMPAIGN_INSTANT_REFILL_LEVEL_KEY, 0),
        attackBoostLevel = preferences.getInt(CAMPAIGN_ATTACK_BOOST_LEVEL_KEY, 0)
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
        .putString(CAMPAIGN_SELECTED_SPECIAL_ABILITY_KEY, campaign.selectedSpecialAbility?.name)
        .putInt(CAMPAIGN_SPEED_BOOST_LEVEL_KEY, campaign.speedBoostLevel)
        .putInt(CAMPAIGN_DEFENSE_BOOST_LEVEL_KEY, campaign.defenseBoostLevel)
        .putInt(CAMPAIGN_INSTANT_REFILL_LEVEL_KEY, campaign.instantRefillLevel)
        .putInt(CAMPAIGN_ATTACK_BOOST_LEVEL_KEY, campaign.attackBoostLevel)
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
    fleetSpeedLevel: Int,
    selectedSpecialAbility: String?,
    speedBoostLevel: Int,
    defenseBoostLevel: Int,
    instantRefillLevel: Int,
    attackBoostLevel: Int
): CampaignState {
    if (!isSupportedCampaignSchemaVersion(schemaVersion)) {
        return CampaignState()
    }

    // Every persisted CampaignState field must be schema-gated here when it is introduced.
    // Reading newer keys from older save versions should default to a safe zero-value instead.
    val migratedBonusStars = if (schemaVersion == 0 || schemaVersion == 1) 0 else bonusStarCredits.coerceAtLeast(0)
    val migratedSpentStars = if (schemaVersion == 0 || schemaVersion == 1) 0 else spentStars.coerceAtLeast(0)
    val migratedCashRateLevel = if (schemaVersion < 2) 0 else cashRateLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)
    val migratedRefillRateLevel = if (schemaVersion < 3) 0 else refillRateLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)
    val migratedFleetSpeedLevel = if (schemaVersion < 3) 0 else fleetSpeedLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)
    val migratedSelectedSpecialAbility = if (schemaVersion < 4) null else decodeSpecialAbilityType(selectedSpecialAbility)
    val migratedSpeedBoostLevel = if (schemaVersion < 4) 0 else speedBoostLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)
    val migratedDefenseBoostLevel = if (schemaVersion < 4) 0 else defenseBoostLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)
    val migratedInstantRefillLevel = if (schemaVersion < 4) 0 else instantRefillLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)
    val migratedAttackBoostLevel = if (schemaVersion < 4) 0 else attackBoostLevel.coerceIn(0, CAMPAIGN_MAX_UPGRADE_LEVEL)

    return CampaignState(
        completedLevels = decodeIntSet(completedLevels),
        starsByLevel = decodeStarsByLevel(starsByLevel),
        bonusStarCredits = migratedBonusStars,
        spentStars = migratedSpentStars,
        cashRateLevel = migratedCashRateLevel,
        refillRateLevel = migratedRefillRateLevel,
        fleetSpeedLevel = migratedFleetSpeedLevel,
        selectedSpecialAbility = migratedSelectedSpecialAbility,
        speedBoostLevel = migratedSpeedBoostLevel,
        defenseBoostLevel = migratedDefenseBoostLevel,
        instantRefillLevel = migratedInstantRefillLevel,
        attackBoostLevel = migratedAttackBoostLevel
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

internal fun decodeSpecialAbilityType(encoded: String?): SpecialAbilityType? {
    return encoded?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { token -> SpecialAbilityType.entries.firstOrNull { it.name == token } }
}
