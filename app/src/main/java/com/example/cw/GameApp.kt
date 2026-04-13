package com.example.cw

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val WORLD_WIDTH = 1000f
private const val WORLD_HEIGHT = 1600f

@Composable
fun GameApp() {
    var appScreen by remember { mutableStateOf(AppScreen.HOME) }
    var campaign by remember { mutableStateOf(CampaignState()) }
    var matchState by remember { mutableStateOf<MatchState?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(appScreen, matchState?.status, matchState?.isPaused, campaign.cashRateLevel) {
        var previousTime = 0L
        while (appScreen == AppScreen.IN_GAME && matchState?.status == MatchStatus.RUNNING) {
            withFrameNanos { frameTime ->
                val currentMatch = matchState ?: return@withFrameNanos
                if (currentMatch.isPaused) {
                    previousTime = frameTime
                    return@withFrameNanos
                }
                if (previousTime == 0L) {
                    previousTime = frameTime
                } else {
                    val dt = ((frameTime - previousTime) / 1_000_000_000f).coerceIn(0.0f, 0.033f)
                    previousTime = frameTime
                    val stepped = stepMatch(currentMatch, dt, campaign.cashIncomeMultiplier())
                    val earnedUpgradePoint = currentMatch.status == MatchStatus.RUNNING &&
                        stepped.status == MatchStatus.PLAYER_WON &&
                        stepped.levelId !in campaign.completedLevels
                    matchState = stepped.copy(earnedUpgradePoint = earnedUpgradePoint)
                    if (earnedUpgradePoint) {
                        campaign = campaign.completeLevel(stepped.levelId)
                    }
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF06111C)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF02050A), Color(0xFF081C29), Color(0xFF0A2331))
                    )
                )
        ) {
            when (appScreen) {
                AppScreen.HOME -> HomeScreen(
                    onLevels = { appScreen = AppScreen.LEVELS },
                    onUpgrades = { appScreen = AppScreen.UPGRADES }
                )

                AppScreen.LEVELS -> LevelSelectScreen(
                    campaign = campaign,
                    onBack = { appScreen = AppScreen.HOME },
                    onPlayLevel = { levelId ->
                        matchState = createMatch(levelId)
                        appScreen = AppScreen.IN_GAME
                    }
                )

                AppScreen.UPGRADES -> UpgradesScreen(
                    campaign = campaign,
                    onBack = { appScreen = AppScreen.HOME },
                    onUpgradeCashRate = {
                        if (campaign.upgradePoints > 0) {
                            campaign = campaign.copy(
                                upgradePoints = campaign.upgradePoints - 1,
                                cashRateLevel = campaign.cashRateLevel + 1
                            )
                        }
                    }
                )

                AppScreen.IN_GAME -> {
                    val activeMatch = matchState
                    if (activeMatch != null) {
                        val latestMatch by rememberUpdatedState(matchState)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { viewportSize = it }
                                .pointerInput(viewportSize) {
                                    detectTapGestures(
                                        onTap = { tap ->
                                            val currentMatch = latestMatch ?: return@detectTapGestures
                                            if (
                                                currentMatch.status != MatchStatus.RUNNING ||
                                                currentMatch.isPaused ||
                                                viewportSize == IntSize.Zero
                                            ) {
                                                return@detectTapGestures
                                            }
                                            matchState = onScreenTap(currentMatch, tap, viewportSize, false)
                                        },
                                        onDoubleTap = { tap ->
                                            val currentMatch = latestMatch ?: return@detectTapGestures
                                            if (
                                                currentMatch.status != MatchStatus.RUNNING ||
                                                currentMatch.isPaused ||
                                                viewportSize == IntSize.Zero
                                            ) {
                                                return@detectTapGestures
                                            }
                                            matchState = onScreenTap(currentMatch, tap, viewportSize, true)
                                        }
                                    )
                                }
                        ) {
                            GameCanvas(
                                state = activeMatch,
                                modifier = Modifier.fillMaxSize()
                            )
                            UpgradeNodeButton(
                                state = activeMatch,
                                viewportSize = viewportSize,
                                onUpgrade = { baseId ->
                                    matchState = upgradeBase(activeMatch, baseId)
                                }
                            )
                        }

                        InGameHud(
                            state = activeMatch,
                            onOpenMenu = {
                                matchState = activeMatch.copy(isPaused = true)
                            }
                        )

                        if (activeMatch.isPaused && activeMatch.status == MatchStatus.RUNNING) {
                            PauseOverlay(
                                onResume = { matchState = activeMatch.copy(isPaused = false) },
                                onQuit = {
                                    matchState = null
                                    appScreen = AppScreen.LEVELS
                                }
                            )
                        }

                        if (activeMatch.status == MatchStatus.PLAYER_WON || activeMatch.status == MatchStatus.PLAYER_LOST) {
                            LevelEndOverlay(
                                state = activeMatch,
                                onLevels = {
                                    matchState = null
                                    appScreen = AppScreen.LEVELS
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onLevels: () -> Unit,
    onUpgrades: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Cell Wars Prototype",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
                Text("Prototype campaign shell", color = Color(0xFFA7C0D8))
                Button(onClick = onLevels, modifier = Modifier.fillMaxWidth()) { Text("Levels") }
                Button(onClick = onUpgrades, modifier = Modifier.fillMaxWidth()) { Text("Upgrades") }
            }
        }
    }
}

@Composable
private fun LevelSelectScreen(
    campaign: CampaignState,
    onBack: () -> Unit,
    onPlayLevel: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Levels", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Back",
                color = Color(0xFFA7C0D8),
                modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onBack() }) }
            )
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Level 1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Regular ships only. No special abilities. Capture the map and defeat the enemy network.",
                    color = Color(0xFFA7C0D8)
                )
                Text(
                    if (1 in campaign.completedLevels) "Completed" else "Not completed",
                    color = if (1 in campaign.completedLevels) Color(0xFF9BE7AE) else Color(0xFFF6CB7D)
                )
                Button(onClick = { onPlayLevel(1) }) { Text("Play") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Level 2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Introduces fast ships. Some captured map bases launch faster-moving fleets.",
                    color = Color(0xFFA7C0D8)
                )
                Text(
                    if (1 in campaign.completedLevels) {
                        if (2 in campaign.completedLevels) "Completed" else "Unlocked"
                    } else {
                        "Complete Level 1 to unlock"
                    },
                    color = if (2 in campaign.completedLevels) Color(0xFF9BE7AE) else Color(0xFFF6CB7D)
                )
                Button(
                    onClick = { onPlayLevel(2) },
                    enabled = 1 in campaign.completedLevels
                ) {
                    Text("Play")
                }
            }
        }
    }
}

@Composable
private fun UpgradesScreen(
    campaign: CampaignState,
    onBack: () -> Unit,
    onUpgradeCashRate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Upgrades", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Back",
                color = Color(0xFFA7C0D8),
                modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onBack() }) }
            )
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Upgrade Points: ${campaign.upgradePoints}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Cash Flow Level: ${campaign.cashRateLevel}", color = Color(0xFFA7C0D8))
                Text(
                    "Increases how fast funds rise during a level. More upgrades will be added later.",
                    color = Color(0xFFA7C0D8)
                )
                Button(
                    onClick = onUpgradeCashRate,
                    enabled = campaign.upgradePoints > 0
                ) {
                    Text("Upgrade Cash Flow")
                }
            }
        }
    }
}

@Composable
private fun InGameHud(
    state: MatchState,
    onOpenMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Your Funds ${state.playerMoney.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("AI Funds ${state.enemyMoney.toInt()}", color = Color(0xFFFFC2B9), fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Menu",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.pointerInput(state.status, state.isPaused) {
                        detectTapGestures(onTap = { onOpenMenu() })
                    }
                )
            }
        }

        if (state.message.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xC0182735)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = state.message,
                    color = Color(0xFFEAF4FF),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88101822)),
        contentAlignment = Alignment.Center
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xF0182735)), shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Paused", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("Resume") }
                Button(onClick = onQuit, modifier = Modifier.fillMaxWidth()) { Text("Quit Level") }
            }
        }
    }
}

@Composable
private fun LevelEndOverlay(
    state: MatchState,
    onLevels: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88101822)),
        contentAlignment = Alignment.Center
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xF0182735)), shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (state.status == MatchStatus.PLAYER_WON) "Level Complete" else "Level Failed",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.status == MatchStatus.PLAYER_WON) {
                    Text(
                        if (state.earnedUpgradePoint) "You earned 1 upgrade point." else "Level already completed.",
                        color = Color(0xFFA7C0D8)
                    )
                }
                Button(onClick = onLevels, modifier = Modifier.fillMaxWidth()) { Text("Back To Levels") }
            }
        }
    }
}

@Composable
private fun GameCanvas(state: MatchState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val labelPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 14.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
    }
    val fleetPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
    }
    Canvas(modifier = modifier) {
        drawStarfield()
        state.obstacles.forEach { obstacle -> drawObstacle(obstacle) }
        state.bases.forEach { base -> drawBaseAura(base, base.id in state.selectedBaseIds) }
        drawFleetTrails(state)
        state.bases.forEach { base -> drawBase(base, labelPaint, base.id in state.selectedBaseIds) }
        state.fleets.forEach { fleet -> drawFleet(fleet, fleetPaint) }
    }
}

@Composable
private fun UpgradeNodeButton(
    state: MatchState,
    viewportSize: IntSize,
    onUpgrade: (Int) -> Unit
) {
    if (viewportSize == IntSize.Zero) return
    if (state.selectedBaseIds.size != 1) return

    val base = state.bases.firstOrNull { it.id == state.selectedBaseIds.first() && it.owner == Owner.PLAYER } ?: return
    val canvasSize = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat())
    val center = worldToScreen(base.position, canvasSize)
    val radius = base.radius * scale(canvasSize)
    val density = LocalDensity.current
    val cost = upgradeCost(base)
    val canAfford = state.playerMoney >= cost
    val x = center.x + radius + with(density) { 6.dp.toPx() }
    val y = center.y - radius - with(density) { 4.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .offset { IntOffset(x.toInt(), y.toInt()) }
                .zIndex(2f)
                .widthIn(min = 64.dp)
                .heightIn(min = 40.dp)
                .background(
                    color = if (canAfford) Color(0xFFF6CB7D) else Color(0xFF6C5A2B),
                    shape = RoundedCornerShape(50)
                )
                .clickable { onUpgrade(base.id) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                color = Color(0xFF102132),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = formatFunds(cost),
                color = Color(0xFF102132),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

private fun DrawScope.drawStarfield() {
    val stars = listOf(
        Offset(80f, 120f), Offset(240f, 210f), Offset(480f, 140f), Offset(720f, 260f),
        Offset(910f, 180f), Offset(160f, 420f), Offset(600f, 380f), Offset(870f, 520f),
        Offset(200f, 740f), Offset(760f, 780f), Offset(120f, 1120f), Offset(510f, 1280f),
        Offset(880f, 1460f)
    )
    stars.forEachIndexed { index, offset ->
        drawCircle(
            color = if (index % 3 == 0) Color(0x99FFFFFF) else Color(0x66BEE8FF),
            radius = if (index % 4 == 0) 3.6f else 2.1f,
            center = worldToScreen(offset, size)
        )
    }
}

private fun DrawScope.drawObstacle(obstacle: Obstacle) {
    val center = worldToScreen(obstacle.position, size)
    val radius = obstacle.radius * scale(size)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF283543), Color(0xFF111A23), Color(0xAA0B1117)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0x5587A3BE),
        radius = radius + 8f,
        center = center,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawBaseAura(base: BaseState, selected: Boolean) {
    val center = worldToScreen(base.position, size)
    val baseRadius = base.radius * scale(size)
    if (selected) {
        drawCircle(
            color = Color(0x22FFF3BF),
            radius = baseRadius + 28f,
            center = center
        )
        drawCircle(
            color = Color(0xFFF6CB7D),
            radius = baseRadius + 12f,
            center = center,
            style = Stroke(width = 5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = baseRadius + 20f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawBase(
    base: BaseState,
    labelPaint: Paint,
    selected: Boolean
) {
    val center = worldToScreen(base.position, size)
    val baseRadius = base.radius * scale(size)
    val fillColor = base.owner.color
    if (base.type == BaseType.FAST) {
        val fillPath = diamondPath(center, baseRadius)
        drawPath(
            path = fillPath,
            brush = Brush.radialGradient(
                listOf(
                    if (selected) fillColor.copy(alpha = 1f) else fillColor.copy(alpha = 0.95f),
                    fillColor.copy(alpha = if (selected) 0.68f else 0.42f),
                    Color(0xAA0D1621)
                ),
                center = center,
                radius = baseRadius
            )
        )
        drawPath(
            path = diamondPath(center, baseRadius),
            color = if (selected) Color(0xFFF6CB7D) else base.owner.color.copy(alpha = 0.95f),
            style = Stroke(width = 4f)
        )
    } else {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (selected) fillColor.copy(alpha = 1f) else fillColor.copy(alpha = 0.95f),
                    fillColor.copy(alpha = if (selected) 0.68f else 0.42f),
                    Color(0xAA0D1621)
                ),
                center = center,
                radius = baseRadius
            ),
            radius = baseRadius,
            center = center
        )
        drawCircle(
            color = if (selected) Color(0xFFF6CB7D) else base.owner.color.copy(alpha = 0.95f),
            radius = baseRadius,
            center = center,
            style = Stroke(width = 4f)
        )
    }
    drawContext.canvas.nativeCanvas.drawText(
        base.units.toInt().toString(),
        center.x,
        center.y + 6f,
        labelPaint
    )
    if (selected) {
        drawContext.canvas.nativeCanvas.drawText(
            "SELECTED",
            center.x,
            center.y - baseRadius - 16f,
            labelPaint.apply {
                textSize *= 0.72f
            }
        )
        labelPaint.textSize /= 0.72f
    }
}

private fun DrawScope.drawFleetTrails(state: MatchState) {
    state.fleets.forEach { fleet ->
        val color = fleet.owner.color.copy(alpha = 0.3f)
        val canvasSize = size
        val points = buildList {
            add(worldToScreen(fleet.position, canvasSize))
            fleet.path.drop(fleet.pathIndex).forEach { add(worldToScreen(it, canvasSize)) }
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path = path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawFleet(fleet: FleetState, fleetPaint: Paint) {
    val center = worldToScreen(fleet.position, size)
    val radius = (6f + fleet.units * 0.16f).coerceIn(7f, 18f)
    if (fleet.type == BaseType.FAST) {
        drawPath(
            path = arrowPath(center, radius),
            color = fleet.owner.color.copy(alpha = 0.92f)
        )
    } else {
        drawCircle(
            color = fleet.owner.color.copy(alpha = 0.9f),
            radius = radius,
            center = center
        )
    }
    drawContext.canvas.nativeCanvas.drawText(
        fleet.units.toInt().toString(),
        center.x,
        center.y + 4f,
        fleetPaint
    )
}

private fun onScreenTap(
    state: MatchState,
    screenTap: Offset,
    viewportSize: IntSize,
    isDoubleTap: Boolean
): MatchState {
    val canvasSize = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat())
    val tappedBase = state.bases.firstOrNull {
        val screenCenter = worldToScreen(it.position, canvasSize)
        val screenRadius = it.radius * scale(canvasSize)
        distance(screenCenter, screenTap) <= screenRadius + 56f
    } ?: return state.copy(message = "No base hit")

    if (tappedBase.owner == Owner.PLAYER) {
        if (isDoubleTap && state.selectedBaseIds.isNotEmpty()) {
            val selectedSources = state.bases.filter { it.id in state.selectedBaseIds && it.owner == Owner.PLAYER }
            if (selectedSources.isEmpty()) {
                return state.copy(selectedBaseIds = emptySet(), message = "Selection cleared")
            }

            var updatedState = state
            var launchedCount = 0
            selectedSources.forEach { source ->
                val beforeFleetCount = updatedState.fleets.size
                updatedState = sendFleet(updatedState, source.id, tappedBase.id, Owner.PLAYER)
                if (updatedState.fleets.size > beforeFleetCount) {
                    launchedCount += 1
                }
            }
            return updatedState.copy(
                selectedBaseIds = emptySet(),
                message = if (launchedCount > 0) {
                    "Reinforced from $launchedCount bases"
                } else {
                    "Not enough ships to send"
                }
            )
        }

        val nextSelected = if (tappedBase.id in state.selectedBaseIds) {
            state.selectedBaseIds - tappedBase.id
        } else {
            state.selectedBaseIds + tappedBase.id
        }
        return state.copy(
            selectedBaseIds = nextSelected,
            message = when (nextSelected.size) {
                0 -> "Selection cleared"
                1 -> "1 base selected"
                else -> "${nextSelected.size} bases selected"
            }
        )
    }

    if (state.selectedBaseIds.isEmpty()) {
        return state.copy(message = "Select your bases first")
    }

    val selectedSources = state.bases.filter { it.id in state.selectedBaseIds && it.owner == Owner.PLAYER }
    if (selectedSources.isEmpty()) {
        return state.copy(selectedBaseIds = emptySet(), message = "Selection cleared")
    }

    var updatedState = state
    var launchedCount = 0
    selectedSources.forEach { source ->
        val beforeFleetCount = updatedState.fleets.size
        updatedState = sendFleet(updatedState, source.id, tappedBase.id, Owner.PLAYER)
        if (updatedState.fleets.size > beforeFleetCount) {
            launchedCount += 1
        }
    }
    return updatedState.copy(
        selectedBaseIds = emptySet(),
        message = if (launchedCount > 0) {
            "Launched from $launchedCount bases"
        } else {
            "Not enough ships to send"
        }
    )
}

private fun sendFleet(state: MatchState, sourceId: Int, targetId: Int, sender: Owner): MatchState {
    val source = state.bases.firstOrNull { it.id == sourceId } ?: return state
    val target = state.bases.firstOrNull { it.id == targetId } ?: return state
    if (source.owner != sender || source.id == target.id) return state

    val departingUnits = floor(source.units * 0.5f)
    if (departingUnits < 1f) {
        return state.copy(message = "Not enough ships to send")
    }

    val updatedBases = state.bases.map {
        if (it.id == source.id) it.copy(units = max(0f, it.units - departingUnits)) else it
    }
    val route = buildRoute(source.position, target.position, state.obstacles)

    val fleet = FleetState(
        id = state.nextFleetId,
        owner = sender,
        sourceId = source.id,
        targetId = target.id,
        position = source.position,
        path = route,
        pathIndex = 0,
        units = departingUnits,
        speed = if (source.type == BaseType.FAST) 260f else 120f,
        arrivalMultiplier = 1f,
        fleetDamageMultiplier = 1f,
        type = source.type
    )

    return state.copy(
        bases = updatedBases,
        fleets = state.fleets + fleet,
        nextFleetId = state.nextFleetId + 1,
        selectedBaseIds = if (sender == Owner.PLAYER) state.selectedBaseIds else state.selectedBaseIds,
        message = if (sender == Owner.PLAYER) "Launched ${departingUnits.toInt()} ships" else state.message
    )
}

private fun upgradeBase(state: MatchState, baseId: Int): MatchState {
    return upgradeBaseForOwner(state, baseId, Owner.PLAYER, showMessage = true)
}

private fun upgradeBaseForOwner(
    state: MatchState,
    baseId: Int,
    owner: Owner,
    showMessage: Boolean
): MatchState {
    val base = state.bases.firstOrNull { it.id == baseId && it.owner == owner } ?: return state
    val cost = upgradeCost(base)
    val availableMoney = when (owner) {
        Owner.PLAYER -> state.playerMoney
        Owner.ENEMY -> state.enemyMoney
        Owner.NEUTRAL -> 0f
    }

    if (availableMoney < cost) {
        return if (showMessage && owner == Owner.PLAYER) {
            state.copy(message = "Need $cost funds")
        } else {
            state
        }
    }

    val updatedBases = state.bases.map {
        if (it.id == baseId) {
            it.copy(cap = it.cap + 10, capLevel = it.capLevel + 1)
        } else {
            it
        }
    }

    return when (owner) {
        Owner.PLAYER -> state.copy(
            playerMoney = state.playerMoney - cost,
            bases = updatedBases,
            message = if (showMessage) "Base upgraded" else state.message
        )

        Owner.ENEMY -> state.copy(
            enemyMoney = state.enemyMoney - cost,
            bases = updatedBases
        )

        Owner.NEUTRAL -> state
    }
}

private fun stepMatch(state: MatchState, dt: Float, cashIncomeMultiplier: Float): MatchState {
    if (state.status != MatchStatus.RUNNING) return state

    var bases = produceShips(state.bases, dt)
    val playerMoney = state.playerMoney + incomePerSecond(Owner.PLAYER, bases, cashIncomeMultiplier) * dt
    val enemyMoney = state.enemyMoney + incomePerSecond(Owner.ENEMY, bases, cashIncomeMultiplier) * dt
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
            enemyMoney = enemyMoney
        ),
        dt
    )

    val playerStillInMatch = ownerHasPresence(aiState, Owner.PLAYER)
    val enemyStillInMatch = ownerHasPresence(aiState, Owner.ENEMY)
    val status = when {
        !enemyStillInMatch -> MatchStatus.PLAYER_WON
        !playerStillInMatch -> MatchStatus.PLAYER_LOST
        else -> MatchStatus.RUNNING
    }

    return aiState.copy(
        status = status,
        message = when (status) {
            MatchStatus.RUNNING -> aiState.message
            MatchStatus.PLAYER_WON -> "All enemy structures captured"
            MatchStatus.PLAYER_LOST -> "Your network collapsed"
        }
    )
}

private fun ownerHasPresence(state: MatchState, owner: Owner): Boolean {
    return state.bases.any { it.owner == owner } || state.fleets.any { it.owner == owner }
}

private fun runEnemyAi(state: MatchState, dt: Float): MatchState {
    var nextThink = state.enemyThinkCountdown - dt
    var updatedState = state.copy(enemyThinkCountdown = nextThink)

    if (nextThink > 0f) return updatedState
    nextThink = 1.2f

    updatedState = updatedState.copy(enemyThinkCountdown = nextThink)
    val ownedBaseIds = updatedState.bases.filter { it.owner == Owner.ENEMY }.map { it.id }
    for (baseId in ownedBaseIds) {
        val source = updatedState.bases.firstOrNull { it.id == baseId && it.owner == Owner.ENEMY } ?: continue
        var acted = false
        val nearbyTargets = updatedState.bases
            .asSequence()
            .filter { it.owner != Owner.ENEMY && it.id != source.id }
            .sortedBy { distance(source.position, it.position) }
            .take(3)
            .toList()

        for (target in nearbyTargets) {
            if (source.units >= target.units * 2.5f) {
                updatedState = sendFleet(updatedState, source.id, target.id, Owner.ENEMY)
                acted = true
                break
            }
        }

        if (!acted) {
            updatedState = upgradeBaseForOwner(updatedState, source.id, Owner.ENEMY, showMessage = false)
        }
    }

    return updatedState.copy(enemyThinkCountdown = nextThink)
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
            target.copy(
                owner = fleet.owner,
                units = min(target.cap.toFloat(), attackPower - target.units)
            )
        } else {
            target.copy(units = target.units - attackPower)
        }
    }
}

private fun incomePerSecond(owner: Owner, bases: List<BaseState>, multiplier: Float): Float {
    val owned = bases.count { it.owner == owner }
    return if (owner == Owner.NEUTRAL) 0f else (1.5f + owned * 0.35f) * multiplier
}

private fun createMatch(levelId: Int): MatchState {
    val obstacles: List<Obstacle>
    val bases: List<BaseState>

    if (levelId == 2) {
        obstacles = listOf(
            Obstacle(Offset(520f, 480f), 88f),
            Obstacle(Offset(330f, 860f), 64f),
            Obstacle(Offset(705f, 980f), 86f),
            Obstacle(Offset(520f, 1240f), 58f)
        )
        bases = listOf(
            BaseState(1, Offset(180f, 1350f), Owner.PLAYER, BaseType.COMMAND, units = 32f, cap = 48, capLevel = 1),
            BaseState(2, Offset(820f, 250f), Owner.ENEMY, BaseType.COMMAND, units = 32f, cap = 48, capLevel = 1),
            BaseState(3, Offset(300f, 1120f), Owner.NEUTRAL, BaseType.COMMAND, units = 16f, cap = 40, capLevel = 1),
            BaseState(4, Offset(735f, 1180f), Owner.NEUTRAL, BaseType.FAST, units = 19f, cap = 42, capLevel = 1),
            BaseState(5, Offset(530f, 790f), Owner.NEUTRAL, BaseType.COMMAND, units = 22f, cap = 42, capLevel = 1),
            BaseState(6, Offset(260f, 520f), Owner.NEUTRAL, BaseType.FAST, units = 18f, cap = 40, capLevel = 1),
            BaseState(7, Offset(780f, 630f), Owner.NEUTRAL, BaseType.COMMAND, units = 18f, cap = 40, capLevel = 1),
            BaseState(8, Offset(455f, 1320f), Owner.NEUTRAL, BaseType.COMMAND, units = 19f, cap = 40, capLevel = 1)
        )
    } else {
        obstacles = listOf(
            Obstacle(Offset(500f, 520f), 95f),
            Obstacle(Offset(320f, 990f), 70f),
            Obstacle(Offset(710f, 1050f), 78f)
        )
        bases = listOf(
            BaseState(1, Offset(180f, 1350f), Owner.PLAYER, BaseType.COMMAND, units = 32f, cap = 48, capLevel = 1),
            BaseState(2, Offset(820f, 250f), Owner.ENEMY, BaseType.COMMAND, units = 32f, cap = 48, capLevel = 1),
            BaseState(3, Offset(260f, 1100f), Owner.NEUTRAL, BaseType.COMMAND, units = 16f, cap = 40, capLevel = 1),
            BaseState(4, Offset(735f, 1210f), Owner.NEUTRAL, BaseType.COMMAND, units = 20f, cap = 44, capLevel = 1),
            BaseState(5, Offset(545f, 790f), Owner.NEUTRAL, BaseType.COMMAND, units = 22f, cap = 42, capLevel = 1),
            BaseState(6, Offset(250f, 480f), Owner.NEUTRAL, BaseType.COMMAND, units = 18f, cap = 40, capLevel = 1),
            BaseState(7, Offset(790f, 650f), Owner.NEUTRAL, BaseType.COMMAND, units = 18f, cap = 40, capLevel = 1),
            BaseState(8, Offset(465f, 1240f), Owner.NEUTRAL, BaseType.COMMAND, units = 19f, cap = 40, capLevel = 1)
        )
    }

    return MatchState(
        bases = bases,
        fleets = emptyList(),
        obstacles = obstacles,
        playerMoney = 0f,
        enemyMoney = 0f,
        nextFleetId = 1,
        selectedBaseIds = emptySet(),
        message = "Capture nearby structures and push forward",
        status = MatchStatus.RUNNING,
        enemyThinkCountdown = 1.1f,
        levelId = levelId,
        isPaused = false
    )
}

private fun upgradeCost(base: BaseState): Float = 18f + base.capLevel * 10f

private fun formatFunds(amount: Float): String = amount.toInt().toString()

private fun buildRoute(start: Offset, end: Offset, obstacles: List<Obstacle>): List<Offset> {
    var route = listOf(start, end)
    repeat(3) {
        val obstacle = obstacles.firstOrNull { obstacle ->
            route.zipWithNext().any { (a, b) -> segmentHitsCircle(a, b, obstacle.position, obstacle.radius + 34f) }
        } ?: return@repeat

        val newRoute = mutableListOf<Offset>()
        route.zipWithNext().forEach { (a, b) ->
            newRoute += a
            if (segmentHitsCircle(a, b, obstacle.position, obstacle.radius + 34f)) {
                newRoute += computeDetourPoint(a, b, obstacle)
            }
        }
        newRoute += route.last()
        route = newRoute.distinctBy { "${it.x.roundKey()}-${it.y.roundKey()}" }
    }
    return route.drop(1)
}

private fun computeDetourPoint(start: Offset, end: Offset, obstacle: Obstacle): Offset {
    val center = obstacle.position
    val padding = obstacle.radius + 58f
    val toStart = normalize(start - center)
    val toEnd = normalize(end - center)
    val candidateA = center + normalize(toStart + toEnd).rotate90() * padding
    val candidateB = center + normalize(toStart + toEnd).rotateMinus90() * padding

    val routeA = distance(start, candidateA) + distance(candidateA, end)
    val routeB = distance(start, candidateB) + distance(candidateB, end)
    val chosen = if (routeA <= routeB) candidateA else candidateB

    return Offset(
        chosen.x.coerceIn(80f, WORLD_WIDTH - 80f),
        chosen.y.coerceIn(120f, WORLD_HEIGHT - 120f)
    )
}

private fun segmentHitsCircle(a: Offset, b: Offset, center: Offset, radius: Float): Boolean {
    val ab = b - a
    val t = (((center - a).x * ab.x) + ((center - a).y * ab.y)) /
        max(0.0001f, ab.x * ab.x + ab.y * ab.y)
    val clampedT = t.coerceIn(0f, 1f)
    val closest = a + ab * clampedT
    return distance(closest, center) < radius
}

private fun pathLength(points: List<Offset>): Float {
    if (points.size < 2) return 0f
    var total = 0f
    for (i in 0 until points.lastIndex) {
        total += distance(points[i], points[i + 1])
    }
    return total
}

private fun scale(size: Size): Float = min(size.width / WORLD_WIDTH, size.height / WORLD_HEIGHT)

private fun worldToScreen(offset: Offset, canvasSize: Size): Offset {
    val s = scale(canvasSize)
    val origin = Offset(
        (canvasSize.width - WORLD_WIDTH * s) / 2f,
        (canvasSize.height - WORLD_HEIGHT * s) / 2f
    )
    return Offset(origin.x + offset.x * s, origin.y + offset.y * s)
}

private fun distance(a: Offset, b: Offset): Float = (a - b).getDistance()

private fun normalize(offset: Offset): Offset {
    val value = offset.getDistance()
    return if (value <= 0.0001f) Offset(1f, 0f) else offset / value
}

private fun diamondPath(center: Offset, radius: Float): Path {
    return Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
}

private fun arrowPath(center: Offset, radius: Float): Path {
    return Path().apply {
        moveTo(center.x + radius, center.y)
        lineTo(center.x - radius * 0.45f, center.y - radius * 0.72f)
        lineTo(center.x - radius * 0.16f, center.y)
        lineTo(center.x - radius * 0.45f, center.y + radius * 0.72f)
        close()
    }
}

private fun Offset.rotate90(): Offset = Offset(-y, x)

private fun Offset.rotateMinus90(): Offset = Offset(y, -x)

private fun Float.roundKey(): Int = (this * 10).toInt()

private data class MatchState(
    val bases: List<BaseState>,
    val fleets: List<FleetState>,
    val obstacles: List<Obstacle>,
    val playerMoney: Float,
    val enemyMoney: Float,
    val nextFleetId: Int,
    val selectedBaseIds: Set<Int>,
    val message: String,
    val status: MatchStatus,
    val enemyThinkCountdown: Float,
    val levelId: Int,
    val isPaused: Boolean,
    val earnedUpgradePoint: Boolean = false
)

private data class CampaignState(
    val completedLevels: Set<Int> = emptySet(),
    val upgradePoints: Int = 0,
    val cashRateLevel: Int = 0
) {
    fun cashIncomeMultiplier(): Float = 1f + cashRateLevel * 0.25f

    fun completeLevel(levelId: Int): CampaignState {
        return if (levelId in completedLevels) {
            this
        } else {
            copy(
                completedLevels = completedLevels + levelId,
                upgradePoints = upgradePoints + 1
            )
        }
    }
}

private data class BaseState(
    val id: Int,
    val position: Offset,
    val owner: Owner,
    val type: BaseType,
    val units: Float,
    val cap: Int,
    val capLevel: Int,
    val radius: Float = 54f
) {
    val productionRate: Float
        get() = 0.9f
}

private data class FleetState(
    val id: Int,
    val owner: Owner,
    val sourceId: Int,
    val targetId: Int,
    val position: Offset,
    val path: List<Offset>,
    val pathIndex: Int,
    val units: Float,
    val speed: Float,
    val arrivalMultiplier: Float,
    val fleetDamageMultiplier: Float,
    val type: BaseType
)

private data class Obstacle(
    val position: Offset,
    val radius: Float
)

private enum class AppScreen {
    HOME,
    LEVELS,
    UPGRADES,
    IN_GAME
}

private enum class MatchStatus {
    RUNNING,
    PLAYER_WON,
    PLAYER_LOST
}

private enum class Owner(val color: Color) {
    PLAYER(Color(0xFF59D0FF)),
    ENEMY(Color(0xFFFF7868)),
    NEUTRAL(Color(0xFF8999A8));
}

private enum class BaseType(
    val label: String,
    val shortLabel: String,
    val accent: Color,
    val description: String
) {
    COMMAND(
        label = "Command",
        shortLabel = "CMD",
        accent = Color(0xFFE8F3FF),
        description = "Balanced headquarters with no situational modifier."
    ),
    FAST(
        label = "Fast",
        shortLabel = "FST",
        accent = Color(0xFFBFE7FF),
        description = "Launches faster-moving ships."
    ),
    FACTORY(
        label = "Factory",
        shortLabel = "FAC",
        accent = Color(0xFFB8FFCF),
        description = "Produces ships faster and rewards early economic expansion."
    ),
    RELAY(
        label = "Relay",
        shortLabel = "RLY",
        accent = Color(0xFFFEDC8B),
        description = "Fleets sent to friendly bases travel faster and reinforce harder."
    ),
    ASSAULT(
        label = "Assault",
        shortLabel = "ATK",
        accent = Color(0xFFFFB1A1),
        description = "Fleets sent to neutral or enemy bases hit harder and skirmish better."
    ),
    BATTERY(
        label = "Battery",
        shortLabel = "BAT",
        accent = Color(0xFFC0A4FF),
        description = "Damages passing enemy fleets inside a large defensive radius."
    )
}
