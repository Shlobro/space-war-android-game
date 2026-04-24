package com.example.cw.game.levels

import com.example.cw.game.AiType
import com.example.cw.game.BaseType
import com.example.cw.game.CampaignState
import com.example.cw.game.Owner
import com.example.cw.game.createMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRepositoryTest {
    @Test
    fun decode_parsesValidLevelJson() {
        val level = LevelJson.decode(validJson(levelId = 3, sortOrder = 2))

        assertEquals(3, level.levelId)
        assertEquals("Level 3", level.name)
        assertEquals(3, level.bases.size)
        assertEquals(1, level.obstacles.size)
        assertEquals(BaseType.COMMAND, level.bases.first().type)
    }

    @Test
    fun decode_rejectsDuplicateBaseIds() {
        val json = validJson(levelId = 1, sortOrder = 1).replace(
            "\"id\": 3",
            "\"id\": 2"
        )

        val error = runCatching { LevelJson.decode(json) }.exceptionOrNull()

        assertTrue(error is LevelParseException)
        assertTrue(error?.message?.contains("Duplicate base id") == true)
    }

    @Test
    fun decode_rejectsCapLevelExceedingMaxLevel() {
        val json = validJson(levelId = 1, sortOrder = 1).replace(
            "\"capLevel\": 2, \"maxLevel\": 10",
            "\"capLevel\": 5, \"maxLevel\": 2"
        )

        val error = runCatching { LevelJson.decode(json) }.exceptionOrNull()

        assertTrue(error is LevelParseException)
        assertTrue(error?.message?.contains("capLevel cannot exceed maxLevel") == true)
    }

    @Test
    fun decode_rejectsAiOwnedBaseWithoutController() {
        val json = validJson(levelId = 1, sortOrder = 1).replace(
            "{ \"owner\": \"AI_1\", \"type\": \"STANDARD\" }",
            ""
        )

        val error = runCatching { LevelJson.decode(json) }.exceptionOrNull()

        assertTrue(error is LevelParseException)
        assertTrue(error?.message?.contains("without an AI controller entry") == true)
    }

    @Test
    fun sortLevelSummaries_ordersBySortOrderThenLevelId() {
        val sorted = sortLevelSummaries(
            listOf(
                LevelSummary(2, "B", "B", 2, null),
                LevelSummary(1, "A", "A", 1, null),
                LevelSummary(4, "D", "D", 2, null)
            )
        )

        assertEquals(listOf(1, 2, 4), sorted.map { it.levelId })
    }

    @Test
    fun isLevelUnlocked_usesUnlockAfterLevelId() {
        val locked = LevelSummary(2, "Level 2", "Second", 2, 1)
        val campaign = CampaignState(completedLevels = emptySet())

        assertFalse(isLevelUnlocked(locked, campaign))
        assertTrue(isLevelUnlocked(locked, campaign.copy(completedLevels = setOf(1))))
    }

    @Test
    fun createMatch_preservesLevelValues() {
        val level = LevelDefinition(
            schemaVersion = 2,
            levelId = 9,
            name = "Test Level",
            description = "Desc",
            sortOrder = 9,
            unlockAfterLevelId = null,
            worldBounds = WorldBounds(1200f, 1800f),
            introMessage = "Testing",
            aiControllers = listOf(LevelAiDefinition(Owner.AI_1, AiType.STANDARD)),
            bases = listOf(
                LevelBaseDefinition(1, 100f, 150f, Owner.PLAYER, BaseType.FAST, 25f, 6),
                LevelBaseDefinition(2, 900f, 200f, Owner.AI_1, BaseType.COMMAND, 30f, 5)
            ),
            obstacles = listOf(LevelObstacleDefinition(500f, 600f, 90f))
        )

        val match = createMatch(level)

        assertEquals(9, match.levelId)
        assertEquals("Test Level", match.levelName)
        assertEquals(1200f, match.worldBounds.width)
        assertEquals(1800f, match.worldBounds.height)
        assertEquals("Testing", match.message)
        assertEquals(BASE_RADIUS_MIN + (6 - 1) * RADIUS_PER_LEVEL, match.bases.first().radius)
        assertEquals(BaseType.FAST, match.bases.first().type)
        assertEquals(90f, match.obstacles.single().radius)
    }

    @Test
    fun loadEntriesIgnoringFailures_reportsAndSkipsFailures() {
        val failures = mutableListOf<String>()

        val loaded = loadEntriesIgnoringFailures(
            names = listOf("good.json", "broken.json", "other.json"),
            onFailure = { name, _ -> failures += name },
            load = { name ->
                when (name) {
                    "broken.json" -> throw LevelParseException("Broken file")
                    else -> name.removeSuffix(".json")
                }
            }
        )

        assertEquals(listOf("good", "other"), loaded)
        assertEquals(listOf("broken.json"), failures)
    }

    private fun validJson(levelId: Int, sortOrder: Int): String {
        return """
            {
              "schemaVersion": 2,
              "levelId": $levelId,
              "name": "Level $levelId",
              "description": "Description $levelId",
              "sortOrder": $sortOrder,
              "unlockAfterLevelId": null,
              "worldWidth": 1000,
              "worldHeight": 1600,
              "introMessage": "Start",
              "aiControllers": [
                { "owner": "AI_1", "type": "STANDARD" }
              ],
              "bases": [
                { "id": 1, "x": 100, "y": 1500, "owner": "PLAYER", "type": "COMMAND", "units": 20, "capLevel": 2, "maxLevel": 10 },
                { "id": 2, "x": 900, "y": 100, "owner": "AI_1", "type": "COMMAND", "units": 20, "capLevel": 2, "maxLevel": 10 },
                { "id": 3, "x": 500, "y": 800, "owner": "NEUTRAL", "type": "COMMAND", "units": 10, "capLevel": 1, "maxLevel": 10 }
              ],
              "obstacles": [
                { "x": 450, "y": 700, "radius": 60 }
              ]
            }
        """.trimIndent()
    }
}
