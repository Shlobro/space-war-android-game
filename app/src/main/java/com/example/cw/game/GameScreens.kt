package com.example.cw.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.cw.game.levels.LevelSummary
import com.example.cw.game.levels.isLevelUnlocked

@Composable
internal fun HomeScreen(
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
internal fun LevelSelectScreen(
    campaign: CampaignState,
    levels: List<LevelSummary>,
    loadError: String?,
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

        if (loadError != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xAA462326)), shape = RoundedCornerShape(24.dp)) {
                Text(
                    text = loadError,
                    color = Color(0xFFFFDDD7),
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        if (levels.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)), shape = RoundedCornerShape(24.dp)) {
                Text(
                    text = "No packaged levels were found in assets/levels.",
                    color = Color(0xFFA7C0D8),
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        levels.forEach { level ->
            val unlocked = isLevelUnlocked(level, campaign)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(level.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(level.description, color = Color(0xFFA7C0D8))
                    Text(
                        text = when {
                            level.levelId in campaign.completedLevels -> "Completed"
                            unlocked -> "Unlocked"
                            else -> "Complete Level ${level.unlockAfterLevelId} to unlock"
                        },
                        color = if (level.levelId in campaign.completedLevels) Color(0xFF9BE7AE) else Color(0xFFF6CB7D)
                    )
                    Button(
                        onClick = { onPlayLevel(level.levelId) },
                        enabled = unlocked
                    ) {
                        Text("Play")
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpgradesScreen(
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
internal fun InGameHud(
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
                    Text(state.levelName, color = Color(0xFFA7C0D8), fontWeight = FontWeight.SemiBold)
                    Text("Your Funds ${state.playerMoney.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
                    state.aiStates.entries.sortedBy { it.key.ordinal }.forEach { (owner, aiState) ->
                        Text(
                            "${owner.label} Funds ${aiState.money.toInt()}",
                            color = owner.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
internal fun PauseOverlay(
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
internal fun LevelEndOverlay(
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
                Text(state.levelName, color = Color(0xFFA7C0D8))
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
internal fun UpgradeNodeButton(
    state: MatchState,
    viewportSize: IntSize,
    onUpgrade: (Int) -> Unit
) {
    if (viewportSize == IntSize.Zero) return

    val base = selectedUpgradablePlayerBase(state) ?: return
    val canvasSize = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat())
    val center = worldToScreen(base.position, canvasSize, state.worldBounds)
    val radius = base.radius * scale(canvasSize, state.worldBounds)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cost = upgradeCost(base)
    val canAfford = state.playerMoney >= cost
    var measuredButtonSize by remember { mutableStateOf(IntSize.Zero) }
    val fallbackButtonSize = with(density) { IntSize(88.dp.roundToPx(), 40.dp.roundToPx()) }
    val buttonSize = if (measuredButtonSize == IntSize.Zero) fallbackButtonSize else measuredButtonSize
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeAreaInsets = with(density) {
        EdgeInsets(
            left = safeDrawingPadding.calculateLeftPadding(layoutDirection).roundToPx(),
            top = safeDrawingPadding.calculateTopPadding().roundToPx(),
            right = safeDrawingPadding.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = safeDrawingPadding.calculateBottomPadding().roundToPx()
        )
    }
    val buttonOffset = upgradeNodeButtonOffset(
        center = center,
        radius = radius,
        viewportSize = viewportSize,
        buttonSize = buttonSize,
        baseMarginPx = with(density) { 8.dp.roundToPx() },
        horizontalGapPx = with(density) { 6.dp.roundToPx() },
        verticalGapPx = with(density) { 4.dp.roundToPx() },
        edgeInsets = safeAreaInsets
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .offset { buttonOffset }
                .zIndex(2f)
                .widthIn(min = 64.dp)
                .heightIn(min = 40.dp)
                .background(
                    color = if (canAfford) Color(0xFFF6CB7D) else Color(0xFF6C5A2B),
                    shape = RoundedCornerShape(50)
                )
                .clickable { onUpgrade(base.id) }
                .onSizeChanged { measuredButtonSize = it }
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

internal data class EdgeInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
)

internal fun upgradeNodeButtonOffset(
    center: Offset,
    radius: Float,
    viewportSize: IntSize,
    buttonSize: IntSize,
    baseMarginPx: Int,
    horizontalGapPx: Int,
    verticalGapPx: Int,
    edgeInsets: EdgeInsets = EdgeInsets()
): IntOffset {
    val minX = (edgeInsets.left + baseMarginPx).toFloat()
    val minY = (edgeInsets.top + baseMarginPx).toFloat()
    val maxX = (viewportSize.width - buttonSize.width - edgeInsets.right - baseMarginPx).toFloat()
    val maxY = (viewportSize.height - buttonSize.height - edgeInsets.bottom - baseMarginPx).toFloat()
    val preferredX = center.x + radius + horizontalGapPx
    val preferredY = center.y - radius - verticalGapPx

    return IntOffset(
        x = preferredX.coerceIn(minX, maxX.coerceAtLeast(minX)).toInt(),
        y = preferredY.coerceIn(minY, maxY.coerceAtLeast(minY)).toInt()
    )
}

internal fun selectedUpgradablePlayerBase(state: MatchState): BaseState? {
    if (state.selectedBaseIds.size != 1) return null

    val selectedBaseId = state.selectedBaseIds.first()
    val base = state.bases.firstOrNull { it.id == selectedBaseId && it.owner == Owner.PLAYER } ?: return null
    return base.takeIf { it.capLevel < it.maxLevel }
}
