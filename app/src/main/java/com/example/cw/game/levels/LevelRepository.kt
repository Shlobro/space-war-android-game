package com.example.cw.game.levels

import android.content.res.AssetManager
import android.util.Log
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
        return loadEntriesIgnoringFailures(
            names = levelFileNames(),
            onFailure = ::reportLoadFailure,
            load = { fileName -> loadLevelFile(fileName).toSummary() }
        ).let(::sortLevelSummaries)
    }

    override fun loadLevel(levelId: Int): LevelDefinition {
        return loadEntriesIgnoringFailures(
            names = levelFileNames(),
            onFailure = ::reportLoadFailure,
            load = ::loadLevelFile
        ).asSequence()
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

    private fun reportLoadFailure(fileName: String, error: Throwable) {
        Log.e("LevelRepository", "Failed to load $fileName from assets/$levelsDirectory", error)
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

internal fun <T> loadEntriesIgnoringFailures(
    names: List<String>,
    onFailure: (String, Throwable) -> Unit,
    load: (String) -> T
): List<T> {
    return names.mapNotNull { name ->
        runCatching { load(name) }
            .onFailure { onFailure(name, it) }
            .getOrNull()
    }
}
