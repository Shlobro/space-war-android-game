package com.example.cw.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal const val ENEMY_AI_THINK_INTERVAL_SECONDS = 5f
internal const val BASE_FUNDS_PER_SECOND = 0.6f
internal const val PER_OWNED_BASE_FUNDS_PER_SECOND = 0.25f
internal const val INVALID_TAP_HINT_DURATION_SECONDS = 1.5f
internal const val AI_PRESSURE_ATTACK_MIN_ODDS = 1.2f
private const val TAP_SLOP_PX = 56f

internal fun onScreenTap(
    state: MatchState,
    screenTap: Offset,
    viewportSize: IntSize,
    isDoubleTap: Boolean
): MatchState {
    val canvasSize = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat())
    val tappedBase = state.bases.firstOrNull {
        val screenCenter = worldToScreen(it.position, canvasSize, state.worldBounds)
        val screenRadius = it.radius * scale(canvasSize, state.worldBounds)
        distance(screenCenter, screenTap) <= screenRadius + TAP_SLOP_PX
    } ?: return if (state.selectedBaseIds.isEmpty()) {
        state
    } else {
        state.copy(selectedBaseIds = emptySet())
    }

    if (tappedBase.owner == Owner.PLAYER) {
        if (isDoubleTap && state.selectedBaseIds.isNotEmpty()) {
            val selectedSources = state.bases.filter { it.id in state.selectedBaseIds && it.owner == Owner.PLAYER }
            if (selectedSources.isEmpty()) {
                return state.copy(selectedBaseIds = emptySet())
            }

            return launchFromSelectedBases(state, selectedSources, tappedBase.id)
        }

        val nextSelected = if (tappedBase.id in state.selectedBaseIds) {
            state.selectedBaseIds - tappedBase.id
        } else {
            state.selectedBaseIds + tappedBase.id
        }
        return state.copy(
            selectedBaseIds = nextSelected,
            message = state.message,
            messageExpiresAtSeconds = state.messageExpiresAtSeconds
        )
    }

    if (state.selectedBaseIds.isEmpty()) {
        return state.copy(
            message = "Tap one of your bases first",
            messageExpiresAtSeconds = state.elapsedSeconds + INVALID_TAP_HINT_DURATION_SECONDS
        )
    }

    val selectedSources = state.bases.filter { it.id in state.selectedBaseIds && it.owner == Owner.PLAYER }
    if (selectedSources.isEmpty()) {
        return state.copy(selectedBaseIds = emptySet())
    }

    return launchFromSelectedBases(state, selectedSources, tappedBase.id)
}

internal fun sendFleet(
    state: MatchState,
    sourceId: Int,
    targetId: Int,
    sender: Owner,
    showMessage: Boolean = true
): MatchState {
    val source = state.bases.firstOrNull { it.id == sourceId } ?: return state
    val target = state.bases.firstOrNull { it.id == targetId } ?: return state
    if (source.owner != sender || source.id == target.id) return state

    val departingUnits = departingUnitsForLaunch(source.units)
    if (departingUnits < 1) {
        return if (showMessage && sender == Owner.PLAYER) {
            state.copy(message = "Not enough ships to send", messageExpiresAtSeconds = null)
        } else {
            state
        }
    }

    val updatedBases = state.bases.map {
        if (it.id == source.id) it.copy(units = max(0f, it.units - departingUnits)) else it
    }
    val route = buildRoute(source.position, target.position, state.obstacles, state.worldBounds)

    val fleet = FleetState(
        id = state.nextFleetId,
        owner = sender,
        sourceId = source.id,
        targetId = target.id,
        position = source.position,
        path = route,
        pathIndex = 0,
        units = departingUnits.toFloat(),
        speed = if (source.type == BaseType.FAST) 260f else 120f,
        arrivalMultiplier = 1f,
        fleetDamageMultiplier = 1f,
        type = source.type
    )

    return state.copy(
        bases = updatedBases,
        fleets = state.fleets + fleet,
        nextFleetId = state.nextFleetId + 1,
        message = if (showMessage && sender == Owner.PLAYER) "Launched $departingUnits ships" else state.message,
        messageExpiresAtSeconds = if (showMessage && sender == Owner.PLAYER) null else state.messageExpiresAtSeconds
    )
}

private fun launchFromSelectedBases(
    state: MatchState,
    selectedSources: List<BaseState>,
    targetId: Int
): MatchState {
    var updatedState = state
    var totalLaunchedShips = 0

    selectedSources.forEach { source ->
        val currentSource = updatedState.bases.firstOrNull { it.id == source.id && it.owner == Owner.PLAYER } ?: return@forEach
        val departingUnits = departingUnitsForLaunch(currentSource.units)
        val beforeFleetCount = updatedState.fleets.size
        updatedState = sendFleet(updatedState, source.id, targetId, Owner.PLAYER, showMessage = false)
        if (updatedState.fleets.size > beforeFleetCount) {
            totalLaunchedShips += departingUnits
        }
    }

    return if (totalLaunchedShips > 0) {
        updatedState.copy(
            selectedBaseIds = emptySet(),
            message = "Launched $totalLaunchedShips ships",
            messageExpiresAtSeconds = null
        )
    } else {
        updatedState.copy(
            selectedBaseIds = emptySet(),
            message = "Not enough ships to send",
            messageExpiresAtSeconds = null
        )
    }
}

internal fun upgradeBase(state: MatchState, baseId: Int): MatchState {
    return upgradeBaseForOwner(state, baseId, Owner.PLAYER, showMessage = true)
}

private fun upgradeBaseForOwner(
    state: MatchState,
    baseId: Int,
    owner: Owner,
    showMessage: Boolean
): MatchState {
    val base = state.bases.firstOrNull { it.id == baseId && it.owner == owner } ?: return state
    if (base.capLevel >= base.maxLevel) {
        return if (showMessage && owner == Owner.PLAYER) {
            state.copy(message = "Base is at max level", messageExpiresAtSeconds = null)
        } else {
            state
        }
    }
    val cost = upgradeCost(base)
    val availableMoney = when (owner) {
        Owner.PLAYER -> state.playerMoney
        else -> state.aiStates[owner]?.money ?: 0f
    }

    if (availableMoney < cost) {
        return if (showMessage && owner == Owner.PLAYER) {
            state.copy(message = "Need ${formatFunds(cost)} funds", messageExpiresAtSeconds = null)
        } else {
            state
        }
    }

    val updatedBases = state.bases.map {
        if (it.id == baseId) {
            it.copy(capLevel = it.capLevel + 1)
        } else {
            it
        }
    }

    return when (owner) {
        Owner.PLAYER -> state.copy(
            playerMoney = state.playerMoney - cost,
            bases = updatedBases,
            message = if (showMessage) "Base upgraded" else state.message,
            messageExpiresAtSeconds = if (showMessage) null else state.messageExpiresAtSeconds
        )

        else -> {
            val aiState = state.aiStates[owner] ?: return state
            state.copy(
                aiStates = state.aiStates + (owner to aiState.copy(money = aiState.money - cost)),
                bases = updatedBases
            )
        }
    }
}

internal fun stepMatch(state: MatchState, dt: Float, cashIncomeMultiplier: Float): MatchState {
    if (state.status != MatchStatus.RUNNING || state.isPaused) return state

    var bases = produceShips(state.bases, dt)
    val elapsedSeconds = state.elapsedSeconds + dt
    val playerMoney = state.playerMoney + incomePerSecond(Owner.PLAYER, bases, cashIncomeMultiplier) * dt
    val aiStatesWithIncome = state.aiStates.mapValues { (owner, aiState) ->
        aiState.copy(money = aiState.money + incomePerSecond(owner, bases, cashIncomeMultiplier) * dt)
    }
    var fleets = moveFleets(state.fleets, dt)
    fleets = resolveFleetSkirmishes(fleets, dt)

    val arrival = applyFleetArrivals(bases, fleets)
    bases = arrival.first
    fleets = arrival.second

    val aiState = runEnemyAi(
        state.copy(
            bases = bases,
            fleets = fleets,
            playerMoney = playerMoney,
            aiStates = aiStatesWithIncome
        ),
        dt
    )
    val normalizedSelection = aiState.selectedBaseIds.intersect(
        aiState.bases
            .asSequence()
            .filter { it.owner == Owner.PLAYER }
            .map { it.id }
            .toSet()
    )

    val playerStillInMatch = ownerHasPresence(aiState, Owner.PLAYER)
    val aiStillInMatch = aiState.aiStates.keys.any { ownerHasPresence(aiState, it) }
    val status = when {
        !aiStillInMatch -> MatchStatus.PLAYER_WON
        !playerStillInMatch -> MatchStatus.PLAYER_LOST
        else -> MatchStatus.RUNNING
    }
    val earnedStars = if (status == MatchStatus.PLAYER_WON) {
        starsEarnedForCompletion(elapsedSeconds, state.starThresholds)
    } else {
        0
    }
    val messageExpired = aiState.messageExpiresAtSeconds?.let { elapsedSeconds >= it } == true
    val activeMessage = when (status) {
        MatchStatus.RUNNING -> if (messageExpired) "" else aiState.message
        MatchStatus.PLAYER_WON -> "All AI structures captured"
        MatchStatus.PLAYER_LOST -> "Your network collapsed"
    }
    val activeMessageExpiry = if (status == MatchStatus.RUNNING && !messageExpired) {
        aiState.messageExpiresAtSeconds
    } else {
        null
    }

    return aiState.copy(
        selectedBaseIds = normalizedSelection,
        status = status,
        elapsedSeconds = elapsedSeconds,
        earnedStars = earnedStars,
        message = activeMessage,
        messageExpiresAtSeconds = activeMessageExpiry
    )
}

internal fun ownerHasPresence(state: MatchState, owner: Owner): Boolean {
    return state.bases.any { it.owner == owner } || state.fleets.any { it.owner == owner }
}

internal fun starsEarnedForCompletion(elapsedSeconds: Float, starThresholds: com.example.cw.game.levels.StarThresholds): Int {
    return when {
        elapsedSeconds <= starThresholds.threeStarTimeSeconds -> 3
        elapsedSeconds <= starThresholds.twoStarTimeSeconds -> 2
        else -> 1
    }
}

private fun runEnemyAi(state: MatchState, dt: Float): MatchState {
    var updatedState = state
    updatedState.aiStates.keys.forEach { owner ->
        val currentAiState = updatedState.aiStates[owner] ?: return@forEach
        var nextThink = currentAiState.thinkCountdown - dt
        if (nextThink > 0f) {
            updatedState = updatedState.copy(
                aiStates = updatedState.aiStates + (owner to currentAiState.copy(thinkCountdown = nextThink))
            )
            return@forEach
        }

        nextThink = ENEMY_AI_THINK_INTERVAL_SECONDS
        updatedState = updatedState.copy(
            aiStates = updatedState.aiStates + (owner to currentAiState.copy(thinkCountdown = nextThink))
        )

        updatedState = when (currentAiState.type) {
            AiType.STANDARD -> runStandardAiTurn(updatedState, owner)
        }
    }

    return updatedState
}

private fun runStandardAiTurn(state: MatchState, owner: Owner): MatchState {
    var updatedState = state
    val ownedBaseIds = updatedState.bases.filter { it.owner == owner }.map { it.id }
    for (baseId in ownedBaseIds) {
        val source = updatedState.bases.firstOrNull { it.id == baseId && it.owner == owner } ?: continue
        var acted = false
        val nearbyTargets = updatedState.bases
            .asSequence()
            .filter { it.owner != owner && it.id != source.id }
            .sortedBy { distance(source.position, it.position) }
            .take(3)
            .toList()

        for (target in nearbyTargets) {
            if (source.units >= target.units * 2.5f) {
                updatedState = sendFleet(updatedState, source.id, target.id, owner)
                acted = true
                break
            }
        }

        if (!acted) {
            if (source.capLevel < source.maxLevel) {
                updatedState = upgradeBaseForOwner(updatedState, source.id, owner, showMessage = false)
            } else if (source.units >= source.cap.toFloat()) {
                val pressureTarget = preferredPressureTarget(source, nearbyTargets)
                if (pressureTarget != null && source.units >= pressureTarget.units * AI_PRESSURE_ATTACK_MIN_ODDS) {
                    updatedState = sendFleet(updatedState, source.id, pressureTarget.id, owner)
                }
            }
        }
    }
    return updatedState
}

private fun preferredPressureTarget(source: BaseState, nearbyTargets: List<BaseState>): BaseState? {
    return nearbyTargets
        .asSequence()
        .minWithOrNull(compareBy<BaseState>({ it.units }, { distance(source.position, it.position) }))
}

private fun produceShips(bases: List<BaseState>, dt: Float): List<BaseState> {
    return bases.map { base ->
        val rateMultiplier = if (base.owner == Owner.NEUTRAL) 0.5f else 1f
        val rate = base.productionRate * rateMultiplier * dt
        val units = if (base.units > base.cap.toFloat()) {
            max(base.cap.toFloat(), base.units - rate)
        } else {
            min(base.cap.toFloat(), base.units + rate)
        }
        base.copy(units = units)
    }
}

private fun moveFleets(fleets: List<FleetState>, dt: Float): List<FleetState> {
    return fleets.map { fleet ->
        var position = fleet.position
        var index = fleet.pathIndex
        var distanceLeft = fleet.speed * dt
        while (distanceLeft > 0f && index < fleet.path.size) {
            val waypoint = fleet.path[index]
            val toWaypoint = waypoint - position
            val stepDistance = toWaypoint.getDistance()
            if (stepDistance <= distanceLeft) {
                position = waypoint
                distanceLeft -= stepDistance
                index += 1
            } else {
                val direction = toWaypoint / stepDistance
                position += direction * distanceLeft
                distanceLeft = 0f
            }
        }
        fleet.copy(position = position, pathIndex = index)
    }
}

private fun resolveFleetSkirmishes(fleets: List<FleetState>, dt: Float): List<FleetState> {
    val damages = MutableList(fleets.size) { 0f }
    for (i in fleets.indices) {
        for (j in i + 1 until fleets.size) {
            val first = fleets[i]
            val second = fleets[j]
            if (first.owner == second.owner) continue
            if (distance(first.position, second.position) > 70f) continue

            damages[i] += second.units * second.fleetDamageMultiplier * 0.14f * dt
            damages[j] += first.units * first.fleetDamageMultiplier * 0.14f * dt
        }
    }

    return fleets.mapIndexedNotNull { index, fleet ->
        val units = fleet.units - damages[index]
        if (units > 0.35f) fleet.copy(units = units) else null
    }
}

private fun applyFleetArrivals(
    bases: List<BaseState>,
    fleets: List<FleetState>
): Pair<List<BaseState>, List<FleetState>> {
    var currentBases = bases
    val survivors = mutableListOf<FleetState>()
    fleets.forEach { fleet ->
        if (fleet.pathIndex < fleet.path.size) {
            survivors += fleet
            return@forEach
        }

        val target = currentBases.firstOrNull { it.id == fleet.targetId } ?: return@forEach
        val updatedTarget = resolveArrival(target, fleet)
        currentBases = currentBases.map { if (it.id == target.id) updatedTarget else it }
    }
    return currentBases to survivors
}

private fun resolveArrival(target: BaseState, fleet: FleetState): BaseState {
    return if (target.owner == fleet.owner) {
        target.copy(units = target.units + fleet.units * fleet.arrivalMultiplier)
    } else {
        val attackPower = fleet.units * fleet.arrivalMultiplier
        if (attackPower > target.units) {
            val capturedCapLevel = capturedCapLevel(target.capLevel)
            target.copy(
                owner = fleet.owner,
                units = attackPower - target.units,
                capLevel = capturedCapLevel
            )
        } else {
            target.copy(units = target.units - attackPower)
        }
    }
}

private fun capturedCapLevel(capLevel: Int): Int = max(1, capLevel - 2)

private fun departingUnitsForLaunch(units: Float): Int = floor(units * 0.5f).toInt()

private fun incomePerSecond(owner: Owner, bases: List<BaseState>, multiplier: Float): Float {
    val owned = bases.count { it.owner == owner }
    return if (owner.isNeutral) 0f else (BASE_FUNDS_PER_SECOND + owned * PER_OWNED_BASE_FUNDS_PER_SECOND) * multiplier
}
