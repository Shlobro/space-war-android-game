package com.example.cw.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignPersistenceTest {
    @Test
    fun encodeAndDecodeCampaignCollections_roundTrip() {
        val completedLevels = setOf(3, 1, 2)
        val starsByLevel = mapOf(2 to 3, 1 to 2)

        assertEquals(completedLevels, decodeIntSet(encodeIntSet(completedLevels)))
        assertEquals(starsByLevel, decodeStarsByLevel(encodeStarsByLevel(starsByLevel)))
    }

    @Test
    fun decodeStarsByLevel_ignoresInvalidEntries() {
        val decoded = decodeStarsByLevel("1:2;bad;3:0;4:5;7:3")

        assertEquals(mapOf(1 to 2, 7 to 3), decoded)
    }

    @Test
    fun decodeCampaignState_returnsEmptyStateForUnsupportedSchemaVersion() {
        val decoded = decodeCampaignState(
            schemaVersion = 9,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 4,
            bonusStarCredits = 0,
            spentStars = 0,
            cashRateLevel = 2,
            refillRateLevel = 1,
            fleetSpeedLevel = 3,
            selectedSpecialAbility = SpecialAbilityType.DEFENSE_BOOST.name,
            speedBoostLevel = 1,
            defenseBoostLevel = 2,
            instantRefillLevel = 3,
            attackBoostLevel = 4
        )

        assertEquals(CampaignState(), decoded)
    }

    @Test
    fun decodeCampaignState_supportsLegacyUnversionedSavesWithoutInventingSpendableStars() {
        val decoded = decodeCampaignState(
            schemaVersion = 0,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 4,
            bonusStarCredits = 0,
            spentStars = 0,
            cashRateLevel = 2,
            refillRateLevel = 1,
            fleetSpeedLevel = 3,
            selectedSpecialAbility = SpecialAbilityType.DEFENSE_BOOST.name,
            speedBoostLevel = 1,
            defenseBoostLevel = 2,
            instantRefillLevel = 3,
            attackBoostLevel = 4
        )

        assertEquals(setOf(1, 2), decoded.completedLevels)
        assertEquals(mapOf(1 to 3, 2 to 2), decoded.starsByLevel)
        assertEquals(0, decoded.bonusStarCredits)
        assertEquals(5, decoded.availableStars)
        assertEquals(0, decoded.spentStars)
        assertEquals(0, decoded.cashRateLevel)
        assertEquals(0, decoded.refillRateLevel)
        assertEquals(0, decoded.fleetSpeedLevel)
        assertEquals(null, decoded.selectedSpecialAbility)
        assertEquals(0, decoded.speedBoostLevel)
        assertEquals(0, decoded.defenseBoostLevel)
        assertEquals(0, decoded.instantRefillLevel)
        assertEquals(0, decoded.attackBoostLevel)
    }

    @Test
    fun decodeCampaignState_supportsCashRateLevelForSchemaTwoAndLater() {
        val decoded = decodeCampaignState(
            schemaVersion = 2,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 0,
            bonusStarCredits = 1,
            spentStars = 3,
            cashRateLevel = 2,
            refillRateLevel = 4,
            fleetSpeedLevel = 1,
            selectedSpecialAbility = SpecialAbilityType.SPEED_BOOST.name,
            speedBoostLevel = 1,
            defenseBoostLevel = 2,
            instantRefillLevel = 3,
            attackBoostLevel = 4
        )

        assertEquals(2, decoded.cashRateLevel)
        assertEquals(0, decoded.refillRateLevel)
        assertEquals(0, decoded.fleetSpeedLevel)
    }

    @Test
    fun decodeCampaignState_supportsCurrentSpendableStarSchema() {
        val decoded = decodeCampaignState(
            schemaVersion = 4,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 0,
            bonusStarCredits = 1,
            spentStars = 3,
            cashRateLevel = 2,
            refillRateLevel = 4,
            fleetSpeedLevel = 1,
            selectedSpecialAbility = SpecialAbilityType.INSTANT_REFILL.name,
            speedBoostLevel = 2,
            defenseBoostLevel = 1,
            instantRefillLevel = 4,
            attackBoostLevel = 3
        )

        assertEquals(1, decoded.bonusStarCredits)
        assertEquals(3, decoded.spentStars)
        assertEquals(3, decoded.availableStars)
        assertEquals(4, decoded.refillRateLevel)
        assertEquals(1, decoded.fleetSpeedLevel)
        assertEquals(SpecialAbilityType.INSTANT_REFILL, decoded.selectedSpecialAbility)
        assertEquals(2, decoded.speedBoostLevel)
        assertEquals(1, decoded.defenseBoostLevel)
        assertEquals(4, decoded.instantRefillLevel)
        assertEquals(3, decoded.attackBoostLevel)
    }

    @Test
    fun decodeCampaignState_ignoresNewUpgradeLevelsForPreSchemaThreeSaves() {
        val decoded = decodeCampaignState(
            schemaVersion = 2,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 0,
            bonusStarCredits = 1,
            spentStars = 3,
            cashRateLevel = 2,
            refillRateLevel = 4,
            fleetSpeedLevel = 1,
            selectedSpecialAbility = SpecialAbilityType.ATTACK_BOOST.name,
            speedBoostLevel = 2,
            defenseBoostLevel = 3,
            instantRefillLevel = 4,
            attackBoostLevel = 5
        )

        assertEquals(2, decoded.cashRateLevel)
        assertEquals(0, decoded.refillRateLevel)
        assertEquals(0, decoded.fleetSpeedLevel)
        assertEquals(null, decoded.selectedSpecialAbility)
        assertEquals(0, decoded.speedBoostLevel)
        assertEquals(0, decoded.defenseBoostLevel)
        assertEquals(0, decoded.instantRefillLevel)
        assertEquals(0, decoded.attackBoostLevel)
    }

    @Test
    fun decodeIntSet_returnsEmptySetForBlankInput() {
        assertTrue(decodeIntSet(null).isEmpty())
        assertTrue(decodeIntSet("").isEmpty())
    }

    @Test
    fun decodeSpecialAbilityType_returnsNullForUnknownValue() {
        assertEquals(null, decodeSpecialAbilityType("missing"))
    }

    @Test
    fun campaignState_selectsOneSpecialAbilityAtATime() {
        val campaign = CampaignState()
            .selectSpecialAbility(SpecialAbilityType.SPEED_BOOST)
            .selectSpecialAbility(SpecialAbilityType.ATTACK_BOOST)

        assertEquals(SpecialAbilityType.ATTACK_BOOST, campaign.selectedSpecialAbility)
    }

    @Test
    fun campaignState_upgradesSpecialAbilityAndSpendsAStar() {
        val campaign = CampaignState(
            starsByLevel = mapOf(1 to 3),
            selectedSpecialAbility = SpecialAbilityType.DEFENSE_BOOST
        )

        val upgraded = campaign.upgradeSpecialAbility(SpecialAbilityType.DEFENSE_BOOST)

        assertEquals(1, upgraded.defenseBoostLevel)
        assertEquals(1, upgraded.spentStars)
        assertEquals(2, upgraded.availableStars)
    }

    @Test
    fun decodeCampaignState_clampsAbilityLevelsToDocumentedMaximum() {
        val decoded = decodeCampaignState(
            schemaVersion = 4,
            completedLevels = null,
            starsByLevel = null,
            upgradePoints = 0,
            bonusStarCredits = 0,
            spentStars = 0,
            cashRateLevel = 9,
            refillRateLevel = 8,
            fleetSpeedLevel = 7,
            selectedSpecialAbility = SpecialAbilityType.ATTACK_BOOST.name,
            speedBoostLevel = 12,
            defenseBoostLevel = 10,
            instantRefillLevel = 8,
            attackBoostLevel = 99
        )

        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.cashRateLevel)
        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.refillRateLevel)
        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.fleetSpeedLevel)
        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.speedBoostLevel)
        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.defenseBoostLevel)
        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.instantRefillLevel)
        assertEquals(CAMPAIGN_MAX_UPGRADE_LEVEL, decoded.attackBoostLevel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun campaignState_upgradeSpecialAbilityRejectsInsufficientStars() {
        CampaignState().upgradeSpecialAbility(SpecialAbilityType.SPEED_BOOST)
    }
}
