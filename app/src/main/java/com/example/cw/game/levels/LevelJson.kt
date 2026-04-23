package com.example.cw.game.levels

import com.example.cw.game.AiType
import com.example.cw.game.BaseType
import com.example.cw.game.Owner

internal class LevelParseException(message: String) : IllegalArgumentException(message)

internal object LevelJson {
    fun decode(text: String): LevelDefinition {
        val root = JsonParser(text).parseObject()

        val level = LevelDefinition(
            schemaVersion = root.int("schemaVersion"),
            levelId = root.int("levelId"),
            name = root.string("name"),
            description = root.string("description"),
            sortOrder = root.int("sortOrder"),
            unlockAfterLevelId = root.optionalInt("unlockAfterLevelId"),
            worldBounds = WorldBounds(
                width = root.float("worldWidth"),
                height = root.float("worldHeight")
            ),
            introMessage = root.string("introMessage"),
            aiControllers = root.array("aiControllers").mapIndexed { index, value ->
                val controller = value.asObject("aiControllers[$index]")
                val ownerName = controller.string("owner")
                val typeName = controller.string("type")
                val owner = enumValueOfOrNull<Owner>(ownerName)
                    ?: throw LevelParseException("Unknown AI owner '$ownerName' in aiControllers[$index]")
                LevelAiDefinition(
                    owner = owner.takeIf { it.isAi }
                        ?: throw LevelParseException("aiControllers[$index] owner must be an AI owner"),
                    type = enumValueOfOrNull<AiType>(typeName)
                        ?: throw LevelParseException("Unknown AI type '$typeName' in aiControllers[$index]")
                )
            },
            bases = root.array("bases").mapIndexed { index, value ->
                val base = value.asObject("bases[$index]")
                val ownerName = base.string("owner")
                val typeName = base.string("type")
                LevelBaseDefinition(
                    id = base.int("id"),
                    x = base.float("x"),
                    y = base.float("y"),
                    owner = enumValueOfOrNull<Owner>(ownerName)
                        ?: throw LevelParseException("Unknown owner '$ownerName' in bases[$index]"),
                    type = enumValueOfOrNull<BaseType>(typeName)
                        ?: throw LevelParseException("Unknown base type '$typeName' in bases[$index]"),
                    units = base.float("units"),
                    cap = base.int("cap"),
                    capLevel = base.int("capLevel"),
                    radius = base.optionalFloat("radius") ?: DEFAULT_BASE_RADIUS
                )
            },
            obstacles = root.array("obstacles").mapIndexed { index, value ->
                val obstacle = value.asObject("obstacles[$index]")
                LevelObstacleDefinition(
                    x = obstacle.float("x"),
                    y = obstacle.float("y"),
                    radius = obstacle.float("radius")
                )
            }
        )

        validateLevel(level)
        return level
    }

    private fun validateLevel(level: LevelDefinition) {
        if (level.schemaVersion != LEVEL_SCHEMA_VERSION) {
            throw LevelParseException(
                "Unsupported schemaVersion ${level.schemaVersion}; expected $LEVEL_SCHEMA_VERSION"
            )
        }
        if (level.worldBounds.width <= 0f || level.worldBounds.height <= 0f) {
            throw LevelParseException("worldWidth and worldHeight must be positive")
        }
        if (level.name.isBlank()) {
            throw LevelParseException("name must not be blank")
        }
        if (level.description.isBlank()) {
            throw LevelParseException("description must not be blank")
        }
        if (level.introMessage.isBlank()) {
            throw LevelParseException("introMessage must not be blank")
        }
        if (level.bases.none { it.owner == Owner.PLAYER }) {
            throw LevelParseException("At least one player base is required")
        }
        if (level.bases.none { it.owner.isAi }) {
            throw LevelParseException("At least one AI-owned base is required")
        }

        val configuredAiOwners = level.aiControllers.map { it.owner }
        if (configuredAiOwners.size != configuredAiOwners.distinct().size) {
            throw LevelParseException("AI controller owners must be unique")
        }

        val seenBaseIds = mutableSetOf<Int>()
        level.bases.forEach { base ->
            if (!seenBaseIds.add(base.id)) {
                throw LevelParseException("Duplicate base id ${base.id}")
            }
            if (base.x !in 0f..level.worldBounds.width || base.y !in 0f..level.worldBounds.height) {
                throw LevelParseException("Base ${base.id} is outside world bounds")
            }
            if (base.radius <= 0f) {
                throw LevelParseException("Base ${base.id} radius must be positive")
            }
            if (base.units < 0f) {
                throw LevelParseException("Base ${base.id} units must be non-negative")
            }
            if (base.cap < 1) {
                throw LevelParseException("Base ${base.id} cap must be at least 1")
            }
            if (base.capLevel < 1) {
                throw LevelParseException("Base ${base.id} capLevel must be at least 1")
            }
            if (base.cap != com.example.cw.game.capacityForLevel(base.capLevel)) {
                throw LevelParseException(
                    "Base ${base.id} cap must equal capLevel * 10"
                )
            }
            if (base.owner.isAi && base.owner !in configuredAiOwners) {
                throw LevelParseException("Base ${base.id} uses ${base.owner.name} without an AI controller entry")
            }
        }

        level.aiControllers.forEach { controller ->
            if (level.bases.none { it.owner == controller.owner }) {
                throw LevelParseException("${controller.owner.name} is configured as an AI controller but owns no bases")
            }
        }

        level.obstacles.forEachIndexed { index, obstacle ->
            if (obstacle.x !in 0f..level.worldBounds.width || obstacle.y !in 0f..level.worldBounds.height) {
                throw LevelParseException("Obstacle $index is outside world bounds")
            }
            if (obstacle.radius <= 0f) {
                throw LevelParseException("Obstacle $index radius must be positive")
            }
        }
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? {
    return enumValues<T>().firstOrNull { it.name == name }
}

private sealed interface JsonValue {
    data class JsonObject(val fields: Map<String, JsonValue>) : JsonValue
    data class JsonArray(val items: List<JsonValue>) : JsonValue
    data class JsonString(val value: String) : JsonValue
    data class JsonNumber(val value: Double) : JsonValue
    data object JsonNull : JsonValue
}

private class JsonParser(private val text: String) {
    private var index = 0

    fun parseObject(): JsonValue.JsonObject {
        val value = parseValue()
        skipWhitespace()
        if (index != text.length) {
            error("Unexpected trailing content")
        }
        return value.asObject("root")
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        if (index >= text.length) error("Unexpected end of input")
        return when (val current = text[index]) {
            '{' -> parseObjectInternal()
            '[' -> parseArray()
            '"' -> JsonValue.JsonString(parseString())
            in '0'..'9', '-', '+' -> JsonValue.JsonNumber(parseNumber())
            'n' -> parseLiteral("null", JsonValue.JsonNull)
            else -> error("Unexpected character '$current'")
        }
    }

    private fun parseObjectInternal(): JsonValue.JsonObject {
        expect('{')
        skipWhitespace()
        val fields = linkedMapOf<String, JsonValue>()
        if (peek('}')) {
            expect('}')
            return JsonValue.JsonObject(fields)
        }
        while (true) {
            val key = parseString()
            expect(':')
            fields[key] = parseValue()
            when {
                peek('}') -> {
                    expect('}')
                    return JsonValue.JsonObject(fields)
                }

                peek(',') -> expect(',')
                else -> error("Expected ',' or '}'")
            }
        }
    }

    private fun parseArray(): JsonValue.JsonArray {
        expect('[')
        skipWhitespace()
        val items = mutableListOf<JsonValue>()
        if (peek(']')) {
            expect(']')
            return JsonValue.JsonArray(items)
        }
        while (true) {
            items += parseValue()
            when {
                peek(']') -> {
                    expect(']')
                    return JsonValue.JsonArray(items)
                }

                peek(',') -> expect(',')
                else -> error("Expected ',' or ']'")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < text.length) {
            val current = text[index++]
            when (current) {
                '"' -> return result.toString()
                '\\' -> {
                    if (index >= text.length) error("Unterminated escape sequence")
                    val escaped = text[index++]
                    result.append(
                        when (escaped) {
                            '"', '\\', '/' -> escaped
                            'b' -> '\b'
                            'f' -> '\u000C'
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'u' -> parseUnicodeEscape()
                            else -> error("Unsupported escape sequence '\\$escaped'")
                        }
                    )
                }

                else -> result.append(current)
            }
        }
        error("Unterminated string")
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > text.length) error("Incomplete unicode escape")
        val hex = text.substring(index, index + 4)
        index += 4
        return hex.toIntOrNull(16)?.toChar() ?: error("Invalid unicode escape '$hex'")
    }

    private fun parseNumber(): Double {
        val start = index
        while (index < text.length && text[index] in "-+0123456789.eE") {
            index += 1
        }
        return text.substring(start, index).toDoubleOrNull() ?: error("Invalid number")
    }

    private fun parseLiteral(expected: String, value: JsonValue): JsonValue {
        if (!text.startsWith(expected, index)) {
            error("Expected '$expected'")
        }
        index += expected.length
        return value
    }

    private fun expect(char: Char) {
        skipWhitespace()
        if (index >= text.length || text[index] != char) {
            error("Expected '$char'")
        }
        index += 1
    }

    private fun peek(char: Char): Boolean {
        skipWhitespace()
        return index < text.length && text[index] == char
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) {
            index += 1
        }
    }

    private fun error(message: String): Nothing {
        throw LevelParseException("$message at character $index")
    }
}

private fun JsonValue.asObject(path: String): JsonValue.JsonObject {
    return this as? JsonValue.JsonObject ?: throw LevelParseException("$path must be an object")
}

private fun JsonValue.JsonObject.string(name: String): String {
    return (fields[name] as? JsonValue.JsonString)?.value
        ?: throw LevelParseException("$name must be a string")
}

private fun JsonValue.JsonObject.int(name: String): Int {
    val number = (fields[name] as? JsonValue.JsonNumber)?.value
        ?: throw LevelParseException("$name must be a number")
    return number.toInt().also {
        if (it.toDouble() != number) {
            throw LevelParseException("$name must be an integer")
        }
    }
}

private fun JsonValue.JsonObject.optionalInt(name: String): Int? {
    return when (val value = fields[name]) {
        null, JsonValue.JsonNull -> null
        is JsonValue.JsonNumber -> {
            val intValue = value.value.toInt()
            if (intValue.toDouble() != value.value) {
                throw LevelParseException("$name must be an integer")
            }
            intValue
        }

        else -> throw LevelParseException("$name must be a number or null")
    }
}

private fun JsonValue.JsonObject.float(name: String): Float {
    val number = (fields[name] as? JsonValue.JsonNumber)?.value
        ?: throw LevelParseException("$name must be a number")
    return number.toFloat()
}

private fun JsonValue.JsonObject.optionalFloat(name: String): Float? {
    return when (val value = fields[name]) {
        null, JsonValue.JsonNull -> null
        is JsonValue.JsonNumber -> value.value.toFloat()
        else -> throw LevelParseException("$name must be a number or null")
    }
}

private fun JsonValue.JsonObject.array(name: String): List<JsonValue> {
    return (fields[name] as? JsonValue.JsonArray)?.items
        ?: throw LevelParseException("$name must be an array")
}
