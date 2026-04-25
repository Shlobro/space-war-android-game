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
            fleetSpeedLevel = 3
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
            fleetSpeedLevel = 3
        )

        assertEquals(setOf(1, 2), decoded.completedLevels)
        assertEquals(mapOf(1 to 3, 2 to 2), decoded.starsByLevel)
        assertEquals(0, decoded.bonusStarCredits)
        assertEquals(5, decoded.availableStars)
        assertEquals(0, decoded.spentStars)
        assertEquals(0, decoded.cashRateLevel)
        assertEquals(0, decoded.refillRateLevel)
        assertEquals(0, decoded.fleetSpeedLevel)
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
            fleetSpeedLevel = 1
        )

        assertEquals(2, decoded.cashRateLevel)
        assertEquals(0, decoded.refillRateLevel)
        assertEquals(0, decoded.fleetSpeedLevel)
    }

    @Test
    fun decodeCampaignState_supportsCurrentSpendableStarSchema() {
        val decoded = decodeCampaignState(
            schemaVersion = 3,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 0,
            bonusStarCredits = 1,
            spentStars = 3,
            cashRateLevel = 2,
            refillRateLevel = 4,
            fleetSpeedLevel = 1
        )

        assertEquals(1, decoded.bonusStarCredits)
        assertEquals(3, decoded.spentStars)
        assertEquals(3, decoded.availableStars)
        assertEquals(4, decoded.refillRateLevel)
        assertEquals(1, decoded.fleetSpeedLevel)
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
            fleetSpeedLevel = 1
        )

        assertEquals(2, decoded.cashRateLevel)
        assertEquals(0, decoded.refillRateLevel)
        assertEquals(0, decoded.fleetSpeedLevel)
    }

    @Test
    fun decodeIntSet_returnsEmptySetForBlankInput() {
        assertTrue(decodeIntSet(null).isEmpty())
        assertTrue(decodeIntSet("").isEmpty())
    }
}
