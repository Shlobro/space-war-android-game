package com.example.cw.game.levels

import android.content.res.AssetManager
import com.example.cw.game.CampaignState

internal interface LevelRepository {
    fun listLevels(): List<LevelSummary>
    fun loadLevel(levelId: Int): LevelDefinition
}

internal class AssetLevelRepository(
    private val assetManager: AssetManager,
    private val levelsDirectory: String = "levels"
) : LevelRepository {
    override fun listLevels(): List<LevelSummary> {
        return levelFileNames().mapNotNull { fileName ->
            runCatching { loadLevelFile(fileName).toSummary() }.getOrNull()
        }.let(::sortLevelSummaries)
    }

    override fun loadLevel(levelId: Int): LevelDefinition {
        return levelFileNames().asSequence()
            .map(::loadLevelFile)
            .firstOrNull { it.levelId == levelId }
            ?: throw LevelParseException("Level $levelId was not found in assets/$levelsDirectory")
    }

    private fun levelFileNames(): List<String> {
        return assetManager.list(levelsDirectory)
            ?.filter { it.endsWith(".json", ignoreCase = true) }
            ?.sorted()
            .orEmpty()
    }

    private fun loadLevelFile(fileName: String): LevelDefinition {
        val path = "$levelsDirectory/$fileName"
        val json = assetManager.open(path).bufferedReader().use { it.readText() }
        return LevelJson.decode(json)
    }
}

internal class InMemoryLevelRepository(levels: List<LevelDefinition>) : LevelRepository {
    private val levelsById = levels.associateBy { it.levelId }
    private val summaries = sortLevelSummaries(levels.map(LevelDefinition::toSummary))

    override fun listLevels(): List<LevelSummary> = summaries

    override fun loadLevel(levelId: Int): LevelDefinition {
        return levelsById[levelId] ?: throw LevelParseException("Unknown level $levelId")
    }
}

internal fun sortLevelSummaries(levels: List<LevelSummary>): List<LevelSummary> {
    return levels.sortedWith(compareBy(LevelSummary::sortOrder, LevelSummary::levelId))
}

internal fun isLevelUnlocked(level: LevelSummary, campaign: CampaignState): Boolean {
    return level.unlockAfterLevelId == null || level.unlockAfterLevelId in campaign.completedLevels
}
