package com.example.cw.game.levels

import com.example.cw.game.AiType
import com.example.cw.game.BaseType
import com.example.cw.game.Owner

internal const val LEVEL_SCHEMA_VERSION = 2
internal const val DEFAULT_WORLD_WIDTH = 1000f
internal const val DEFAULT_WORLD_HEIGHT = 1600f
internal const val BASE_RADIUS_MIN = 36f
internal const val RADIUS_PER_LEVEL = 6f
internal const val DEFAULT_MAX_LEVEL = 10

internal data class WorldBounds(
    val width: Float = DEFAULT_WORLD_WIDTH,
    val height: Float = DEFAULT_WORLD_HEIGHT
)

internal data class LevelDefinition(
    val schemaVersion: Int,
    val levelId: Int,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val unlockAfterLevelId: Int?,
    val worldBounds: WorldBounds,
    val introMessage: String,
    val aiControllers: List<LevelAiDefinition>,
    val bases: List<LevelBaseDefinition>,
    val obstacles: List<LevelObstacleDefinition>
) {
    fun toSummary(): LevelSummary {
        return LevelSummary(
            levelId = levelId,
            name = name,
            description = description,
            sortOrder = sortOrder,
            unlockAfterLevelId = unlockAfterLevelId
        )
    }
}

internal data class LevelSummary(
    val levelId: Int,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val unlockAfterLevelId: Int?
)

internal data class LevelAiDefinition(
    val owner: Owner,
    val type: AiType
)

internal data class LevelBaseDefinition(
    val id: Int,
    val x: Float,
    val y: Float,
    val owner: Owner,
    val type: BaseType,
    val units: Float,
    val capLevel: Int,
    val maxLevel: Int = DEFAULT_MAX_LEVEL
) {
    val cap: Int get() = capLevel * 10
    val radius: Float get() = BASE_RADIUS_MIN + (capLevel - 1) * RADIUS_PER_LEVEL
}

internal data class LevelObstacleDefinition(
    val x: Float,
    val y: Float,
    val radius: Float
)
