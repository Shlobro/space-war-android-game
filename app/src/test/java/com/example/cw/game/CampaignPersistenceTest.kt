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
            cashRateLevel = 2
        )

        assertEquals(CampaignState(), decoded)
    }

    @Test
    fun decodeCampaignState_supportsLegacyUnversionedSaves() {
        val decoded = decodeCampaignState(
            schemaVersion = 0,
            completedLevels = "1,2",
            starsByLevel = "1:3;2:2",
            upgradePoints = 4,
            cashRateLevel = 2
        )

        assertEquals(setOf(1, 2), decoded.completedLevels)
        assertEquals(mapOf(1 to 3, 2 to 2), decoded.starsByLevel)
        assertEquals(4, decoded.upgradePoints)
        assertEquals(2, decoded.cashRateLevel)
    }

    @Test
    fun decodeIntSet_returnsEmptySetForBlankInput() {
        assertTrue(decodeIntSet(null).isEmpty())
        assertTrue(decodeIntSet("").isEmpty())
    }
}
